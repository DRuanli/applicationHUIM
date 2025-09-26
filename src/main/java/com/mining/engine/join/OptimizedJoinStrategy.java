// File: src/main/java/com/mining/engine/join/OptimizedJoinStrategy.java
package com.mining.engine.join;

import com.mining.config.AlgorithmConstants;
import com.mining.core.model.UtilityList;
import com.mining.core.model.UtilityList.Element;
import com.mining.engine.statistics.MiningStatistics;
import com.mining.parallel.TopKManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Optimized join strategy with advanced pruning and memory management.
 */
@Slf4j
@Getter
public class OptimizedJoinStrategy implements JoinStrategy {

    private final TopKManager topKManager;
    private final MiningStatistics statistics;
    private final JoinStatistics joinStatistics;

    public OptimizedJoinStrategy(TopKManager topKManager, MiningStatistics statistics) {
        this.topKManager = topKManager;
        this.statistics = statistics;
        this.joinStatistics = new JoinStatistics();
    }

    @Override
    public UtilityList join(UtilityList ul1, UtilityList ul2) {
        joinStatistics.incrementTotalJoins();

        // Early termination based on RTWU
        double joinedRTWU = Math.min(ul1.getRtwu(), ul2.getRtwu());
        double currentThreshold = topKManager.getThreshold();

        if (joinedRTWU < currentThreshold - AlgorithmConstants.EPSILON) {
            statistics.incrementRtwuPruned();
            joinStatistics.incrementPrunedJoins();
            return null;
        }

        // Check if lists are empty
        if (ul1.isEmpty() || ul2.isEmpty()) {
            joinStatistics.incrementEmptyJoins();
            return null;
        }

        // Perform the actual join
        List<Element> joinedElements = performJoin(ul1, ul2);

        if (joinedElements.isEmpty()) {
            joinStatistics.incrementEmptyResultJoins();
            return null;
        }

        // Create joined itemset
        Set<Integer> joinedItemset = createJoinedItemset(ul1.getItemset(), ul2.getItemset());

        joinStatistics.incrementSuccessfulJoins();
        return new UtilityList(joinedItemset, joinedElements, joinedRTWU);
    }

    /**
     * Performs the actual join operation on utility list elements.
     */
    private List<Element> performJoin(UtilityList ul1, UtilityList ul2) {
        List<Element> elements1 = ul1.getElements();
        List<Element> elements2 = ul2.getElements();

        int size1 = elements1.size();
        int size2 = elements2.size();

        // Estimate initial capacity for better memory efficiency
        int estimatedCapacity = estimateJoinCapacity(size1, size2);
        List<Element> result = new ArrayList<>(estimatedCapacity);

        // Two-pointer merge approach
        int i = 0, j = 0;
        int matchCount = 0;
        int missCount = 0;

        while (i < size1 && j < size2) {
            Element e1 = elements1.get(i);
            Element e2 = elements2.get(j);

            if (e1.getTid() == e2.getTid()) {
                // Join matching transactions
                Element joined = joinElements(e1, e2);
                if (joined != null) {
                    result.add(joined);
                    matchCount++;
                }
                i++;
                j++;
                missCount = 0; // Reset miss counter
            } else if (e1.getTid() < e2.getTid()) {
                i++;
                missCount++;
            } else {
                j++;
                missCount++;
            }

            // Early termination if too many consecutive misses
            if (shouldTerminateEarly(missCount, matchCount, i, j, size1, size2)) {
                joinStatistics.incrementEarlyTerminations();
                break;
            }
        }

        // Optimize memory if significantly overallocated
        if (result instanceof ArrayList && result.size() < estimatedCapacity / 3) {
            ((ArrayList<Element>) result).trimToSize();
        }

        return result;
    }

    /**
     * Joins two elements from matching transactions.
     */
    private Element joinElements(Element e1, Element e2) {
        double newUtility = e1.getUtility() + e2.getUtility();
        double newRemaining = Math.min(e1.getRemaining(), e2.getRemaining());
        double newLogProbability = e1.getLogProbability() + e2.getLogProbability();

        // Check if probability is meaningful (avoid underflow)
        if (newLogProbability <= AlgorithmConstants.LOG_EPSILON) {
            return null;
        }

        return new Element(e1.getTid(), newUtility, newRemaining, newLogProbability);
    }

    /**
     * Creates the joined itemset efficiently.
     */
    private Set<Integer> createJoinedItemset(Set<Integer> set1, Set<Integer> set2) {
        int totalSize = set1.size() + set2.size();

        // Use optimal collection size and load factor
        Set<Integer> result = new HashSet<>(totalSize, AlgorithmConstants.LOAD_FACTOR);
        result.addAll(set1);
        result.addAll(set2);

        return result;
    }

    /**
     * Estimates optimal initial capacity for join result.
     */
    private int estimateJoinCapacity(int size1, int size2) {
        // Heuristic: typically 30-50% of minimum size
        int minSize = Math.min(size1, size2);
        int estimate = minSize / 3;

        // Bound the estimate
        estimate = Math.max(estimate, 4);
        estimate = Math.min(estimate, 1024);

        return estimate;
    }

    /**
     * Determines if join should terminate early.
     */
    private boolean shouldTerminateEarly(int missCount, int matchCount,
                                        int i, int j, int size1, int size2) {
        // Don't terminate if we're close to the end
        if ((size1 - i) < 10 || (size2 - j) < 10) {
            return false;
        }

        // Terminate if too many consecutive misses with no matches
        if (matchCount == 0 && missCount > 50 && (i + j) > 100) {
            return true;
        }

        // Terminate if miss ratio is too high
        if (matchCount > 0 && missCount > matchCount * 10 && (i + j) > 200) {
            return true;
        }

        return false;
    }

    @Override
    public String getStrategyName() {
        return "OptimizedJoinStrategy";
    }

    @Override
    public JoinStatistics getStatistics() {
        return joinStatistics;
    }
}