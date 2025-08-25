package com.telegram_notifier.controller;

import com.telegram_notifier.service.JobNotificationService;
import com.telegram_notifier.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobNotificationController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobNotificationController.class);

    private final JobNotificationService jobNotificationService;
    private final StorageService storageService;

    public JobNotificationController(JobNotificationService jobNotificationService, StorageService storageService) {
        this.jobNotificationService = jobNotificationService;
        this.storageService = storageService;
    }

    @PostMapping("/process-manual")
    public ResponseEntity<Map<String, Object>> processManualMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        try {
            Map<String, Object> result = jobNotificationService.processSingleMessage(message);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing manual message", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process message"));
        }
    }

    @PostMapping("/process-now")
    public ResponseEntity<Map<String, Object>> processNow() {
        try {
            Map<String, Object> result = jobNotificationService.processMessagesManually();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error processing messages", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to process messages"));
        }
    }

    @PostMapping("/init-storage")
    public ResponseEntity<Map<String, String>> initializeStorage() {
        try {
            jobNotificationService.initializeStorage();
            return ResponseEntity.ok(Map.of(
                "status", "success", 
                "message", "Storage initialized successfully",
                "storage", storageService.getStorageInfo()
            ));
        } catch (Exception e) {
            log.error("Error initializing storage", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to initialize storage"));
        }
    }

    @GetMapping("/storage-info")
    public ResponseEntity<Map<String, String>> getStorageInfo() {
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "storage", storageService.getStorageInfo()
        ));
    }

    @PostMapping("/reset-timestamps")
    public ResponseEntity<Map<String, Object>> resetTimestamps() {
        log.info("Reset channel timestamps requested");
        
        try {
            jobNotificationService.resetChannelTimestamps();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "All channel timestamps reset - next processing will treat recent messages as new"
            ));
        } catch (Exception e) {
            log.error("Error resetting timestamps", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to reset timestamps: " + e.getMessage()));
        }
    }
    
    @GetMapping("/processing-status")
    public ResponseEntity<Map<String, Object>> getProcessingStatus() {
        try {
            Map<String, Object> status = jobNotificationService.getProcessingStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting processing status", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get status: " + e.getMessage()));
        }
    }
    
    @PostMapping("/test-notion")
    public ResponseEntity<Map<String, Object>> testNotion() {
        try {
            // We need to inject NotionStorageService to test it
            Map<String, Object> result = new HashMap<>();
            result.put("status", "info");
            result.put("message", "Test Notion connection manually with /processing-status to see storage info");
            result.put("current_storage", storageService.getStorageInfo());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing Notion", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to test Notion: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy", 
            "service", "Telegram Job Notifier with Timestamps",
            "storage", storageService.getStorageInfo()
        ));
    }
}