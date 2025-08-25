package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

@Service
public class CsvStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CsvStorageService.class);

    private final AppProperties properties;
    private static final String CSV_HEADER = "Posted At,Company,Role,Location,Salary,URL,Raw Snippet,Source Channel\n";

    public CsvStorageService(AppProperties properties) {
        this.properties = properties;
    }

    private String getCsvFilePath() {
        return properties.getStorage().getFilePath() + ".csv";
    }

    public void initializeCsvFile() {
        try {
            String filePath = getCsvFilePath();
            if (!Files.exists(Paths.get(filePath))) {
                Files.write(Paths.get(filePath), CSV_HEADER.getBytes());
                log.info("Created CSV file: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to initialize CSV file", e);
        }
    }

    public void appendJob(JobDetails job) {
        try (FileWriter fileWriter = new FileWriter(getCsvFilePath(), true);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            String postedAt = job.getPostedAt() == null ? "" : 
                job.getPostedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            // Escape any commas and quotes in the data
            String csvLine = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                    escapeForCsv(postedAt),
                    escapeForCsv(job.getCompany()),
                    escapeForCsv(job.getRole()),
                    escapeForCsv(job.getLocation()),
                    escapeForCsv(job.getSalary()),
                    escapeForCsv(job.getUrl()),
                    escapeForCsv(job.getRawSnippet()),
                    escapeForCsv(job.getSourceChannel())
            );

            printWriter.print(csvLine);
            log.info("Saved job to CSV: {} - {}", job.getCompany(), job.getRole());

        } catch (IOException e) {
            log.error("Failed to append job to CSV file", e);
        }
    }

    private String escapeForCsv(String value) {
        if (value == null) return "";
        // Replace quotes with double quotes for CSV escaping
        return value.replace("\"", "\"\"");
    }
}
