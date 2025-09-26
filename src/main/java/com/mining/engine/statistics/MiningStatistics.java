// File: src/main/java/com/mining/engine/statistics/MiningStatistics.java
package com.mining.engine.statistics;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced statistics tracking for mining operations.
 * Thread-safe implementation for parallel processing.
 */
@Getter
public class MiningStatistics {
    
    // Core statistics
    private final AtomicLong candidatesGenerated = new AtomicLong(0);
    private final AtomicLong candidatesPruned = new AtomicLong(0);
    private final AtomicLong utilityListsCreated = new AtomicLong(0);
    
    // Pruning statistics
    private final AtomicLong euPruned = new AtomicLong(0);
    private final AtomicLong epPruned = new AtomicLong(0);
    private final AtomicLong rtwuPruned = new AtomicLong(0);
    private final AtomicLong branchPruned = new AtomicLong(0);
    private final AtomicLong bulkBranchPruned = new AtomicLong(0);
    
    // Performance metrics
    private final Map<String, Long> timingMetrics = new ConcurrentHashMap<>();
    private final Map<String, Long> countMetrics = new ConcurrentHashMap<>();
    
    // Timing tracking
    private long startTime;
    private long endTime;
    
    public void startTiming() {
        this.startTime = System.currentTimeMillis();
    }
    
    public void endTiming() {
        this.endTime = System.currentTimeMillis();
    }
    
    public long getExecutionTime() {
        return endTime - startTime;
    }
    
    // Increment methods
    public void incrementCandidatesGenerated() {
        candidatesGenerated.incrementAndGet();
    }
    
    public void incrementCandidatesPruned() {
        candidatesPruned.incrementAndGet();
    }
    
    public void addCandidatesPruned(long count) {
        candidatesPruned.addAndGet(count);
    }
    
    public void incrementUtilityListsCreated() {
        utilityListsCreated.incrementAndGet();
    }
    
    public void incrementEuPruned() {
        euPruned.incrementAndGet();
    }
    
    public void incrementEpPruned() {
        epPruned.incrementAndGet();
    }
    
    public void incrementRtwuPruned() {
        rtwuPruned.incrementAndGet();
    }
    
    public void incrementBranchPruned() {
        branchPruned.incrementAndGet();
    }
    
    public void incrementBulkBranchPruned() {
        bulkBranchPruned.incrementAndGet();
    }
    
    /**
     * Record a timing metric.
     */
    public void recordTiming(String metricName, long timeMs) {
        timingMetrics.merge(metricName, timeMs, Long::sum);
    }
    
    /**
     * Record a count metric.
     */
    public void recordCount(String metricName, long count) {
        countMetrics.merge(metricName, count, Long::sum);
    }
    
    /**
     * Get pruning effectiveness ratio.
     */
    public double getPruningEffectiveness() {
        long total = candidatesGenerated.get();
        return total > 0 ? (double) candidatesPruned.get() / total : 0.0;
    }
    
    /**
     * Print formatted statistics.
     */
    public void printFormattedStatistics() {
        System.out.printf("Candidates Generated:  %,d%n", candidatesGenerated.get());
        System.out.printf("Utility Lists Created: %,d%n", utilityListsCreated.get());
        
        System.out.println("\nPruning Statistics:");
        System.out.printf("  RTWU Pruned:         %,d%n", rtwuPruned.get());
        System.out.printf("  Branches Pruned:     %,d%n", branchPruned.get());
        System.out.printf("  Bulk Pruned:         %,d%n", bulkBranchPruned.get());
        System.out.printf("  EU+Remaining Pruned: %,d%n", euPruned.get());
        System.out.printf("  Probability Pruned:  %,d%n", epPruned.get());
        System.out.printf("  Total Pruned:        %,d%n", candidatesPruned.get());
        System.out.printf("  Pruning Rate:        %.2f%%%n", getPruningEffectiveness() * 100);
    }
    
    /**
     * Convert statistics to map for export.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        
        map.put("candidatesGenerated", candidatesGenerated.get());
        map.put("candidatesPruned", candidatesPruned.get());
        map.put("utilityListsCreated", utilityListsCreated.get());
        map.put("euPruned", euPruned.get());
        map.put("epPruned", epPruned.get());
        map.put("rtwuPruned", rtwuPruned.get());
        map.put("branchPruned", branchPruned.get());
        map.put("bulkBranchPruned", bulkBranchPruned.get());
        map.put("pruningEffectiveness", getPruningEffectiveness());
        map.put("executionTime", getExecutionTime());
        
        if (!timingMetrics.isEmpty()) {
            map.put("timings", new HashMap<>(timingMetrics));
        }
        
        if (!countMetrics.isEmpty()) {
            map.put("counts", new HashMap<>(countMetrics));
        }
        
        return map;
    }
    
    /**
     * Convert statistics to string list for text export.
     */
    public List<String> toStringList() {
        List<String> lines = new ArrayList<>();
        
        lines.add(String.format("Candidates Generated: %d", candidatesGenerated.get()));
        lines.add(String.format("Candidates Pruned: %d", candidatesPruned.get()));
        lines.add(String.format("Utility Lists Created: %d", utilityListsCreated.get()));
        lines.add(String.format("Pruning Effectiveness: %.2f%%", getPruningEffectiveness() * 100));
        lines.add(String.format("Execution Time: %d ms", getExecutionTime()));
        
        return lines;
    }
    
    /**
     * Reset all statistics.
     */
    public void reset() {
        candidatesGenerated.set(0);
        candidatesPruned.set(0);
        utilityListsCreated.set(0);
        euPruned.set(0);
        epPruned.set(0);
        rtwuPruned.set(0);
        branchPruned.set(0);
        bulkBranchPruned.set(0);
        timingMetrics.clear();
        countMetrics.clear();
    }
}