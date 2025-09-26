// File: src/main/java/com/mining/api/dto/request/MiningRequest.java
package com.mining.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MiningRequest {
    
    @NotBlank(message = "Database file is required")
    private String databaseFile;
    
    @NotBlank(message = "Profit file is required")
    private String profitFile;
    
    @NotNull(message = "K value is required")
    @Positive(message = "K must be positive")
    @Max(value = 1000, message = "K cannot exceed 1000")
    private Integer k;
    
    @NotNull(message = "Minimum probability is required")
    @DecimalMin(value = "0.0", message = "Minimum probability must be >= 0")
    @DecimalMax(value = "1.0", message = "Minimum probability must be <= 1")
    private Double minProbability;
    
    private Boolean useParallel = true;
    private Integer numThreads;
}