package com.mining.api.controller;

import com.mining.api.entity.ItemsetEntity;
import com.mining.api.entity.MiningJobEntity;
import com.mining.api.service.MiningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/mining")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MiningController {

    private final MiningService miningService;
    private static final String UPLOAD_DIR = "uploads";

    // ========== JOB MANAGEMENT ==========

    /**
     * Start a new mining job with file uploads
     */
    @PostMapping("/jobs")
    public ResponseEntity<Map<String, Object>> startMiningJob(
            @RequestParam("databaseFile") MultipartFile databaseFile,
            @RequestParam("profitFile") MultipartFile profitFile,
            @RequestParam("k") @Min(1) @Max(1000) int k,
            @RequestParam("minProbability") @Min(0) @Max(1) double minProbability) {

        try {
            // Validate files
            validateUploadedFile(databaseFile, "Database file");
            validateUploadedFile(profitFile, "Profit file");

            // Save uploaded files
            String dbPath = saveUploadedFile(databaseFile, "database_");
            String profitPath = saveUploadedFile(profitFile, "profit_");

            log.info("Starting mining job: k={}, minProb={}, dbFile={}, profitFile={}",
                k, minProbability, databaseFile.getOriginalFilename(), profitFile.getOriginalFilename());

            // Start mining job asynchronously
            CompletableFuture<String> jobFuture = miningService.startMiningJob(
                dbPath, profitPath, k, minProbability);

            // Wait for job creation (not completion)
            String jobId = jobFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "PENDING");
            response.put("message", "Mining job started successfully");
            response.put("k", k);
            response.put("minProbability", minProbability);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to start mining job", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start mining job: " + e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Start mining job with file paths (for testing/development)
     */
    @PostMapping("/jobs/paths")
    public ResponseEntity<Map<String, Object>> startMiningJobWithPaths(
            @RequestParam("databasePath") String databasePath,
            @RequestParam("profitPath") String profitPath,
            @RequestParam("k") @Min(1) @Max(1000) int k,
            @RequestParam("minProbability") @Min(0) @Max(1) double minProbability) {

        try {
            log.info("Starting mining job with paths: k={}, minProb={}, dbPath={}, profitPath={}",
                k, minProbability, databasePath, profitPath);

            CompletableFuture<String> jobFuture = miningService.startMiningJob(
                databasePath, profitPath, k, minProbability);

            String jobId = jobFuture.get();

            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobId);
            response.put("status", "PENDING");
            response.put("message", "Mining job started successfully");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to start mining job with paths", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to start mining job: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get job status and details
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<MiningJobEntity> getJob(@PathVariable String jobId) {
        log.debug("Getting job details for: {}", jobId);
        Optional<MiningJobEntity> job = miningService.findJobById(jobId);
        return job.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all jobs with pagination
     */
    @GetMapping("/jobs")
    public ResponseEntity<Page<MiningJobEntity>> getJobs(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        log.debug("Getting jobs: page={}, size={}", page, size);
        Page<MiningJobEntity> jobs = miningService.findJobs(page, size);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get jobs by status
     */
    @GetMapping("/jobs/status/{status}")
    public ResponseEntity<List<MiningJobEntity>> getJobsByStatus(
            @PathVariable MiningJobEntity.JobStatus status) {

        log.debug("Getting jobs by status: {}", status);
        List<MiningJobEntity> jobs = miningService.findJobsByStatus(status);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get recent jobs
     */
    @GetMapping("/jobs/recent")
    public ResponseEntity<List<MiningJobEntity>> getRecentJobs(
            @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days) {

        log.debug("Getting recent jobs for last {} days", days);
        List<MiningJobEntity> jobs = miningService.findRecentJobs(days);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Cancel a running job
     */
    @PutMapping("/jobs/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        log.info("Cancelling job: {}", jobId);

        boolean cancelled = miningService.cancelJob(jobId);

        Map<String, Object> response = new HashMap<>();
        if (cancelled) {
            response.put("message", "Job cancelled successfully");
            response.put("jobId", jobId);
            return ResponseEntity.ok(response);
        } else {
            response.put("error", "Job not found or cannot be cancelled");
            response.put("jobId", jobId);
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Delete a job and its results
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable String jobId) {
        log.info("Deleting job: {}", jobId);

        try {
            miningService.deleteJob(jobId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Job deleted successfully");
            response.put("jobId", jobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete job: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // ========== ITEMSET QUERIES ==========

    /**
     * Get itemsets for a job (with pagination)
     */
    @GetMapping("/jobs/{jobId}/itemsets")
    public ResponseEntity<Page<ItemsetEntity>> getJobItemsets(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        log.debug("Getting itemsets for job: {}, page={}, size={}", jobId, page, size);
        Page<ItemsetEntity> itemsets = miningService.findItemsetsByJobId(jobId, page, size);
        return ResponseEntity.ok(itemsets);
    }

    /**
     * Get all itemsets for a job (no pagination)
     */
    @GetMapping("/jobs/{jobId}/itemsets/all")
    public ResponseEntity<List<ItemsetEntity>> getAllJobItemsets(@PathVariable String jobId) {
        log.debug("Getting all itemsets for job: {}", jobId);
        List<ItemsetEntity> itemsets = miningService.findByMiningJobId(jobId);
        return ResponseEntity.ok(itemsets);
    }

    /**
     * Get top-K itemsets for a job
     */
    @GetMapping("/jobs/{jobId}/itemsets/top/{k}")
    public ResponseEntity<List<ItemsetEntity>> getTopKItemsets(
            @PathVariable String jobId,
            @PathVariable @Min(1) @Max(1000) int k) {

        log.debug("Getting top-{} itemsets for job: {}", k, jobId);
        List<ItemsetEntity> itemsets = miningService.findTopKByJobId(jobId, k);
        return ResponseEntity.ok(itemsets);
    }

    /**
     * Search itemsets by pattern
     */
    @GetMapping("/jobs/{jobId}/itemsets/search")
    public ResponseEntity<List<ItemsetEntity>> searchItemsets(
            @PathVariable String jobId,
            @RequestParam String pattern) {

        log.debug("Searching itemsets in job: {} with pattern: {}", jobId, pattern);
        List<ItemsetEntity> itemsets = miningService.searchItemsets(jobId, pattern);
        return ResponseEntity.ok(itemsets);
    }

    /**
     * Get high-utility itemsets above threshold
     */
    @GetMapping("/jobs/{jobId}/itemsets/high-utility")
    public ResponseEntity<List<ItemsetEntity>> getHighUtilityItemsets(
            @PathVariable String jobId,
            @RequestParam double threshold) {

        log.debug("Getting high-utility itemsets for job: {} above threshold: {}", jobId, threshold);
        List<ItemsetEntity> itemsets = miningService.findHighUtilityItemsets(jobId, threshold);
        return ResponseEntity.ok(itemsets);
    }

    /**
     * Get itemsets by size
     */
    @GetMapping("/jobs/{jobId}/itemsets/size/{size}")
    public ResponseEntity<List<ItemsetEntity>> getItemsetsBySize(
            @PathVariable String jobId,
            @PathVariable @Min(1) @Max(50) int size) {

        log.debug("Getting itemsets of size {} for job: {}", size, jobId);
        List<ItemsetEntity> itemsets = miningService.findItemsetsBySize(jobId, size);
        return ResponseEntity.ok(itemsets);
    }

    // ========== STATISTICS ==========

    /**
     * Get overall mining statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.debug("Getting mining statistics");
        Map<String, Object> stats = miningService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get detailed statistics for a specific job
     */
    @GetMapping("/jobs/{jobId}/statistics")
    public ResponseEntity<Map<String, Object>> getJobStatistics(@PathVariable String jobId) {
        log.debug("Getting statistics for job: {}", jobId);
        Map<String, Object> stats = miningService.getJobStatistics(jobId);
        return ResponseEntity.ok(stats);
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "PTK-HUIM Mining API");
        status.put("version", "2.0.0");
        status.put("timestamp", System.currentTimeMillis());

        // Add basic system info
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("processors", runtime.availableProcessors());
        systemInfo.put("maxMemoryMB", runtime.maxMemory() / 1024 / 1024);
        systemInfo.put("totalMemoryMB", runtime.totalMemory() / 1024 / 1024);
        systemInfo.put("freeMemoryMB", runtime.freeMemory() / 1024 / 1024);
        status.put("system", systemInfo);

        return ResponseEntity.ok(status);
    }

    /**
     * Get API information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "PTK-HUIM Mining API");
        info.put("version", "2.0.0");
        info.put("description", "Enhanced Parallel Top-K High-Utility Itemset Mining");
        info.put("endpoints", Map.of(
            "jobs", "/api/mining/jobs",
            "statistics", "/api/mining/statistics",
            "health", "/api/mining/health"
        ));
        return ResponseEntity.ok(info);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Validate uploaded file
     */
    private void validateUploadedFile(MultipartFile file, String description) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException(description + " is required");
        }

        if (file.getSize() > 50 * 1024 * 1024) { // 50MB limit
            throw new IllegalArgumentException(description + " is too large (max 50MB)");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException(description + " must have a valid filename");
        }
    }

    /**
     * Save uploaded file to temporary directory
     */
    private String saveUploadedFile(MultipartFile file, String prefix) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String filename = prefix + timestamp + extension;
        Path filePath = uploadPath.resolve(filename);

        // Save file
        file.transferTo(filePath.toFile());

        log.debug("Saved uploaded file: {} -> {}", originalFilename, filePath.toString());
        return filePath.toString();
    }

    // ========== EXCEPTION HANDLING ==========

    /**
     * Handle validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleValidationError(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Validation failed");
        error.put("message", e.getMessage());
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Handle general errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception e) {
        log.error("Unexpected error in controller", e);
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", "An unexpected error occurred");
        error.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}