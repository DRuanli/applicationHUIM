// File: src/main/java/com/mining/engine/MiningEngine.java
package com.mining.engine;

import com.google.common.base.Preconditions;
import com.mining.config.AlgorithmConstants;
import com.mining.core.model.Itemset;
import com.mining.core.model.Transaction;
import com.mining.core.model.UtilityList;
import com.mining.engine.join.JoinStrategy;
import com.mining.engine.join.OptimizedJoinStrategy;
import com.mining.engine.pruning.PruningStrategy;
import com.mining.engine.pruning.EnhancedPruningStrategy;
import com.mining.engine.statistics.MiningStatistics;
import com.mining.parallel.TaskScheduler;
import com.mining.parallel.TopKManager;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Core mining engine for PTK-HUIM-U± algorithm.
 * Implements the main mining logic with parallel processing support.
 */
@Slf4j
@Getter
public class MiningEngine {

    private final Map<Integer, Double> itemProfits;
    private final int k;
    private final double minProbability;

    // Core components
    private final TopKManager topKManager;
    private final MiningStatistics statistics;
    private final PruningStrategy pruningStrategy;
    private final JoinStrategy joinStrategy;
    private final TaskScheduler taskScheduler;
    private final UtilityListBuilder utilityListBuilder;

    // Item ordering
    private Map<Integer, Integer> itemToRank;
    private Map<Integer, Double> itemRTWU;

    // Memory monitoring
    private final AtomicLong peakMemoryUsage;

    @Builder
    public MiningEngine(Map<Integer, Double> itemProfits, int k, double minProbability) {
        Preconditions.checkNotNull(itemProfits, "Item profits cannot be null");
        Preconditions.checkArgument(k > 0, "K must be positive");
        Preconditions.checkArgument(minProbability >= 0 && minProbability <= 1,
            "Minimum probability must be between 0 and 1");

        this.itemProfits = Collections.unmodifiableMap(new HashMap<>(itemProfits));
        this.k = k;
        this.minProbability = minProbability;

        // Initialize components
        this.topKManager = new TopKManager(k);
        this.statistics = new MiningStatistics();
        this.pruningStrategy = new EnhancedPruningStrategy(topKManager, minProbability, statistics);
        this.joinStrategy = new OptimizedJoinStrategy(topKManager, statistics);
        this.peakMemoryUsage = new AtomicLong(0);
        this.utilityListBuilder = new UtilityListBuilder(itemProfits, minProbability, statistics);

        int numThreads = Runtime.getRuntime().availableProcessors();
        this.taskScheduler = new TaskScheduler(numThreads, peakMemoryUsage, this);

        log.info("Mining engine initialized with k={}, minProb={}, threads={}",
            k, minProbability, numThreads);
    }

    /**
     * Main mining method - executes the PTK-HUIM-U algorithm.
     *
     * @param database List of transactions
     * @return List of top-K itemsets
     */
    public List<Itemset> mine(List<Transaction> database) {
        Preconditions.checkNotNull(database, "Database cannot be null");
        Preconditions.checkArgument(!database.isEmpty(), "Database cannot be empty");

        log.info("Starting PTK-HUIM-U mining on {} transactions", database.size());

        try {
            // Phase 1: Initialize utility lists with optimization
            log.info("Phase 1: Building optimized utility lists...");
            Map<Integer, UtilityList> singleItemLists = initializeUtilityLists(database);

            if (singleItemLists.isEmpty()) {
                log.warn("No valid items found after initialization");
                return Collections.emptyList();
            }

            // Sort items by RTWU ranking
            List<Integer> sortedItems = getSortedItemsByRank(singleItemLists.keySet());
            log.info("Processing {} items after filtering", sortedItems.size());

            // Process single items
            processSingleItems(singleItemLists, sortedItems);

            // Phase 2: Parallel mining for larger itemsets
            log.info("Phase 2: Mining larger itemsets with parallel processing...");

            if (sortedItems.size() >= AlgorithmConstants.PARALLEL_THRESHOLD) {
                taskScheduler.executePrefixMining(sortedItems, singleItemLists);
            } else {
                executeSequentialMining(sortedItems, singleItemLists);
            }

            // Get final results
            List<Itemset> results = topKManager.getTopK();
            log.info("Mining completed. Found {} itemsets", results.size());

            return results;

        } catch (Exception e) {
            log.error("Mining failed with error", e);
            throw new RuntimeException("Mining failed: " + e.getMessage(), e);
        } finally {
            updatePeakMemory();
        }
    }

    /**
     * Initialize utility lists with suffix sum optimization.
     */
    private Map<Integer, UtilityList> initializeUtilityLists(List<Transaction> database) {
        // Calculate RTWU values
        this.itemRTWU = utilityListBuilder.calculateRTWU(database);

        // Build item ranking based on RTWU
        this.itemToRank = buildItemRanking(itemRTWU);

        // Build utility lists with optimization
        return utilityListBuilder.buildUtilityLists(database, itemToRank, itemRTWU);
    }

    /**
     * Build item ranking based on RTWU values.
     */
    private Map<Integer, Integer> buildItemRanking(Map<Integer, Double> itemRTWU) {
        Map<Integer, Integer> ranking = new HashMap<>();

        List<Integer> rankedItems = itemRTWU.entrySet().stream()
            .sorted((a, b) -> {
                int cmp = Double.compare(a.getValue(), b.getValue());
                return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
            })
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        for (int i = 0; i < rankedItems.size(); i++) {
            ranking.put(rankedItems.get(i), i);
        }

        log.debug("Item ranking established for {} items", ranking.size());
        return ranking;
    }

