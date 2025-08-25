package com.telegram_notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.telegram_notifier.model.JobDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class JsonStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonStorageService.class);

    private static final String JSON_FILE = "job_listings.json";
    private final ObjectMapper objectMapper;

    public JsonStorageService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void appendJob(JobDetails job) {
        try {
            List<JobDetails> jobs = loadExistingJobs();
            jobs.add(job);
            
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(JSON_FILE), jobs);
            
            log.info("Saved job to JSON: {} - {}", job.getCompany(), job.getRole());
            
        } catch (IOException e) {
            log.error("Failed to append job to JSON file", e);
        }
    }

    private List<JobDetails> loadExistingJobs() {
        try {
            File file = new File(JSON_FILE);
            if (file.exists()) {
                return objectMapper.readValue(file, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, JobDetails.class));
            }
        } catch (IOException e) {
            log.warn("Could not load existing jobs from JSON file, starting fresh", e);
        }
        return new ArrayList<>();
    }

    public List<JobDetails> getAllJobs() {
        return loadExistingJobs();
    }
}
