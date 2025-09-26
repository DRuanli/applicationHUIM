// File: src/main/java/com/mining/engine/pruning/PruningStrategy.java
package com.mining.engine.pruning;

import com.mining.core.model.UtilityList;

import java.util.List;

/**
 * Interface for pruning strategies in utility mining.
 */
public interface PruningStrategy {

    /**
     * Checks if a utility list should be pruned based on RTWU.
     */
    boolean shouldPruneByRTWU(double rtwu);

    /**
     * Checks if a utility list should be pruned based on existential probability.
     */
    boolean shouldPruneByExistentialProbability(double probability);

    /**
     * Checks if a utility list should be pruned based on upper bound.
     */
    boolean shouldPruneByUpperBound(double sumEU, double sumRemaining);

    /**
     * Checks if an itemset qualifies for top-K.
     */
    boolean qualifiesForTopK(double sumEU, double probability);

    /**
     * Comprehensive pruning check.
     */
    boolean shouldPrune(UtilityList utilityList);

    /**
     * Bulk pruning for multiple extensions.
     */
    boolean shouldBulkPrune(UtilityList prefix, List<UtilityList> extensions);
}