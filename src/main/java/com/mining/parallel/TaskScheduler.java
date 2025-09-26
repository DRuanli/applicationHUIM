// File: src/main/java/com/mining/parallel/TaskScheduler.java
package com.mining.parallel;

import com.mining.config.AlgorithmConstants;
import com.mining.core.model.UtilityList;
import com.mining.engine.MiningEngine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enhanced task scheduler for parallel mining with ForkJoinPool.
 * Implements work-stealing for optimal load balancing.
 */
@Slf4j
@Getter
public class TaskScheduler {

    private final ForkJoinPool customThreadPool;
    private final AtomicLong peakMemoryUsage;
    private final MiningEngine miningEngine;
    private final int parallelism;

    // Performance tracking
    private final AtomicLong totalTasksSubmitted = new AtomicLong(0);
    private final AtomicLong totalTasksCompleted = new AtomicLong(0);
    private final AtomicLong totalStealCount = new AtomicLong(0);

    public TaskScheduler(int numThreads, AtomicLong peakMemoryUsage, MiningEngine miningEngine) {
        this.parallelism = numThreads;
        this.peakMemoryUsage = peakMemoryUsage;
        this.miningEngine = miningEngine;

        // Create custom ForkJoinPool with specific configuration
        this.customThreadPool = new ForkJoinPool(
            numThreads,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            this::handleUncaughtException,
            false
        );

        log.info("TaskScheduler initialized with {} threads", numThreads);
    }

    /**
     * Execute prefix mining in parallel.
     */
    public void executePrefixMining(List<Integer> sortedItems,
                                  Map<Integer, UtilityList> singleItemLists) {
        if (sortedItems.size() < AlgorithmConstants.PARALLEL_THRESHOLD) {
            log.debug("Dataset too small for parallel processing, using sequential");
            miningEngine.executeSequentialMining(sortedItems, singleItemLists);
            return;
        }

        log.info("Starting parallel prefix mining for {} items", sortedItems.size());

        try {
            PrefixMiningTask rootTask = new PrefixMiningTask(
                sortedItems, singleItemLists, 0, sortedItems.size()
            );

            totalTasksSubmitted.incrementAndGet();
            customThreadPool.invoke(rootTask);

            // Update steal count after completion
            totalStealCount.addAndGet(customThreadPool.getStealCount());

            log.info("Parallel mining completed. Tasks: {}, Steals: {}",
                totalTasksCompleted.get(), totalStealCount.get());

        } catch (Exception e) {
            log.error("Error in parallel processing, falling back to sequential", e);
            miningEngine.executeSequentialMining(sortedItems, singleItemLists);
        }
    }

    /**
     * Execute extension search conditionally in parallel.
     */
    public boolean executeExtensionSearch(UtilityList prefix,
                                        List<UtilityList> extensions,
                                        Map<Integer, UtilityList> singleItemLists) {
        // Only parallelize if worth it
        if (extensions.size() < AlgorithmConstants.PARALLEL_THRESHOLD ||
            !ForkJoinTask.inForkJoinPool()) {
            return false;
        }

        ExtensionSearchTask task = new ExtensionSearchTask(
            prefix, extensions, singleItemLists, 0, extensions.size()
        );

        totalTasksSubmitted.incrementAndGet();
        task.invoke();
        return true;
    }

    /**
     * ForkJoin task for parallel prefix mining.
     */
    public class PrefixMiningTask extends RecursiveAction {
        private final List<Integer> sortedItems;
        private final Map<Integer, UtilityList> singleItemLists;
        private final int start;
        private final int end;

