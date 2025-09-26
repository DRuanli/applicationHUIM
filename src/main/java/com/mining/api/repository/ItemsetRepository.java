// File: src/main/java/com/mining/api/repository/ItemsetRepository.java
package com.mining.api.repository;

import com.mining.api.entity.ItemsetEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface ItemsetRepository extends JpaRepository<ItemsetEntity, Long> {
    
    /**
     * Delete all itemsets for a specific mining job
     */
    @Modifying
    @Transactional
    void deleteByMiningJobId(String miningJobId);
    
    /**
     * Find all itemsets by job ID ordered by rank
     */
    List<ItemsetEntity> findByMiningJobIdOrderByRankAsc(String miningJobId);
    
    /**
     * Find all itemsets by job ID ordered by utility (descending)
     */
    List<ItemsetEntity> findByMiningJobIdOrderByExpectedUtilityDesc(String miningJobId);
    
    /**
     * Find top-K itemsets by job ID with pagination
     */
    @Query("SELECT i FROM ItemsetEntity i WHERE i.miningJobId = :jobId " +
           "ORDER BY i.expectedUtility DESC")
    List<ItemsetEntity> findTopKByJobId(@Param("jobId") String jobId, Pageable pageable);
    
    /**
     * Find itemsets by job ID with pagination (ordered by rank)
     */
    Page<ItemsetEntity> findByMiningJobIdOrderByRankAsc(String miningJobId, Pageable pageable);
    
    /**
     * Find itemsets with utility above threshold
     */
    @Query("SELECT i FROM ItemsetEntity i WHERE i.miningJobId = :jobId " +
           "AND i.expectedUtility >= :threshold ORDER BY i.expectedUtility DESC")
    List<ItemsetEntity> findHighUtilityItemsets(
        @Param("jobId") String jobId, 
        @Param("threshold") Double threshold
    );
    
    /**
     * Find itemsets by size (number of items)
     */
    @Query("SELECT i FROM ItemsetEntity i WHERE i.miningJobId = :jobId " +
           "AND i.itemsetSize = :size ORDER BY i.expectedUtility DESC")
    List<ItemsetEntity> findByItemsetSize(
        @Param("jobId") String jobId, 
        @Param("size") Integer size
    );
    
    /**
     * Find itemsets containing specific item
     */
    @Query("SELECT i FROM ItemsetEntity i WHERE i.miningJobId = :jobId " +
           "AND i.items LIKE %:item% ORDER BY i.expectedUtility DESC")
    List<ItemsetEntity> findItemsetsContaining(
        @Param("jobId") String jobId, 
        @Param("item") String item
    );
    
    /**
     * Count itemsets for a job
     */
    long countByMiningJobId(String miningJobId);
    
    /**
     * Count high-utility itemsets (above threshold)
     */
    @Query("SELECT COUNT(i) FROM ItemsetEntity i WHERE i.miningJobId = :jobId " +
           "AND i.expectedUtility >= :threshold")
    long countHighUtilityItemsets(
        @Param("jobId") String jobId, 
        @Param("threshold") Double threshold
    );
    
    /**
     * Get utility statistics for a job
     */
    @Query("SELECT MIN(i.expectedUtility), MAX(i.expectedUtility), AVG(i.expectedUtility) " +
           "FROM ItemsetEntity i WHERE i.miningJobId = :jobId")
    Object[] getUtilityStatistics(@Param("jobId") String jobId);
    
    /**
     * Get probability statistics for a job
     */
    @Query("SELECT MIN(i.probability), MAX(i.probability), AVG(i.probability) " +
           "FROM ItemsetEntity i WHERE i.miningJobId = :jobId")
    Object[] getProbabilityStatistics(@Param("jobId") String jobId);
    
    /**
     * Get itemset size distribution
     */
    @Query("SELECT i.itemsetSize, COUNT(i) FROM ItemsetEntity i " +
           "WHERE i.miningJobId = :jobId GROUP BY i.itemsetSize ORDER BY i.itemsetSize")
    List<Object[]> getItemsetSizeDistribution(@Param("jobId") String jobId);
    
    /**
     * Find top itemsets across all jobs
     */
    @Query("SELECT i FROM ItemsetEntity i ORDER BY i.expectedUtility DESC")
    List<ItemsetEntity> findGlobalTopItemsets(Pageable pageable);
    
    /**
     * Search itemsets by pattern in items
     */
    @Query("SELECT i FROM ItemsetEntity i WHERE i.miningJobId = :jobId " +
           "AND (i.items LIKE %:pattern% OR CAST(i.expectedUtility AS string) LIKE %:pattern%) " +
           "ORDER BY i.expectedUtility DESC")
    List<ItemsetEntity> searchItemsets(
        @Param("jobId") String jobId, 
        @Param("pattern") String pattern
    );
}