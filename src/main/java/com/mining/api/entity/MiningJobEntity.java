// File: src/main/java/com/mining/api/entity/MiningJobEntity.java
package com.mining.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "mining_jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiningJobEntity {

    @Id
    @Column(name = "job_id", length = 50)
    private String jobId;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "k_value", nullable = false)
    private Integer k;

    @Column(name = "min_probability", nullable = false)
    private Double minProbability;

    @Column(name = "database_filename", length = 500)
    private String databaseFilename;

    @Column(name = "profit_filename", length = 500)
    private String profitFilename;

    @Column(name = "itemsets_found")
    @Builder.Default
    private Integer itemsetsFound = 0;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "started_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedAt;

    // Additional metadata
    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "item_count")
    private Integer itemCount;

    @Column(name = "peak_memory_mb")
    private Long peakMemoryMb;

    public enum JobStatus {
        PENDING("Job created and waiting to start"),
        RUNNING("Mining algorithm is executing"),
        COMPLETED("Job completed successfully"),
        FAILED("Job failed with error");

        private final String description;

        JobStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Get execution time in seconds
     */
    public Double getExecutionTimeSeconds() {
        return executionTimeMs != null ? executionTimeMs / 1000.0 : null;
    }

    /**
     * Check if job is completed (success or failure)
     */
    public boolean isCompleted() {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }

    /**
     * Check if job is currently running
     */
    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }
}
