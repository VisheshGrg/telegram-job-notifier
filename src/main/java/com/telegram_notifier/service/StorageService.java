package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class StorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageService.class);

    private final AppProperties properties;
    private final NotionStorageService notionStorageService;

    public StorageService(AppProperties properties, NotionStorageService notionStorageService) {
        this.properties = properties;
        this.notionStorageService = notionStorageService;
    }

    @PostConstruct
    public void init() {
        String storageType = properties.getStorage().getType();
        log.info("Initializing storage service with type: {}", storageType);
        
        if (!"notion".equalsIgnoreCase(storageType)) {
            log.warn("Storage type '{}' is not supported. Only 'notion' storage is available. Please update your configuration.", storageType);
            log.warn("Using Notion storage as the only available option.");
        }
        
        // Notion service initializes itself via @PostConstruct
        log.info("✅ Notion storage service is available and ready");
    }

    public void saveJob(JobDetails jobDetails) {
        saveJob(jobDetails, null);
    }
    
    public void saveJob(JobDetails jobDetails, String resumeLink) {
        try {
            if (resumeLink != null) {
                notionStorageService.saveJob(jobDetails, resumeLink);
                log.debug("💾 Job saved to Notion with resume link: {} - {}", jobDetails.getCompany(), jobDetails.getRole());
            } else {
                notionStorageService.saveJob(jobDetails);
                log.debug("💾 Job saved to Notion: {} - {}", jobDetails.getCompany(), jobDetails.getRole());
            }
        } catch (Exception e) {
            log.error("❌ Failed to save job to Notion storage: {} - {}", jobDetails.getCompany(), jobDetails.getRole(), e);
            throw new RuntimeException("Failed to save job to Notion storage", e);
        }
    }

    public void createHeaders() {
        try {
            notionStorageService.createHeaders();
            log.debug("📋 Notion storage headers initialized");
        } catch (Exception e) {
            log.error("❌ Failed to create Notion storage headers", e);
            throw new RuntimeException("Failed to create Notion storage headers", e);
        }
    }

    public String getStorageInfo() {
        try {
            return notionStorageService.getStorageInfo();
        } catch (Exception e) {
            log.error("❌ Failed to get Notion storage info", e);
            return "Notion Storage (Status: Error - " + e.getMessage() + ")";
        }
    }
}