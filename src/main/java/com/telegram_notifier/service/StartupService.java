package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class StartupService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StartupService.class);

    private final AppProperties properties;
    private final JobNotificationService jobNotificationService;
    private final StorageService storageService;

    public StartupService(AppProperties properties, 
                         JobNotificationService jobNotificationService,
                         StorageService storageService) {
        this.properties = properties;
        this.jobNotificationService = jobNotificationService;
        this.storageService = storageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=".repeat(80));
        log.info("Telegram Job Notifier Application Started Successfully!");
        log.info("=".repeat(80));
        
        log.info("Configuration:");
        log.info("  API ID: {}", properties.getTelegram().getApiId());
        log.info("  Phone Number: {}", properties.getTelegram().getPhoneNumber());
        log.info("  Channels: {}", properties.getTelegram().getChannels());
        log.info("  Poll Interval: {} minutes", properties.getTelegram().getPollIntervalMinutes());
        log.info("  AI Model: {}", properties.getAi().getGemini().getModel());
        log.info("  Storage: {}", storageService.getStorageInfo());
        
        log.info("Available endpoints:");
        log.info("  POST /api/jobs/process-manual - Process a manual job posting");
        log.info("  POST /api/jobs/process-now - Trigger immediate message processing");
        log.info("  POST /api/jobs/init-storage - Initialize storage");
        log.info("  GET  /api/jobs/storage-info - View storage information");
        log.info("  GET  /api/jobs/health - Health check");
        
        log.info("=".repeat(80));
        
        // Initialize storage on startup
        try {
            log.info("Initializing storage...");
            jobNotificationService.initializeStorage();
        } catch (Exception e) {
            log.warn("Could not initialize storage on startup", e);
        }
    }
}
