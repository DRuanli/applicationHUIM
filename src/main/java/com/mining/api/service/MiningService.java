package com.mining.api.service;

import com.mining.api.entity.ItemsetEntity;
import com.mining.api.entity.MiningJobEntity;
import com.mining.api.repository.ItemsetRepository;
import com.mining.api.repository.MiningJobRepository;
import com.mining.core.model.Itemset;
import com.mining.core.model.Transaction;
import com.mining.engine.MiningEngine;
import com.mining.engine.statistics.MiningStatistics;
import com.mining.io.DataLoader;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MiningService {

    private final ItemsetRepository itemsetRepository;
    private final MiningJobRepository miningJobRepository;
    private final DataLoader dataLoader;
    private final ObjectMapper objectMapper;

    /**
     * Start a new mining job asynchronously
     */
    @Async("miningTaskExecutor")
    @Transactional
    public CompletableFuture<String> startMiningJob(String databaseFile, String profitFile,
                                                   int k, double minProbability) {
        String jobId = generateJobId();

        try {
            // Create and save job entity
            MiningJobEntity job = MiningJobEntity.builder()
                .jobId(jobId)
                .k(k)
                .minProbability(minProbability)
                .databaseFilename(databaseFile)
                .profitFilename(profitFile)
                .status(MiningJobEntity.JobStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

            miningJobRepository.save(job);
            log.info("Created mining job: {}", jobId);

            // Execute mining
            executeMining(jobId, databaseFile, profitFile, k, minProbability);

            return CompletableFuture.completedFuture(jobId);

        } catch (Exception e) {
            log.error("Failed to start mining job: {}", jobId, e);
            handleJobError(jobId, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Execute the mining process
     */
    @Transactional
    private void executeMining(String jobId, String databaseFile, String profitFile,
                              int k, double minProbability) throws Exception {
        log.info("Starting mining execution for job: {}", jobId);

        // Update job status to RUNNING
        MiningJobEntity job = miningJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        job.setStatus(MiningJobEntity.JobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        miningJobRepository.save(job);

        long startTime = System.currentTimeMillis();
        long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        try {
            // Load data
            log.debug("Loading data files for job: {}", jobId);
            Map<Integer, Double> profits = dataLoader.readProfitTable(profitFile);
            List<Transaction> database = dataLoader.readDatabase(databaseFile);

            // Update job with data statistics
            job.setTransactionCount(database.size());
            job.setItemCount(profits.size());
            miningJobRepository.save(job);

            log.info("Loaded {} transactions and {} items for job: {}",
                database.size(), profits.size(), jobId);

            // Create and configure mining engine
            MiningEngine miningEngine = MiningEngine.builder()
                .itemProfits(profits)
                .k(k)
                .minProbability(minProbability)
                .build();

            log.debug("Starting mining algorithm for job: {}", jobId);

            // Execute mining
            List<Itemset> results = miningEngine.mine(database);

            // Calculate peak memory usage
            long currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long peakMemory = Math.max(startMemory, currentMemory);

            log.info("Mining completed for job: {} - found {} itemsets", jobId, results.size());

            // Clean up previous results for this job (if any)
            itemsetRepository.deleteByMiningJobId(jobId);

            // Save results to database
            if (!results.isEmpty()) {
                List<ItemsetEntity> entities = convertToEntities(results, jobId);
                itemsetRepository.saveAll(entities);
                log.debug("Saved {} itemset entities for job: {}", entities.size(), jobId);
            }

            // Update job completion status
            long executionTime = System.currentTimeMillis() - startTime;
            job.setStatus(MiningJobEntity.JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setExecutionTimeMs(executionTime);
            job.setItemsetsFound(results.size());
            job.setPeakMemoryMb(peakMemory / 1024 / 1024);
            miningJobRepository.save(job);

            // Cleanup resources
            miningEngine.shutdown();

            log.info("Mining job completed successfully: {} ({} ms, {} itemsets, {} MB peak memory)",
                jobId, executionTime, results.size(), peakMemory / 1024 / 1024);

        } catch (Exception e) {
            log.error("Mining job failed: {}", jobId, e);
            job.setStatus(MiningJobEntity.JobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            job.setErrorMessage(truncateErrorMessage(e.getMessage()));
            miningJobRepository.save(job);
            throw e;
        }
    }

    /**
     * Convert itemsets to database entities
     */
    private List<ItemsetEntity> convertToEntities(List<Itemset> itemsets, String jobId) {
        List<ItemsetEntity> entities = new ArrayList<>();

        for (int i = 0; i < itemsets.size(); i++) {
            Itemset itemset = itemsets.get(i);

            ItemsetEntity entity = ItemsetEntity.builder()
                .miningJobId(jobId)
                .items(convertItemsToJson(itemset.getItems()))
                .expectedUtility(itemset.getExpectedUtility())
                .probability(itemset.getProbability())
                .support(itemset.getSupport())
                .rank(i + 1)
                .itemsetSize(itemset.getItems().size())
                .createdAt(LocalDateTime.now())
                .build();

            entities.add(entity);
        }

        return entities;
    }

    /**
     * Convert items set to JSON string
     */
    private String convertItemsToJson(Set<Integer> items) {
        try {
            List<Integer> sortedItems = new ArrayList<>(items);
            Collections.sort(sortedItems);
            return objectMapper.writeValueAsString(sortedItems);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert items to JSON: {}", items, e);
            return items.toString();
        }
    }

    /**
     * Generate unique job ID
     */
    private String generateJobId() {
        return "job_" + System.currentTimeMillis() + "_" +
               UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Handle job errors
     */
    @Transactional
    private void handleJobError(String jobId, Exception e) {
        try {
            Optional<MiningJobEntity> jobOpt = miningJobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                MiningJobEntity job = jobOpt.get();
                job.setStatus(MiningJobEntity.JobStatus.FAILED);
                job.setCompletedAt(LocalDateTime.now());
                job.setErrorMessage(truncateErrorMessage(e.getMessage()));
                miningJobRepository.save(job);
            }
        } catch (Exception ex) {
            log.error("Failed to update job error status for {}", jobId, ex);
        }
    }

    /**
     * Truncate error message to fit database column
     */
    private String truncateErrorMessage(String message) {
        if (message == null) return "Unknown error";
        return message.length() > 1000 ? message.substring(0, 997) + "..." : message;
    }

    // === PUBLIC QUERY METHODS ===

    /**
     * Get top-K itemsets for a job
     */
    public List<ItemsetEntity> findTopKByJobId(String jobId, int k) {
        Pageable pageable = PageRequest.of(0, k);
        return itemsetRepository.findTopKByJobId(jobId, pageable);
    }

    /**
     * Get all itemsets for a job
     */
    public List<ItemsetEntity> findByMiningJobId(String jobId) {
        return itemsetRepository.findByMiningJobIdOrderByRankAsc(jobId);
    }

    /**
     * Get itemsets with pagination
     */
    public Page<ItemsetEntity> findItemsetsByJobId(String jobId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return itemsetRepository.findByMiningJobIdOrderByRankAsc(jobId, pageable);
    }

    /**
     * Get job by ID
     */
    public Optional<MiningJobEntity> findJobById(String jobId) {
        return miningJobRepository.findById(jobId);
    }

    /**
     * Get jobs with pagination
     */
    public Page<MiningJobEntity> findJobs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return miningJobRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Get jobs by status
     */
    public List<MiningJobEntity> findJobsByStatus(MiningJobEntity.JobStatus status) {
        return miningJobRepository.findByStatus(status);
    }

    /**
     * Get recent jobs (last N days)
     */
    public List<MiningJobEntity> findRecentJobs(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return miningJobRepository.findRecentJobs(since);
    }

    /**
     * Search itemsets
     */
    public List<ItemsetEntity> searchItemsets(String jobId, String pattern) {
        return itemsetRepository.searchItemsets(jobId, pattern);
    }

    /**
     * Get high-utility itemsets above threshold
     */
    public List<ItemsetEntity> findHighUtilityItemsets(String jobId, double threshold) {
        return itemsetRepository.findHighUtilityItemsets(jobId, threshold);
    }

    /**
     * Get itemsets by size
     */
    public List<ItemsetEntity> findItemsetsBySize(String jobId, int size) {
        return itemsetRepository.findByItemsetSize(jobId, size);
    }

    /**
     * Get mining statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalJobs", miningJobRepository.getTotalJobs());
        stats.put("completedJobs", miningJobRepository.getCompletedJobs());
        stats.put("failedJobs", miningJobRepository.getFailedJobs());
        stats.put("runningJobs", miningJobRepository.countByStatus(MiningJobEntity.JobStatus.RUNNING));
        stats.put("pendingJobs", miningJobRepository.countByStatus(MiningJobEntity.JobStatus.PENDING));

        // Execution time statistics
        stats.put("averageExecutionTime", miningJobRepository.getAverageExecutionTime().orElse(0.0));
        stats.put("maxExecutionTime", miningJobRepository.getMaxExecutionTime().orElse(0L));
        stats.put("minExecutionTime", miningJobRepository.getMinExecutionTime().orElse(0L));

        // Itemset statistics
        stats.put("totalItemsetsFound", miningJobRepository.getTotalItemsetsFound().orElse(0L));

        return stats;
    }

    /**
     * Get detailed job statistics
     */
    public Map<String, Object> getJobStatistics(String jobId) {
        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        long itemsetCount = itemsetRepository.countByMiningJobId(jobId);
        stats.put("totalItemsets", itemsetCount);

        if (itemsetCount > 0) {
            // Utility statistics
            Object[] utilityStats = itemsetRepository.getUtilityStatistics(jobId);
            if (utilityStats != null && utilityStats.length == 3) {
                stats.put("minUtility", utilityStats[0]);
                stats.put("maxUtility", utilityStats[1]);
                stats.put("avgUtility", utilityStats[2]);
            }

            // Probability statistics
            Object[] probStats = itemsetRepository.getProbabilityStatistics(jobId);
            if (probStats != null && probStats.length == 3) {
                stats.put("minProbability", probStats[0]);
                stats.put("maxProbability", probStats[1]);
                stats.put("avgProbability", probStats[2]);
            }

            // Size distribution
            List<Object[]> sizeDistribution = itemsetRepository.getItemsetSizeDistribution(jobId);
            Map<Integer, Long> sizeMap = new HashMap<>();
            for (Object[] row : sizeDistribution) {
                sizeMap.put((Integer) row[0], (Long) row[1]);
            }
            stats.put("sizeDistribution", sizeMap);
        }

        return stats;
    }

    /**
     * Delete a job and its results
     */
    @Transactional
    public void deleteJob(String jobId) {
        log.info("Deleting job and all related data: {}", jobId);
        itemsetRepository.deleteByMiningJobId(jobId);
        miningJobRepository.deleteById(jobId);
    }

    /**
     * Delete multiple jobs
     */
    @Transactional
    public void deleteJobs(List<String> jobIds) {
        for (String jobId : jobIds) {
            deleteJob(jobId);
        }
    }

    /**
     * Cancel a running job (mark as failed)
     */
    @Transactional
    public boolean cancelJob(String jobId) {
        Optional<MiningJobEntity> jobOpt = miningJobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            MiningJobEntity job = jobOpt.get();
            if (job.getStatus() == MiningJobEntity.JobStatus.RUNNING ||
                job.getStatus() == MiningJobEntity.JobStatus.PENDING) {

                job.setStatus(MiningJobEntity.JobStatus.FAILED);
                job.setCompletedAt(LocalDateTime.now());
                job.setErrorMessage("Job cancelled by user");
                miningJobRepository.save(job);

                log.info("Cancelled job: {}", jobId);
                return true;
            }
        }
        return false;
    }
}