        public PrefixMiningTask(List<Integer> sortedItems,
                              Map<Integer, UtilityList> singleItemLists,
                              int start, int end) {
            this.sortedItems = sortedItems;
            this.singleItemLists = singleItemLists;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int size = end - start;

            // Base case: process sequentially
            if (size <= AlgorithmConstants.TASK_GRANULARITY) {
                processSequentially();
                totalTasksCompleted.incrementAndGet();
                return;
            }

            // Recursive case: divide and conquer
            int mid = start + size / 2;

            PrefixMiningTask leftTask = new PrefixMiningTask(
                sortedItems, singleItemLists, start, mid
            );

            PrefixMiningTask rightTask = new PrefixMiningTask(
                sortedItems, singleItemLists, mid, end
            );

            totalTasksSubmitted.addAndGet(2);

            // Fork left, compute right, then join
            leftTask.fork();
            rightTask.compute();
            leftTask.join();
        }

        private void processSequentially() {
            for (int i = start; i < end; i++) {
                processPrefix(i);

                // Periodic memory monitoring
                if (i % 10 == 0) {
                    updateMemoryUsage();
                }
            }
        }

        private void processPrefix(int index) {
            Integer item = sortedItems.get(index);
            UtilityList prefix = singleItemLists.get(item);

            if (prefix == null) {
                return;
            }

            // Check if branch should be pruned
            Map<Integer, Double> itemRTWU = miningEngine.getItemRTWU();
            double threshold = miningEngine.getTopKManager().getThreshold();

            if (itemRTWU.get(item) < threshold - AlgorithmConstants.EPSILON) {
                miningEngine.getStatistics().incrementBranchPruned();
                return;
            }

            // Build extensions
            List<UtilityList> extensions = buildExtensions(index);

            if (!extensions.isEmpty()) {
                miningEngine.searchWithPruning(prefix, extensions, singleItemLists);
            }
        }

        private List<UtilityList> buildExtensions(int prefixIndex) {
            List<UtilityList> extensions = new ArrayList<>();
            double threshold = miningEngine.getTopKManager().getThreshold();
            Map<Integer, Double> itemRTWU = miningEngine.getItemRTWU();

            for (int j = prefixIndex + 1; j < sortedItems.size(); j++) {
                Integer extItem = sortedItems.get(j);
                UtilityList extUL = singleItemLists.get(extItem);

                if (extUL == null) continue;

                if (itemRTWU.get(extItem) < threshold - AlgorithmConstants.EPSILON) {
                    miningEngine.getStatistics().incrementRtwuPruned();
                    continue;
                }

                extensions.add(extUL);
            }

            return extensions;
        }
    }

    /**
     * ForkJoin task for parallel extension search.
     */
    public class ExtensionSearchTask extends RecursiveAction {
        private final UtilityList prefix;
        private final List<UtilityList> extensions;
        private final Map<Integer, UtilityList> singleItemLists;
        private final int start;
        private final int end;

        public ExtensionSearchTask(UtilityList prefix,
                                 List<UtilityList> extensions,
                                 Map<Integer, UtilityList> singleItemLists,
                                 int start, int end) {
            this.prefix = prefix;
            this.extensions = extensions;
            this.singleItemLists = singleItemLists;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            int size = end - start;

            // Apply bulk pruning for parallel tasks
            if (shouldBulkPrune()) {
                miningEngine.getStatistics().incrementBulkBranchPruned();
                miningEngine.getStatistics().addCandidatesPruned(size);
                totalTasksCompleted.incrementAndGet();
                return;
            }

            // Base case: process sequentially
            if (size <= AlgorithmConstants.TASK_GRANULARITY) {
                processSequentially();
                totalTasksCompleted.incrementAndGet();
                return;
            }

            // Recursive case: divide and conquer
            int mid = start + size / 2;

            ExtensionSearchTask leftTask = new ExtensionSearchTask(
                prefix, extensions, singleItemLists, start, mid
            );

            ExtensionSearchTask rightTask = new ExtensionSearchTask(
                prefix, extensions, singleItemLists, mid, end
            );

            totalTasksSubmitted.addAndGet(2);
            invokeAll(leftTask, rightTask);
        }

