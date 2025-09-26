// File: src/main/java/com/mining/parallel/TopKManager.java
package com.mining.parallel;

import com.mining.config.AlgorithmConstants;
import com.mining.core.model.Itemset;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Enhanced lock-free Top-K manager using CAS operations.
 * Optimized for high-concurrency parallel mining.
 */
@Slf4j
@Getter
public class TopKManager {

    private final int k;
    private final AtomicReferenceArray<Itemset> topKArray;
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicReference<Double> threshold = new AtomicReference<>(0.0);

    // Performance tracking
    private final AtomicLong casRetries = new AtomicLong(0);
    private final AtomicLong successfulUpdates = new AtomicLong(0);
    private final AtomicLong failedUpdates = new AtomicLong(0);

    // Cache for faster threshold access
    private volatile double cachedThreshold = 0.0;

    // Optional read-write lock for bulk operations
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public TopKManager(int k) {
        if (k <= 0) {
            throw new IllegalArgumentException("K must be positive");
        }

        this.k = k;
        this.topKArray = new AtomicReferenceArray<>(k);

        log.debug("TopKManager initialized with k={}", k);
    }

    /**
     * Try to add an itemset to top-K using lock-free CAS operations.
     *
     * @param items The itemset
     * @param expectedUtility Expected utility value
     * @param probability Existential probability
     * @return true if successfully added/updated
     */
    public boolean tryAdd(Set<Integer> items, double expectedUtility, double probability) {
        // Fast path - check cached threshold
        if (expectedUtility < cachedThreshold - AlgorithmConstants.EPSILON) {
            failedUpdates.incrementAndGet();
            return false;
        }

        // Try to add to empty slot first
        for (int i = 0; i < k; i++) {
            if (topKArray.get(i) == null) {
                Itemset newItemset = new Itemset(items, expectedUtility, probability);
                if (topKArray.compareAndSet(i, null, newItemset)) {
                    size.incrementAndGet();
                    successfulUpdates.incrementAndGet();
                    updateThreshold();
                    log.trace("Added itemset to slot {}: EU={}", i, expectedUtility);
                    return true;
                }
                // CAS failed, another thread filled this slot
                casRetries.incrementAndGet();
            }
        }

        // Check for duplicates and potential updates
        for (int i = 0; i < k; i++) {
            Itemset existing = topKArray.get(i);
            if (existing != null && existing.getItems().equals(items)) {
                // Found duplicate - only update if new utility is higher
                if (expectedUtility > existing.getExpectedUtility() + AlgorithmConstants.EPSILON) {
                    // New utility is higher, replace the itemset
                    Itemset updatedItemset = new Itemset(items, expectedUtility,
                        Math.max(existing.getProbability(), probability));

                    if (topKArray.compareAndSet(i, existing, updatedItemset)) {
                        successfulUpdates.incrementAndGet();
                        updateThreshold();
                        log.trace("Updated itemset at slot {}: EU {} -> {}",
                            i, existing.getExpectedUtility(), expectedUtility);
                        return true;
                    }
                    // CAS failed, another thread changed it
                    casRetries.incrementAndGet();
                    return false;
                } else {
                    // New utility is not higher, don't update
                    failedUpdates.incrementAndGet();
                    log.trace("Not updating itemset at slot {}: existing EU {} >= new EU {}",
                        i, existing.getExpectedUtility(), expectedUtility);
                    return false;
                }
            }
        }

        // Array is full - try to replace weakest
        if (size.get() >= k) {
            return tryReplaceWeakest(items, expectedUtility, probability);
        }

        failedUpdates.incrementAndGet();
        return false;
    }

    /**
     * Try to replace the weakest itemset.
     */
    private boolean tryReplaceWeakest(Set<Integer> items, double expectedUtility, double probability) {
        int maxRetries = Math.min(k, AlgorithmConstants.MAX_CAS_RETRIES);

        for (int retry = 0; retry < maxRetries; retry++) {
            // Find current weakest
            int weakestIndex = -1;
            double weakestUtility = Double.MAX_VALUE;
            Itemset weakestItemset = null;

            for (int i = 0; i < k; i++) {
                Itemset current = topKArray.get(i);
                if (current != null && current.getExpectedUtility() < weakestUtility) {
                    weakestUtility = current.getExpectedUtility();
                    weakestIndex = i;
                    weakestItemset = current;
                }
            }

            // Check if new itemset is better than weakest
            if (weakestIndex != -1 && expectedUtility > weakestUtility + AlgorithmConstants.EPSILON) {
                Itemset newItemset = new Itemset(items, expectedUtility, probability);
                if (topKArray.compareAndSet(weakestIndex, weakestItemset, newItemset)) {
                    successfulUpdates.incrementAndGet();
                    updateThreshold();
                    log.trace("Replaced weakest at slot {}: EU {} -> {}",
                        weakestIndex, weakestUtility, expectedUtility);
                    return true;
                }
                casRetries.incrementAndGet();
                // CAS failed, retry
            } else {
                // Not better than weakest
                failedUpdates.incrementAndGet();
                return false;
            }
        }

        // Max retries exceeded
        failedUpdates.incrementAndGet();
        return false;
    }

    /**
     * Update the threshold value.
     */
    private void updateThreshold() {
        if (size.get() < k) {
            // Not full yet, threshold stays at 0
            return;
        }

        // Find minimum utility
        double minUtility = Double.MAX_VALUE;
        int validCount = 0;

        for (int i = 0; i < k; i++) {
            Itemset itemset = topKArray.get(i);
            if (itemset != null) {
                validCount++;
                if (itemset.getExpectedUtility() < minUtility) {
                    minUtility = itemset.getExpectedUtility();
                }
            }
        }

        if (validCount >= k) {
            threshold.set(minUtility);
            cachedThreshold = minUtility;
            log.trace("Updated threshold to {}", minUtility);
        }
    }

    /**
     * Get the current threshold value.
     */
    public double getThreshold() {
        return cachedThreshold;
    }

    /**
     * Get the current top-K itemsets sorted by utility.
     */
    public List<Itemset> getTopK() {
        rwLock.readLock().lock();
        try {
            List<Itemset> result = new ArrayList<>();

            for (int i = 0; i < k; i++) {
                Itemset itemset = topKArray.get(i);
                if (itemset != null) {
                    result.add(itemset);
                }
            }

            // Sort by utility (descending)
            result.sort(Comparator.reverseOrder());

            return result;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Get CAS efficiency metric.
     */
    public double getCASEfficiency() {
        long successful = successfulUpdates.get();
        long retries = casRetries.get();
        long total = successful + retries;

        return total > 0 ? (double) successful / total : 1.0;
    }

    /**
     * Get update success rate.
     */
    public double getUpdateSuccessRate() {
        long successful = successfulUpdates.get();
        long failed = failedUpdates.get();
        long total = successful + failed;

        return total > 0 ? (double) successful / total : 0.0;
    }

    /**
     * Reset statistics (for testing).
     */
    public void resetStatistics() {
        casRetries.set(0);
        successfulUpdates.set(0);
        failedUpdates.set(0);
    }

    /**
     * Clear all itemsets (for testing).
     */
    public void clear() {
        rwLock.writeLock().lock();
        try {
            for (int i = 0; i < k; i++) {
                topKArray.set(i, null);
            }
            size.set(0);
            threshold.set(0.0);
            cachedThreshold = 0.0;
            resetStatistics();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}