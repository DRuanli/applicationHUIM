// File: src/main/java/com/mining/engine/join/JoinStrategy.java
package com.mining.engine.join;

import com.mining.core.model.UtilityList;

/**
 * Interface for join strategies in utility list mining.
 */
public interface JoinStrategy {
    
    /**
     * Joins two utility lists to create a new utility list.
     * 
     * @param ul1 First utility list
     * @param ul2 Second utility list
     * @return Joined utility list or null if pruned
     */
    UtilityList join(UtilityList ul1, UtilityList ul2);
    
    /**
     * Gets the name of this join strategy.
     * 
     * @return Strategy name
     */
    String getStrategyName();
    
    /**
     * Gets statistics about join operations.
     * 
     * @return Join statistics
     */
    JoinStatistics getStatistics();
}