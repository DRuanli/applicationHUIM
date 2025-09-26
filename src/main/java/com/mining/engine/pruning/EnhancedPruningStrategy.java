// File: src/main/java/com/mining/engine/pruning/EnhancedPruningStrategy.java
package com.mining.engine.pruning;

import com.mining.config.AlgorithmConstants;
import com.mining.core.model.UtilityList;
import com.mining.engine.statistics.MiningStatistics;
import com.mining.parallel.TopKManager;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Enhanced pruning strategy with multiple optimization techniques.
 */
@Slf4j
public class EnhancedPruningStrategy implements PruningStrategy {

    private final TopKManager topKManager;
    private final double minProbability;
    private final MiningStatistics statistics;

    // Adaptive pruning parameters
    private double adaptiveFactor = 1.0;
    private long lastUpdateTime = 0;
    private static final long ADAPTATION_INTERVAL = 10000; // 10 seconds

    public EnhancedPruningStrategy(TopKManager topKManager, double minProbability,
                                  MiningStatistics statistics) {
        this.topKManager = topKManager;
        this.minProbability = minProbability;
        this.statistics = statistics;
    }

    @Override
    public boolean shouldPruneByRTWU(double rtwu) {
        double threshold = topKManager.getThreshold();
        double adaptiveThreshold = threshold * adaptiveFactor;

        if (rtwu < adaptiveThreshold - AlgorithmConstants.EPSILON) {
            statistics.incrementRtwuPruned();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPruneByExistentialProbability(double probability) {
        if (probability < minProbability - AlgorithmConstants.EPSILON) {
            statistics.incrementEpPruned();
            statistics.incrementCandidatesPruned();
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPruneByUpperBound(double sumEU, double sumRemaining) {
        double threshold = topKManager.getThreshold();
        double upperBound = sumEU + sumRemaining;

        if (upperBound < threshold - AlgorithmConstants.EPSILON) {
            statistics.incrementEuPruned();
            statistics.incrementCandidatesPruned();
            return true;
        }
        return false;
    }

    @Override
    public boolean qualifiesForTopK(double sumEU, double probability) {
        double threshold = topKManager.getThreshold();
        return sumEU >= threshold - AlgorithmConstants.EPSILON &&
               probability >= minProbability - AlgorithmConstants.EPSILON;
    }

    @Override
    public boolean shouldPrune(UtilityList utilityList) {
        // Check existential probability first (cheapest check)
        if (shouldPruneByExistentialProbability(utilityList.getExistentialProbability())) {
            return true;
        }

        // Check RTWU
        if (shouldPruneByRTWU(utilityList.getRtwu())) {
            return true;
        }

        // Check upper bound
        if (shouldPruneByUpperBound(utilityList.getSumEU(), utilityList.getSumRemaining())) {
            return true;
        }

        // Update adaptive factor periodically
        updateAdaptiveFactor();

        return false;
    }

    @Override
    public boolean shouldBulkPrune(UtilityList prefix, List<UtilityList> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return true;
        }

        // Find minimum RTWU among extensions
        double minRTWU = Double.MAX_VALUE;
        for (UtilityList ext : extensions) {
            if (ext.getRtwu() < minRTWU) {
                minRTWU = ext.getRtwu();
            }
        }

        // Calculate maximum possible RTWU for any joined result
        double maxPossibleRTWU = Math.min(prefix.getRtwu(), minRTWU);
        double threshold = topKManager.getThreshold();

        if (maxPossibleRTWU < threshold - AlgorithmConstants.EPSILON) {
            statistics.incrementBulkBranchPruned();
            statistics.addCandidatesPruned(extensions.size());
            return true;
        }

        // Aggressive pruning if threshold is high
        if (threshold > 0 && maxPossibleRTWU < threshold * AlgorithmConstants.AGGRESSIVE_PRUNING_FACTOR) {
            statistics.incrementBulkBranchPruned();
            return true;
        }

        return false;
    }

    /**
     * Updates adaptive pruning factor based on performance.
     */
    private void updateAdaptiveFactor() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastUpdateTime > ADAPTATION_INTERVAL) {
            // Calculate pruning effectiveness
            long totalCandidates = statistics.getCandidatesGenerated().get();
            long prunedCandidates = statistics.getCandidatesPruned().get();

            if (totalCandidates > 1000) {
                double pruneRate = (double) prunedCandidates / totalCandidates;

                // Adjust factor based on pruning rate
                if (pruneRate < 0.5) {
                    // Not pruning enough, be more aggressive
                    adaptiveFactor = Math.min(adaptiveFactor * 1.1, 2.0);
                } else if (pruneRate > 0.9) {
                    // Pruning too much, be less aggressive
                    adaptiveFactor = Math.max(adaptiveFactor * 0.95, 0.8);
                }

                log.debug("Updated adaptive factor to {:.3f} (prune rate: {:.2f}%)",
                    adaptiveFactor, pruneRate * 100);
            }

            lastUpdateTime = currentTime;
        }
    }
}