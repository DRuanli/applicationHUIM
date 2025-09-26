// File: src/main/java/com/mining/api/dto/response/ItemsetResponse.java
package com.mining.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class ItemsetResponse {
    private Set<Integer> items;
    private Double expectedUtility;
    private Double probability;
    private Integer support;
    private Integer rank;
}