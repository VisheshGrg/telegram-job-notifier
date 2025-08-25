package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class StorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StorageService.class);

    private final AppProperties properties;
    private final CsvStorageService csvStorageService;
    private final JsonStorageService jsonStorageService;
    private final SqliteStorageService sqliteStorageService;
    private final NotionStorageService notionStorageService;
    
    @Autowired(required = false)
    private GoogleSheetService googleSheetService;

        public StorageService(AppProperties properties, 
                         CsvStorageService csvStorageService,
                         JsonStorageService jsonStorageService,
                         SqliteStorageService sqliteStorageService,
                         NotionStorageService notionStorageService) {
        this.properties = properties;
        this.csvStorageService = csvStorageService;
        this.jsonStorageService = jsonStorageService;
        this.sqliteStorageService = sqliteStorageService;
        this.notionStorageService = notionStorageService;
    }

    @PostConstruct
    public void init() {
        String storageType = properties.getStorage().getType();
        log.info("Initializing storage service with type: {}", storageType);
        
        switch (storageType.toLowerCase()) {
            case "csv":
                csvStorageService.initializeCsvFile();
                break;
            case "sqlite":
                sqliteStorageService.initializeDatabase();
                break;
            case "notion":
                // Notion service initializes itself via @PostConstruct
                log.info("Notion service is available and ready");
                break;
            case "google-sheets":
                // GoogleSheetService initializes itself
                break;
            case "json":
                // JSON storage doesn't need initialization
                break;
            default:
                log.warn("Unknown storage type: {}, defaulting to CSV", storageType);
                csvStorageService.initializeCsvFile();
        }
    }

    public void saveJob(JobDetails jobDetails) {
        saveJob(jobDetails, null);
    }
    
    public void saveJob(JobDetails jobDetails, String resumeLink) {
        String storageType = properties.getStorage().getType().toLowerCase();
        
        try {
            switch (storageType) {
                case "csv":
                    csvStorageService.appendJob(jobDetails);
                    break;
                case "json":
                    jsonStorageService.appendJob(jobDetails);
                    break;
                case "sqlite":
                    sqliteStorageService.appendJob(jobDetails);
                    break;
                            case "notion":
                if (resumeLink != null) {
                    notionStorageService.saveJob(jobDetails, resumeLink);
                } else {
                    notionStorageService.saveJob(jobDetails);
                }
                break;
                case "google-sheets":
                    if (googleSheetService != null) {
                        googleSheetService.append(jobDetails);
                    } else {
                        log.warn("Google Sheets service not available, falling back to CSV");
                        csvStorageService.appendJob(jobDetails);
                    }
                    break;
                default:
                    log.warn("Unknown storage type: {}, using CSV as fallback", storageType);
                    csvStorageService.appendJob(jobDetails);
            }
        } catch (Exception e) {
            log.error("Failed to save job with {} storage, falling back to CSV", storageType, e);
            try {
                csvStorageService.appendJob(jobDetails);
            } catch (Exception fallbackError) {
                log.error("Even CSV fallback failed!", fallbackError);
            }
        }
    }

    public void createHeaders() {
        String storageType = properties.getStorage().getType().toLowerCase();
        
        switch (storageType) {
            case "notion":
                notionStorageService.createHeaders();
                break;
            case "google-sheets":
                if (googleSheetService != null) {
                    try {
                        googleSheetService.createHeader();
                    } catch (Exception e) {
                        log.error("Failed to create Google Sheets headers", e);
                    }
                } else {
                    log.warn("Google Sheets service not available, using CSV instead");
                    csvStorageService.initializeCsvFile();
                }
                break;
            case "csv":
                csvStorageService.initializeCsvFile();
                break;
            default:
                // JSON and SQLite don't need explicit header creation
                break;
        }
    }

    public String getStorageInfo() {
        String storageType = properties.getStorage().getType().toLowerCase();
        
        switch (storageType) {
            case "csv":
                return "CSV file: job_listings.csv";
            case "json":
                return "JSON file: job_listings.json";
            case "sqlite":
                return "SQLite database: jobs.db (Jobs stored: " + sqliteStorageService.getJobCount() + ")";
            case "notion":
                return notionStorageService.getStorageInfo();
            case "google-sheets":
                return "Google Sheets: " + properties.getSheets().getSpreadsheetId();
            default:
                return "Unknown storage type: " + storageType;
        }
    }
}