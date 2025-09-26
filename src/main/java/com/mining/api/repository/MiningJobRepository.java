// File: src/main/java/com/mining/api/repository/MiningJobRepository.java
package com.mining.api.repository;

import com.mining.api.entity.MiningJobEntity;
import com.mining.api.entity.MiningJobEntity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MiningJobRepository extends JpaRepository<MiningJobEntity, String> {

    /**
     * Find jobs by status
     */
    List<MiningJobEntity> findByStatus(JobStatus status);

    /**
     * Find jobs by status with pagination
     */
    Page<MiningJobEntity> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Find jobs ordered by creation date (newest first)
     */
    Page<MiningJobEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find jobs created after a specific date
     */
    List<MiningJobEntity> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find jobs by k value
     */
    List<MiningJobEntity> findByK(Integer k);

    /**
     * Find completed jobs with execution time within range
     */
    @Query("SELECT j FROM MiningJobEntity j WHERE j.status = 'COMPLETED' " +
           "AND j.executionTimeMs BETWEEN :minTime AND :maxTime " +
           "ORDER BY j.executionTimeMs ASC")
    List<MiningJobEntity> findCompletedJobsByExecutionTimeRange(
        @Param("minTime") Long minTime,
        @Param("maxTime") Long maxTime
    );

    /**
     * Find recent jobs (last N days)
     */
    @Query("SELECT j FROM MiningJobEntity j WHERE j.createdAt >= :since " +
           "ORDER BY j.createdAt DESC")
    List<MiningJobEntity> findRecentJobs(@Param("since") LocalDateTime since);

    // Statistics queries

    /**
     * Count jobs by status
     */
    long countByStatus(JobStatus status);

    /**
     * Get total number of jobs
     */
    @Query("SELECT COUNT(j) FROM MiningJobEntity j")
    long getTotalJobs();

    /**
     * Get number of completed jobs
     */
    @Query("SELECT COUNT(j) FROM MiningJobEntity j WHERE j.status = 'COMPLETED'")
    long getCompletedJobs();

    /**
     * Get number of failed jobs
     */
    @Query("SELECT COUNT(j) FROM MiningJobEntity j WHERE j.status = 'FAILED'")
    long getFailedJobs();

    /**
     * Get average execution time for completed jobs
     */
    @Query("SELECT AVG(j.executionTimeMs) FROM MiningJobEntity j WHERE j.status = 'COMPLETED'")
    Optional<Double> getAverageExecutionTime();

    /**
     * Get total itemsets found across all completed jobs
     */
    @Query("SELECT SUM(j.itemsetsFound) FROM MiningJobEntity j WHERE j.status = 'COMPLETED'")
    Optional<Long> getTotalItemsetsFound();

    /**
     * Get maximum execution time
     */
    @Query("SELECT MAX(j.executionTimeMs) FROM MiningJobEntity j WHERE j.status = 'COMPLETED'")
    Optional<Long> getMaxExecutionTime();

    /**
     * Get minimum execution time
     */
    @Query("SELECT MIN(j.executionTimeMs) FROM MiningJobEntity j WHERE j.status = 'COMPLETED'")
    Optional<Long> getMinExecutionTime();

    /**
     * Find top performing jobs by itemsets found
     */
    @Query("SELECT j FROM MiningJobEntity j WHERE j.status = 'COMPLETED' " +
           "ORDER BY j.itemsetsFound DESC")
    List<MiningJobEntity> findTopPerformingJobs(Pageable pageable);

    /**
     * Find fastest completed jobs
     */
    @Query("SELECT j FROM MiningJobEntity j WHERE j.status = 'COMPLETED' " +
           "ORDER BY j.executionTimeMs ASC")
    List<MiningJobEntity> findFastestJobs(Pageable pageable);
}