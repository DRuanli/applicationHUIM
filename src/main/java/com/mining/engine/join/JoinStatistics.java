// File: src/main/java/com/mining/engine/join/JoinStatistics.java
package com.mining.engine.join;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for join operations.
 */
@Getter
public class JoinStatistics {
    private final AtomicLong totalJoins = new AtomicLong(0);
    private final AtomicLong successfulJoins = new AtomicLong(0);
    private final AtomicLong prunedJoins = new AtomicLong(0);
    private final AtomicLong emptyJoins = new AtomicLong(0);
    private final AtomicLong emptyResultJoins = new AtomicLong(0);
    private final AtomicLong earlyTerminations = new AtomicLong(0);
    
    public void incrementTotalJoins() {
        totalJoins.incrementAndGet();
    }
    
    public void incrementSuccessfulJoins() {
        successfulJoins.incrementAndGet();
    }
    
    public void incrementPrunedJoins() {
        prunedJoins.incrementAndGet();
    }
    
    public void incrementEmptyJoins() {
        emptyJoins.incrementAndGet();
    }
    
    public void incrementEmptyResultJoins() {
        emptyResultJoins.incrementAndGet();
    }
    
    public void incrementEarlyTerminations() {
        earlyTerminations.incrementAndGet();
    }
    
    public double getSuccessRate() {
        long total = totalJoins.get();
        return total > 0 ? (double) successfulJoins.get() / total : 0.0;
    }
}