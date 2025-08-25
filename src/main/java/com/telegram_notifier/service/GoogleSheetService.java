package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "google-sheets")
public class GoogleSheetService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GoogleSheetService.class);

    private final AppProperties props;

    public GoogleSheetService(AppProperties props) {
        this.props = props;
    }

    public void append(JobDetails job) {
        log.warn("Google Sheets integration not available. Google Sheets dependencies are commented out in pom.xml.");
        log.warn("To use Google Sheets:");
        log.warn("1. Uncomment Google Sheets dependencies in pom.xml");
        log.warn("2. Set up Google Sheets API credentials");
        log.warn("3. Restart the application");
        log.warn("For now, falling back to CSV storage...");
        
        // Create a simple CSV fallback
        CsvStorageService csvService = new CsvStorageService(props);
        csvService.initializeCsvFile();
        csvService.appendJob(job);
    }

    public void createHeader() {
        log.info("Google Sheets not available, using CSV fallback for headers");
        CsvStorageService csvService = new CsvStorageService(props);
        csvService.initializeCsvFile();
    }
}