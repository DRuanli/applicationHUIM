// File: src/main/java/com/mining/api/entity/MiningJob.java
package com.mining.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mining_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "itemsets")
@ToString(exclude = "itemsets")
public class MiningJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String databaseFile;
    
    @Column(nullable = false)
    private String profitFile;
    
    @Column(nullable = false)
    private Integer k;
    
    @Column(nullable = false)
    private Double minProbability;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;
    
    @Column
    private Long executionTimeMs;
    
    @Column
    private Long peakMemoryUsage;
    
    @Column
    private Double pruningEffectiveness;
    
    @Column
    private Integer itemsetsFound;
    
    @Column
    private Double threshold;
    
    @Column
    private Integer candidatesGenerated;
    
    @Column
    private Integer candidatesPruned;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime completedAt;
    
    @Column(length = 1000)
    private String errorMessage;
    
    @OneToMany(mappedBy = "miningJob", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<ItemsetEntity> itemsets = new ArrayList<>();
    
    public enum JobStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}