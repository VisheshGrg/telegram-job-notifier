package com.telegram_notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotionStorageService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NotionStorageService.class);

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private WebClient webClient;

    @Autowired
    public NotionStorageService(AppProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void initializeNotionClient() {
        log.info("Initializing Notion API client...");
        
        this.webClient = WebClient.builder()
            .baseUrl("https://api.notion.com/v1")
            .defaultHeader("Authorization", "Bearer " + properties.getNotion().getIntegrationToken())
            .defaultHeader("Notion-Version", properties.getNotion().getVersion())
            .defaultHeader("Content-Type", "application/json")
            .codecs(clientCodecConfigurer -> 
                clientCodecConfigurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
            
        log.info("Notion API client initialized successfully");
    }

    public void saveJob(JobDetails jobDetails) {
        saveJob(jobDetails, null);
    }
    
    public void saveJob(JobDetails jobDetails, String resumeLink) {
        try {
            log.info("💾 Saving job to Notion: {} - {}", jobDetails.getCompany(), jobDetails.getRole());
            if (resumeLink != null) {
                log.info("🔗 Including resume link: {}", resumeLink);
            }
            
            // Create JSON payload for Notion API
            ObjectNode payload = createNotionPayload(jobDetails, resumeLink);
            
            // Send POST request to Notion API
            String response = webClient
                .post()
                .uri("/pages")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            log.info("✅ Successfully saved job to Notion: {} - {}", 
                    jobDetails.getCompany(), jobDetails.getRole());
            log.debug("Notion API response: {}", response);
            
        } catch (WebClientResponseException e) {
            log.error("❌ Notion API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to save job to Notion: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Error saving job to Notion", e);
            throw new RuntimeException("Failed to save job to Notion: " + e.getMessage(), e);
        }
    }

    private ObjectNode createNotionPayload(JobDetails jobDetails) {
        return createNotionPayload(jobDetails, null);
    }
    
    private ObjectNode createNotionPayload(JobDetails jobDetails, String resumeLink) {
        ObjectNode payload = objectMapper.createObjectNode();
        
        // Set parent database
        ObjectNode parent = objectMapper.createObjectNode();
        parent.put("database_id", properties.getNotion().getDatabaseId());
        payload.set("parent", parent);
        
        // Set properties (database columns)
        ObjectNode properties = objectMapper.createObjectNode();
        
        // Company (Title field)
        if (jobDetails.getCompany() != null && !jobDetails.getCompany().isEmpty()) {
            ObjectNode companyProp = objectMapper.createObjectNode();
            ObjectNode title = objectMapper.createObjectNode();
            ObjectNode titleText = objectMapper.createObjectNode();
            titleText.put("content", jobDetails.getCompany());
            title.set("text", titleText);
            companyProp.set("title", objectMapper.createArrayNode().add(title));
            properties.set("Company", companyProp);
        }
        
        // Role (Rich text)
        if (jobDetails.getRole() != null && !jobDetails.getRole().isEmpty()) {
            properties.set("Role", createRichTextProperty(jobDetails.getRole()));
        }
        
        // Location (Rich text)
        if (jobDetails.getLocation() != null && !jobDetails.getLocation().isEmpty()) {
            properties.set("Location", createRichTextProperty(jobDetails.getLocation()));
        }
        
        // Salary (Rich text)
        if (jobDetails.getSalary() != null && !jobDetails.getSalary().isEmpty()) {
            properties.set("Salary", createRichTextProperty(jobDetails.getSalary()));
        }
        
        // URL (URL field)
        if (jobDetails.getUrl() != null && !jobDetails.getUrl().isEmpty()) {
            ObjectNode urlProp = objectMapper.createObjectNode();
            urlProp.put("url", jobDetails.getUrl());
            properties.set("URL", urlProp);
        }
        
        // Source (Rich text)
        if (jobDetails.getSourceChannel() != null && !jobDetails.getSourceChannel().isEmpty()) {
            properties.set("Source", createRichTextProperty(jobDetails.getSourceChannel()));
        }
        
        // Posted Date (Date field)
        if (jobDetails.getPostedAt() != null) {
            ObjectNode dateProp = objectMapper.createObjectNode();
            ObjectNode dateValue = objectMapper.createObjectNode();
            dateValue.put("start", jobDetails.getPostedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
            dateProp.set("date", dateValue);
            properties.set("Posted Date", dateProp);
        }
        
        // Raw Snippet (Rich text)
        if (jobDetails.getRawSnippet() != null && !jobDetails.getRawSnippet().isEmpty()) {
            // Truncate to avoid Notion's limits
            String snippet = jobDetails.getRawSnippet();
            if (snippet.length() > 2000) {
                snippet = snippet.substring(0, 2000) + "...";
            }
            properties.set("Raw Snippet", createRichTextProperty(snippet));
        }
        
        // Resume Link (URL field)
        if (resumeLink != null && !resumeLink.isEmpty()) {
            ObjectNode resumeProp = objectMapper.createObjectNode();
            resumeProp.put("url", resumeLink);
            properties.set("Resume Link", resumeProp);
        }
        
        payload.set("properties", properties);
        
        return payload;
    }

    private ObjectNode createRichTextProperty(String text) {
        ObjectNode richTextProp = objectMapper.createObjectNode();
        ObjectNode richTextItem = objectMapper.createObjectNode();
        ObjectNode textContent = objectMapper.createObjectNode();
        textContent.put("content", text);
        richTextItem.set("text", textContent);
        richTextProp.set("rich_text", objectMapper.createArrayNode().add(richTextItem));
        return richTextProp;
    }

    public void createHeaders() {
        log.info("📋 Notion database should already have the required columns:");
        log.info("   - Company (Title)");
        log.info("   - Role (Text)");
        log.info("   - Location (Text)");
        log.info("   - Salary (Text)");
        log.info("   - URL (URL)");
        log.info("   - Source (Text)");
        log.info("   - Posted Date (Date)");
        log.info("   - Raw Snippet (Text)");
        log.info("   - Resume Link (URL) - NEW for resume generation");
        log.info("✅ If columns are missing, add them to your Notion database");
    }

    public String getStorageInfo() {
        return String.format("Notion Database: %s (Integration: %s...)", 
                           properties.getNotion().getDatabaseId(),
                           properties.getNotion().getIntegrationToken().substring(0, 
                               Math.min(15, properties.getNotion().getIntegrationToken().length())));
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("🧪 Testing Notion API connection...");
            
            // Test by querying the database
            String response = webClient
                .post()
                .uri("/databases/{database_id}/query", properties.getNotion().getDatabaseId())
                .bodyValue(objectMapper.createObjectNode())
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            result.put("status", "success");
            result.put("message", "Notion API connection successful");
            result.put("database_id", properties.getNotion().getDatabaseId());
            
            log.info("✅ Notion API connection test successful");
            
        } catch (WebClientResponseException e) {
            String errorMsg = String.format("Notion API error (%s): %s", 
                                           e.getStatusCode(), e.getResponseBodyAsString());
            result.put("status", "error");
            result.put("message", errorMsg);
            
            log.error("❌ Notion API connection test failed: {}", errorMsg);
            
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Connection test failed: " + e.getMessage());
            
            log.error("❌ Notion API connection test failed", e);
        }
        
        return result;
    }

    @Override
    public String toString() {
        return getStorageInfo();
    }
}