    /**
     * Process single items for top-K.
     */
    private void processSingleItems(Map<Integer, UtilityList> singleItemLists,
                                   List<Integer> sortedItems) {
        int addedCount = 0;

        for (Integer item : sortedItems) {
            UtilityList ul = singleItemLists.get(item);
            if (ul == null) continue;

            double sumEU = ul.getSumEU();
            double existProb = ul.getExistentialProbability();

            if (pruningStrategy.qualifiesForTopK(sumEU, existProb)) {
                if (topKManager.tryAdd(ul.getItemset(), sumEU, existProb)) {
                    addedCount++;
                }
            }
        }

        log.debug("Added {} single items to top-K", addedCount);
    }

    /**
     * Execute sequential mining for smaller datasets.
     */
    public void executeSequentialMining(List<Integer> sortedItems,
                                       Map<Integer, UtilityList> singleItemLists) {
        log.info("Using sequential processing for {} items", sortedItems.size());

        for (int i = 0; i < sortedItems.size(); i++) {
            Integer item = sortedItems.get(i);
            UtilityList prefix = singleItemLists.get(item);

            if (prefix == null || pruningStrategy.shouldPruneByRTWU(itemRTWU.get(item))) {
                statistics.incrementBranchPruned();
                continue;
            }

            // Build extensions
            List<UtilityList> extensions = buildExtensions(prefix, sortedItems, i, singleItemLists);

            if (!extensions.isEmpty()) {
                searchWithPruning(prefix, extensions, singleItemLists);
            }

            // Progress reporting
            if ((i + 1) % AlgorithmConstants.PROGRESS_REPORT_INTERVAL == 0) {
                reportProgress(i + 1, sortedItems.size());
            }
        }
    }

    /**
     * Build extension list for a prefix.
     */
    private List<UtilityList> buildExtensions(UtilityList prefix, List<Integer> sortedItems,
                                             int startIndex, Map<Integer, UtilityList> singleItemLists) {
        List<UtilityList> extensions = new ArrayList<>();
        double threshold = topKManager.getThreshold();

        for (int j = startIndex + 1; j < sortedItems.size(); j++) {
            Integer extItem = sortedItems.get(j);
            UtilityList extUL = singleItemLists.get(extItem);

            if (extUL == null) continue;

            if (itemRTWU.get(extItem) < threshold - AlgorithmConstants.EPSILON) {
                statistics.incrementRtwuPruned();
                continue;
            }

            extensions.add(extUL);
        }

        return extensions;
    }

    /**
     * Search for itemsets with enhanced pruning.
     */
    public void searchWithPruning(UtilityList prefix, List<UtilityList> extensions,
                                 Map<Integer, UtilityList> singleItemLists) {
        if (extensions == null || extensions.isEmpty()) {
            return;
        }

        // Check for bulk pruning opportunity
        if (pruningStrategy.shouldBulkPrune(prefix, extensions)) {
            return;
        }

        // Sort extensions by RTWU for better pruning
        extensions.sort((a, b) -> Double.compare(b.getRtwu(), a.getRtwu()));

        List<UtilityList> newExtensions = new ArrayList<>();

        for (int i = 0; i < extensions.size(); i++) {
            UtilityList extension = extensions.get(i);

            // Join prefix with extension
            UtilityList joined = joinStrategy.join(prefix, extension);

            if (joined == null || joined.isEmpty()) {
                continue;
            }

            statistics.incrementUtilityListsCreated();

            // Apply pruning strategies
            if (pruningStrategy.shouldPrune(joined)) {
                continue;
            }

            // Check if qualifies for top-K
            double sumEU = joined.getSumEU();
            double existProb = joined.getExistentialProbability();

            if (pruningStrategy.qualifiesForTopK(sumEU, existProb)) {
                topKManager.tryAdd(joined.getItemset(), sumEU, existProb);
            }

            // Prepare extensions for recursive search
            if (i < extensions.size() - 1) {
                newExtensions.clear();

                for (int j = i + 1; j < extensions.size(); j++) {
                    UtilityList ext = extensions.get(j);
                    if (!pruningStrategy.shouldPruneByRTWU(ext.getRtwu())) {
                        newExtensions.add(ext);
                    }
                }

                if (!newExtensions.isEmpty()) {
                    searchWithPruning(joined, newExtensions, singleItemLists);
                }
            }
        }
    }

    /**
     * Get sorted items by rank.
     */
    private List<Integer> getSortedItemsByRank(Set<Integer> items) {
        return items.stream()
            .sorted((a, b) -> {
                Integer rankA = itemToRank.get(a);
                Integer rankB = itemToRank.get(b);
                if (rankA == null || rankB == null) {
                    return 0;
                }
                return rankA.compareTo(rankB);
            })
            .collect(Collectors.toList());
    }

    /**
     * Report progress during mining.
     */
    private void reportProgress(int current, int total) {
        long usedMemory = Runtime.getRuntime().totalMemory() -
                         Runtime.getRuntime().freeMemory();
        peakMemoryUsage.updateAndGet(peak -> Math.max(peak, usedMemory));

        log.info("Progress: {}/{} items processed. Memory: {} MB, Threshold: {:.6f}",
            current, total, usedMemory / 1024 / 1024, topKManager.getThreshold());
    }

    /**
     * Update peak memory usage.
     */
    private void updatePeakMemory() {
        long usedMemory = Runtime.getRuntime().totalMemory() -
                         Runtime.getRuntime().freeMemory();
        peakMemoryUsage.updateAndGet(peak -> Math.max(peak, usedMemory));
    }

    /**
     * Shutdown the mining engine and release resources.
     */
    public void shutdown() {
        log.info("Shutting down mining engine...");
        taskScheduler.shutdown();
        log.info("Mining engine shutdown complete");
    }

    /**
     * Get the parallelism level.
     */
    public int getParallelism() {
        return taskScheduler.getParallelism();
    }
}