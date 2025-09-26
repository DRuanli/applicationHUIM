// File: src/main/java/com/mining/api/repository/DataFileRepository.java
package com.mining.api.repository;

import com.mining.api.entity.DataFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataFileRepository extends JpaRepository<DataFile, String> {
    
    List<DataFile> findByFileType(String fileType);
    
    Optional<DataFile> findByFileName(String fileName);
    
    List<DataFile> findByUploadedAtAfter(LocalDateTime date);
    
    void deleteByFileName(String fileName);
}