        private boolean shouldBulkPrune() {
            if (end - start <= 1) {
                return false;
            }

            double threshold = miningEngine.getTopKManager().getThreshold();
            double minRTWU = Double.MAX_VALUE;

            for (int i = start; i < end; i++) {
                UtilityList ext = extensions.get(i);
                if (ext.getRtwu() < minRTWU) {
                    minRTWU = ext.getRtwu();
                }
            }

            return minRTWU < threshold - AlgorithmConstants.EPSILON;
        }

        private void processSequentially() {
            for (int i = start; i < end; i++) {
                processExtension(i);
            }
        }

        private void processExtension(int index) {
            UtilityList extension = extensions.get(index);
            double threshold = miningEngine.getTopKManager().getThreshold();

            // Check RTWU pruning
            if (extension.getRtwu() < threshold - AlgorithmConstants.EPSILON) {
                miningEngine.getStatistics().incrementRtwuPruned();
                return;
            }

            // Join operation
            UtilityList joined = miningEngine.getJoinStrategy().join(prefix, extension);

            if (joined == null || joined.isEmpty()) {
                return;
            }

            miningEngine.getStatistics().incrementUtilityListsCreated();
            miningEngine.getStatistics().incrementCandidatesGenerated();

            // Apply pruning strategies
            if (miningEngine.getPruningStrategy().shouldPrune(joined)) {
                return;
            }

            // Check if qualifies for top-K
            double sumEU = joined.getSumEU();
            double existProb = joined.getExistentialProbability();

            if (miningEngine.getPruningStrategy().qualifiesForTopK(sumEU, existProb)) {
                miningEngine.getTopKManager().tryAdd(joined.getItemset(), sumEU, existProb);
            }

            // Recursive search for larger itemsets
            if (index < extensions.size() - 1) {
                List<UtilityList> newExtensions = buildNewExtensions(index);
                if (!newExtensions.isEmpty()) {
                    miningEngine.searchWithPruning(joined, newExtensions, singleItemLists);
                }
            }
        }

        private List<UtilityList> buildNewExtensions(int currentIndex) {
            List<UtilityList> newExtensions = new ArrayList<>();
            double threshold = miningEngine.getTopKManager().getThreshold();

            for (int j = currentIndex + 1; j < extensions.size(); j++) {
                UtilityList ext = extensions.get(j);
                if (ext.getRtwu() >= threshold - AlgorithmConstants.EPSILON) {
                    newExtensions.add(ext);
                } else {
                    miningEngine.getStatistics().incrementRtwuPruned();
                }
            }

            return newExtensions;
        }
    }

    /**
     * Update peak memory usage.
     */
    private void updateMemoryUsage() {
        long usedMemory = Runtime.getRuntime().totalMemory() -
                         Runtime.getRuntime().freeMemory();
        peakMemoryUsage.updateAndGet(peak -> Math.max(peak, usedMemory));
    }

    /**
     * Handle uncaught exceptions in worker threads.
     */
    private void handleUncaughtException(Thread thread, Throwable exception) {
        log.error("Uncaught exception in thread {}", thread.getName(), exception);
    }

    /**
     * Shutdown the task scheduler.
     */
    public void shutdown() {
        log.info("Shutting down TaskScheduler...");

        customThreadPool.shutdown();

        try {
            if (!customThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Thread pool didn't terminate gracefully, forcing shutdown");
                customThreadPool.shutdownNow();

                if (!customThreadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("Thread pool failed to terminate");
                }
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during shutdown", e);
            customThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("TaskScheduler shutdown complete. Total tasks: {}, Completed: {}, Steals: {}",
            totalTasksSubmitted.get(), totalTasksCompleted.get(), totalStealCount.get());
    }

    /**
     * Get performance statistics.
     */
    public Map<String, Long> getPerformanceStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("tasksSubmitted", totalTasksSubmitted.get());
        stats.put("tasksCompleted", totalTasksCompleted.get());
        stats.put("stealCount", totalStealCount.get());
        stats.put("parallelism", (long) parallelism);
        stats.put("poolSize", (long) customThreadPool.getPoolSize());
        stats.put("activeThreads", (long) customThreadPool.getActiveThreadCount());
        return stats;
    }
}