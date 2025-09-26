// File: src/main/java/com/mining/api/service/AsyncMiningService.java
package com.mining.api.service;

import com.mining.api.dto.request.MiningRequest;
import com.mining.api.dto.response.MiningResponse;
import com.mining.api.entity.MiningJob;
import com.mining.api.repository.MiningJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMiningService {
    
    private final MiningService miningService;
    private final MiningJobRepository jobRepository;
    
    public MiningResponse startMiningAsync(MiningRequest request) {
        // Create job
        MiningJob job = MiningJob.builder()
            .databaseFile(request.getDatabaseFile())
            .profitFile(request.getProfitFile())
            .k(request.getK())
            .minProbability(request.getMinProbability())
            .status(MiningJob.JobStatus.PENDING)
            .build();
        
        job = jobRepository.save(job);
        
        // Start async processing
        processMiningAsync(request, job.getId());
        
        return MiningResponse.builder()
            .jobId(job.getId())
            .status(job.getStatus().toString())
            .message("Mining job started successfully")
            .build();
    }
    
    @Async("miningExecutor")
    public CompletableFuture<MiningResponse> processMiningAsync(MiningRequest request, String jobId) {
        try {
            log.info("Starting async mining for job: {}", jobId);
            MiningResponse response = miningService.executeMining(request, jobId);
            log.info("Completed async mining for job: {}", jobId);
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Async mining failed for job {}: {}", jobId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
}