// File: src/main/java/com/mining/api/dto/response/MiningResponse.java
package com.mining.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MiningResponse {
    private String jobId;
    private String status;
    private String message;
    private Integer k;
    private Double minProbability;
    private List<ItemsetResponse> itemsets;
    private Long executionTimeMs;
    private Integer itemsetsFound;
    private Double threshold;
    private Double pruningEffectiveness;
    private Long peakMemoryUsageMB;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}