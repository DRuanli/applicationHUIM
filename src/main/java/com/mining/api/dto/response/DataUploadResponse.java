// File: src/main/java/com/mining/api/dto/response/DataUploadResponse.java
package com.mining.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DataUploadResponse {
    private String id;
    private String fileName;
    private String fileType;
    private String filePath;
    private Long fileSize;
    private Integer transactionCount;
    private Integer itemCount;
    private LocalDateTime uploadedAt;
    private String message;
}