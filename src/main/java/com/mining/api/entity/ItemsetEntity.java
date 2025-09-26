// File: src/main/java/com/mining/api/entity/ItemsetEntity.java
package com.mining.api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "itemsets", indexes = {
    @Index(name = "idx_mining_job_id", columnList = "mining_job_id"),
    @Index(name = "idx_job_utility", columnList = "mining_job_id,expected_utility"),
    @Index(name = "idx_job_rank", columnList = "mining_job_id,rank_position")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemsetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mining_job_id", nullable = false, length = 50)
    private String miningJobId;

    @Column(name = "items", nullable = false, length = 2000)
    private String items; // JSON array of item IDs: [1,2,3]

    @Column(name = "expected_utility", nullable = false, precision = 15, scale = 6)
    private Double expectedUtility;

    @Column(name = "probability", nullable = false, precision = 10, scale = 6)
    private Double probability;

    @Column(name = "support_count")
    private Integer support;

    @Column(name = "rank_position", nullable = false)
    private Integer rank;

    @Column(name = "itemset_size")
    private Integer itemsetSize;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Reference to parent job (optional for queries)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mining_job_id", referencedColumnName = "job_id", insertable = false, updatable = false)
    private MiningJobEntity miningJob;

    /**
     * Get formatted itemset string for display
     */
    public String getFormattedItemset() {
        if (items == null) return "{}";

        // Remove brackets and format nicely
        String cleaned = items.replace("[", "{").replace("]", "}");
        return cleaned;
    }

    /**
     * Get utility formatted to specified decimal places
     */
    public String getFormattedUtility(int decimalPlaces) {
        if (expectedUtility == null) return "0.0";
        return String.format("%." + decimalPlaces + "f", expectedUtility);
    }

    /**
     * Get probability as percentage
     */
    public String getProbabilityPercentage() {
        if (probability == null) return "0%";
        return String.format("%.2f%%", probability * 100);
    }

    /**
     * Check if this is a high-utility itemset (above threshold)
     */
    public boolean isHighUtility(double threshold) {
        return expectedUtility != null && expectedUtility >= threshold;
    }
}