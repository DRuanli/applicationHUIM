// File: src/main/java/com/mining/api/service/DataService.java
package com.mining.api.service;

import com.mining.api.dto.response.DataUploadResponse;
import com.mining.api.entity.DataFile;
import com.mining.api.exception.MiningException;
import com.mining.api.repository.DataFileRepository;
import com.mining.core.model.Transaction;
import com.mining.io.DataLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataService {
    
    private final DataFileRepository dataFileRepository;
    private final DataLoader dataLoader;
    
    @Value("${app.upload.dir:data/uploads}")
    private String uploadDir;
    
    public DataUploadResponse uploadFile(MultipartFile file, String fileType) {
        try {
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            // Save file to disk
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Calculate checksum
            String checksum = calculateChecksum(filePath);
            
            // Create database record
            DataFile dataFile = DataFile.builder()
                .fileName(originalFilename)
                .fileType(fileType)
                .filePath(filePath.toString())
                .fileSize(file.getSize())
                .checksum(checksum)
                .build();
            
            // Parse file to get metadata
            if ("database".equals(fileType)) {
                List<Transaction> transactions = dataLoader.readDatabase(filePath.toString());
                dataFile.setTransactionCount(transactions.size());
                // Count unique items
                int uniqueItems = transactions.stream()
                    .flatMap(t -> t.getItemIds().stream())
                    .collect(Collectors.toSet())
                    .size();
                dataFile.setItemCount(uniqueItems);
            } else if ("profit".equals(fileType)) {
                Map<Integer, Double> profits = dataLoader.readProfitTable(filePath.toString());
                dataFile.setItemCount(profits.size());
            }
            
            dataFile = dataFileRepository.save(dataFile);
            
            return DataUploadResponse.builder()
                .id(dataFile.getId())
                .fileName(dataFile.getFileName())
                .fileType(dataFile.getFileType())
                .filePath(dataFile.getFilePath())
                .fileSize(dataFile.getFileSize())
                .transactionCount(dataFile.getTransactionCount())
                .itemCount(dataFile.getItemCount())
                .uploadedAt(dataFile.getUploadedAt())
                .build();
            
        } catch (IOException e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            throw new MiningException("File upload failed: " + e.getMessage());
        }
    }
    
    public List<DataUploadResponse> getAllFiles() {
        return dataFileRepository.findAll().stream()
            .map(this::toDataUploadResponse)
            .collect(Collectors.toList());
    }
    
    public void deleteFile(String fileId) {
        DataFile dataFile = dataFileRepository.findById(fileId)
            .orElseThrow(() -> new MiningException("File not found: " + fileId));
        
        // Delete file from disk
        try {
            Path filePath = Paths.get(dataFile.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file from disk: {}", e.getMessage());
        }
        
        // Delete from database
        dataFileRepository.delete(dataFile);
    }
    
    public Map<String, Object> validateFile(String fileId) {
        DataFile dataFile = dataFileRepository.findById(fileId)
            .orElseThrow(() -> new MiningException("File not found: " + fileId));
        
        Map<String, Object> validation = new HashMap<>();
        validation.put("valid", true);
        validation.put("fileName", dataFile.getFileName());
        validation.put("fileType", dataFile.getFileType());
        
        try {
            if ("database".equals(dataFile.getFileType())) {
                List<Transaction> transactions = dataLoader.readDatabase(dataFile.getFilePath());
                validation.put("transactionCount", transactions.size());
                validation.put("message", "Database file is valid");
            } else if ("profit".equals(dataFile.getFileType())) {
                Map<Integer, Double> profits = dataLoader.readProfitTable(dataFile.getFilePath());
                validation.put("itemCount", profits.size());
                validation.put("message", "Profit table is valid");
            }
        } catch (Exception e) {
            validation.put("valid", false);
            validation.put("message", "File validation failed: " + e.getMessage());
        }
        
        return validation;
    }
    
    private String calculateChecksum(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = Files.readAllBytes(filePath);
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private DataUploadResponse toDataUploadResponse(DataFile dataFile) {
        return DataUploadResponse.builder()
            .id(dataFile.getId())
            .fileName(dataFile.getFileName())
            .fileType(dataFile.getFileType())
            .filePath(dataFile.getFilePath())
            .fileSize(dataFile.getFileSize())
            .transactionCount(dataFile.getTransactionCount())
            .itemCount(dataFile.getItemCount())
            .uploadedAt(dataFile.getUploadedAt())
            .build();
    }
}