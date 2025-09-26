// File: src/main/java/com/mining/api/entity/DataFile.java
package com.mining.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataFile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileType; // "database" or "profit"
    
    @Column(nullable = false)
    private String filePath;
    
    @Column
    private Long fileSize;
    
    @Column
    private Integer transactionCount;
    
    @Column
    private Integer itemCount;
    
    @Column
    private String checksum;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    
    @Column
    private String uploadedBy;
}