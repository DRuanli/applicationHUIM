// File: src/main/java/com/mining/util/MemoryMonitor.java
package com.mining.util;

import com.mining.config.AlgorithmConstants;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory monitoring utility for tracking memory usage during mining.
 */
@Slf4j
@Getter
public class MemoryMonitor {

    private final MemoryMXBean memoryMXBean;
    private final AtomicLong peakMemoryUsage = new AtomicLong(0);
    private final AtomicLong totalMemoryAllocated = new AtomicLong(0);

    private ScheduledExecutorService monitoringService;
    private long startMemory;
    private long endMemory;

    // Statistics
    private long minMemoryUsed = Long.MAX_VALUE;
    private long maxMemoryUsed = 0;
    private long avgMemoryUsed = 0;
    private int sampleCount = 0;

    public MemoryMonitor() {
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    /**
     * Start monitoring memory usage.
     */
    public void startMonitoring() {
        startMemory = getCurrentMemoryUsage();
        peakMemoryUsage.set(startMemory);

        // Start periodic monitoring
        monitoringService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "MemoryMonitor");
            thread.setDaemon(true);
            return thread;
        });

        monitoringService.scheduleAtFixedRate(
            this::updateMemoryStats,
            0,
            1,
            TimeUnit.SECONDS
        );

        log.debug("Memory monitoring started. Initial memory: {} MB",
            startMemory / 1024 / 1024);
    }

    /**
     * Stop monitoring memory usage.
     */
    public void stopMonitoring() {
        if (monitoringService != null) {
            monitoringService.shutdown();
            try {
                if (!monitoringService.awaitTermination(5, TimeUnit.SECONDS)) {
                    monitoringService.shutdownNow();
                }
            } catch (InterruptedException e) {
                monitoringService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        endMemory = getCurrentMemoryUsage();

        log.debug("Memory monitoring stopped. Final memory: {} MB, Peak: {} MB",
            endMemory / 1024 / 1024, peakMemoryUsage.get() / 1024 / 1024);
    }

    /**
     * Update memory statistics.
     */
    private void updateMemoryStats() {
        long currentMemory = getCurrentMemoryUsage();

        // Update peak
        peakMemoryUsage.updateAndGet(peak -> Math.max(peak, currentMemory));

        // Update statistics
        minMemoryUsed = Math.min(minMemoryUsed, currentMemory);
        maxMemoryUsed = Math.max(maxMemoryUsed, currentMemory);

        // Update average
        long totalSoFar = avgMemoryUsed * sampleCount;
        sampleCount++;
        avgMemoryUsed = (totalSoFar + currentMemory) / sampleCount;

        // Check for memory pressure
        checkMemoryPressure(currentMemory);
    }

    /**
     * Get current memory usage in bytes.
     */
    public long getCurrentMemoryUsage() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    /**
     * Get current heap memory usage.
     */
    public MemoryUsage getHeapMemoryUsage() {
        return memoryMXBean.getHeapMemoryUsage();
    }

    /**
     * Get memory usage as percentage.
     */
    public double getMemoryUsagePercentage() {
        MemoryUsage heapUsage = getHeapMemoryUsage();
        return (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
    }

    /**
     * Check for memory pressure and trigger GC if needed.
     */
    private void checkMemoryPressure(long currentMemory) {
        double usagePercentage = getMemoryUsagePercentage();

        if (usagePercentage > AlgorithmConstants.MEMORY_THRESHOLD * 100) {
            log.warn("High memory usage detected: {:.2f}%", usagePercentage);

            // Suggest GC (JVM may ignore)
            System.gc();

            // Wait a bit for GC to complete
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            double newUsage = getMemoryUsagePercentage();
            if (newUsage < usagePercentage) {
                log.info("Memory reclaimed. Usage: {:.2f}% -> {:.2f}%",
                    usagePercentage, newUsage);
            }
        }
    }

    /**
     * Get memory statistics summary.
     */
    public MemoryStats getStatistics() {
        return MemoryStats.builder()
            .startMemory(startMemory)
            .endMemory(endMemory)
            .peakMemory(peakMemoryUsage.get())
            .minMemory(minMemoryUsed == Long.MAX_VALUE ? 0 : minMemoryUsed)
            .maxMemory(maxMemoryUsed)
            .avgMemory(avgMemoryUsed)
            .currentMemory(getCurrentMemoryUsage())
            .usagePercentage(getMemoryUsagePercentage())
            .build();
    }

    /**
     * Memory statistics data class.
     */
    @Getter
    @lombok.Builder
    public static class MemoryStats {
        private final long startMemory;
        private final long endMemory;
        private final long peakMemory;
        private final long minMemory;
        private final long maxMemory;
        private final long avgMemory;
        private final long currentMemory;
        private final double usagePercentage;

        public String toFormattedString() {
            return String.format(
                "Memory Stats: Start=%dMB, End=%dMB, Peak=%dMB, Avg=%dMB, Usage=%.2f%%",
                startMemory / 1024 / 1024,
                endMemory / 1024 / 1024,
                peakMemory / 1024 / 1024,
                avgMemory / 1024 / 1024,
                usagePercentage
            );
        }
    }
}