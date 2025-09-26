// File: src/main/java/com/mining/api/controller/ItemsetController.java
package com.mining.api.controller;

import com.mining.api.dto.response.ItemsetResponse;
import com.mining.api.service.MiningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/itemsets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Itemsets", description = "Itemset query API")
@CrossOrigin(origins = "*")
public class ItemsetController {
    
    private final MiningService miningService;
    
    @GetMapping("/top-k")
    @Operation(summary = "Get top-K itemsets")
    public ResponseEntity<List<ItemsetResponse>> getTopK(
            @RequestParam String jobId,
            @RequestParam(defaultValue = "10") int k) {
        
        List<ItemsetResponse> itemsets = miningService.getTopKItemsets(jobId, k);
        return ResponseEntity.ok(itemsets);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search itemsets")
    public ResponseEntity<Page<ItemsetResponse>> searchItemsets(
            @RequestParam(required = false) String jobId,
            @RequestParam(required = false) Double minUtility,
            @RequestParam(required = false) Double minProbability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<ItemsetResponse> results = miningService.searchItemsets(
            jobId, minUtility, minProbability, page, size);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/export/{jobId}")
    @Operation(summary = "Export itemsets")
    public ResponseEntity<String> exportItemsets(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "csv") String format) {
        
        String exportData = miningService.exportItemsets(jobId, format);
        
        HttpHeaders headers = new HttpHeaders();
        if ("csv".equals(format)) {
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "itemsets_" + jobId + ".csv");
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(exportData);
    }
}