// File: src/main/java/com/mining/api/service/WebSocketService.java
package com.mining.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    public void sendUpdate(String jobId, String type, Object data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", type);
        message.put("jobId", jobId);
        message.put("data", data);
        message.put("timestamp", System.currentTimeMillis());
        
        String destination = "/topic/mining/" + jobId;
        messagingTemplate.convertAndSend(destination, message);
        log.debug("Sent WebSocket update to {}: type={}", destination, type);
    }
    
    public void sendProgress(String jobId, int percentage, String status) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("percentage", percentage);
        progress.put("status", status);
        sendUpdate(jobId, "progress", progress);
    }
    
    public void broadcastStatistics(Map<String, Object> statistics) {
        messagingTemplate.convertAndSend("/topic/statistics", statistics);
    }
}