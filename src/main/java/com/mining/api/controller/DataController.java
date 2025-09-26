// File: src/main/java/com/mining/api/controller/DataController.java
package com.mining.api.controller;

import com.mining.api.dto.response.DataUploadResponse;
import com.mining.api.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data", description = "Data management API")
@CrossOrigin(origins = "*")
public class DataController {
    
    private final DataService dataService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload data file")
    public ResponseEntity<DataUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String fileType) {
        
        log.info("Uploading {} file: {}", fileType, file.getOriginalFilename());
        DataUploadResponse response = dataService.uploadFile(file, fileType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/files")
    @Operation(summary = "List uploaded files")
    public ResponseEntity<List<DataUploadResponse>> listFiles() {
        return ResponseEntity.ok(dataService.getAllFiles());
    }
    
    @DeleteMapping("/files/{fileId}")
    @Operation(summary = "Delete file")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        dataService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/validate/{fileId}")
    @Operation(summary = "Validate file")
    public ResponseEntity<?> validateFile(@PathVariable String fileId) {
        return ResponseEntity.ok(dataService.validateFile(fileId));
    }
}