package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import com.telegram_notifier.model.TelegramMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class JobNotificationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JobNotificationService.class);

    private final TelegramService telegramService;
    private final AiService aiService;
    private final ExtractionService extractionService;
    private final StorageService storageService;
    private final ResumeGenerationService resumeGenerationService;
    private final AppProperties properties;

    // Track processing statistics
    private LocalDateTime lastProcessingTime;
    private int totalProcessedToday = 0;
    private int totalSavedToday = 0;

    public JobNotificationService(TelegramService telegramService, 
                                 AiService aiService, 
                                 ExtractionService extractionService, 
                                 StorageService storageService,
                                 ResumeGenerationService resumeGenerationService,
                                 AppProperties properties) {
        this.telegramService = telegramService;
        this.aiService = aiService;
        this.extractionService = extractionService;
        this.storageService = storageService;
        this.resumeGenerationService = resumeGenerationService;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${app.telegram.poll-interval-minutes:30}", timeUnit = TimeUnit.MINUTES)
    public void processMessages() {
        log.info("🔄 Starting scheduled message processing at {}...", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
        try {
            // Fetch only NEW messages using timestamp filtering
            List<TelegramMessage> newMessages = telegramService.fetchRecentMessages();
            log.info("📥 Fetched {} NEW messages from Telegram channels", newMessages.size());

            int processedCount = 0;
            int relevantCount = 0;
            int savedCount = 0;

            for (TelegramMessage telegramMessage : newMessages) {
                try {
                    processedCount++;
                    
                    log.debug("🔍 Processing message from @{} at {}: {}", 
                            telegramMessage.getChannelName(),
                            telegramMessage.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm")),
                            telegramMessage.getContent().substring(0, Math.min(50, telegramMessage.getContent().length())) + "...");
                    
                    // Add delay before AI call to respect rate limits
                    if (processedCount > 1) {
                        int delaySeconds = properties.getAi().getGemini().getRateLimitDelaySeconds();
                        log.debug("⏳ Waiting {} seconds before next Gemini AI call to avoid rate limits...", delaySeconds);
                        Thread.sleep(delaySeconds * 1000);
                    }
                    
                    // Check if message is job-relevant using AI
                    boolean isRelevant = aiService.isRelevant(telegramMessage.getContent());
                    
                    if (isRelevant) {
                        relevantCount++;
                        log.info("✅ Found relevant job post from @{}, extracting details...", 
                                telegramMessage.getChannelName());
                        
                        // Another delay before second AI call (detail extraction)
                        int delaySeconds = properties.getAi().getGemini().getRateLimitDelaySeconds();
                        log.debug("⏳ Waiting {} seconds before detail extraction AI call...", delaySeconds);
                        Thread.sleep(delaySeconds * 1000);
                        
                        // Extract job details using AI
                        JobDetails jobDetails = extractionService.extract(telegramMessage.getContent(), 
                                "telegram_channel_" + telegramMessage.getChannelName());
                        
                        if (jobDetails != null) {
                            // Add timestamp information to job details
                            jobDetails.setPostedDate(telegramMessage.getTimestamp().toLocalDate().toString());
                            
                            // Generate resume for this job application
                            String resumeLink = null;
                            try {
                                log.info("📝 Generating custom resume for {} at {}...", jobDetails.getRole(), jobDetails.getCompany());
                                resumeLink = resumeGenerationService.generateCustomizedResume(jobDetails);
                                if (resumeLink != null) {
                                    log.info("✅ Resume generated successfully: {}", resumeLink);
                                } else {
                                    log.warn("⚠️ Resume generation failed or disabled for this job");
                                }
                            } catch (Exception e) {
                                log.error("❌ Resume generation failed for {} at {}: {}", 
                                        jobDetails.getRole(), jobDetails.getCompany(), e.getMessage());
                                // Continue without resume - don't fail the entire job saving process
                            }
                            
                            storageService.saveJob(jobDetails, resumeLink);
                            savedCount++;
                            
                            log.info("💾 Saved job: {} - {} (from @{})", 
                                    jobDetails.getCompany(), 
                                    jobDetails.getRole(),
                                    telegramMessage.getChannelName());
                            if (resumeLink != null) {
                                log.info("🔗 Resume link saved: {}", resumeLink);
                            }
                        } else {
                            log.warn("⚠️ Failed to extract job details from relevant message from @{}", 
                                    telegramMessage.getChannelName());
                        }
                    } else {
                        log.debug("❌ Message from @{} not job-relevant", telegramMessage.getChannelName());
                    }

                } catch (InterruptedException e) {
                    log.warn("⚠️ Processing interrupted during delay");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("🚨 Error processing individual message from @{}: {}", 
                            telegramMessage.getChannelName(), e.getMessage());
                }
            }

            // Update statistics
            lastProcessingTime = LocalDateTime.now();
            totalProcessedToday += processedCount;
            totalSavedToday += savedCount;

            log.info("✅ Message processing completed at {}. Processed: {}, Relevant: {}, Saved: {}", 
                    lastProcessingTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    processedCount, relevantCount, savedCount);

            if (savedCount > 0) {
                log.info("🎯 Daily totals: {} processed, {} saved", totalProcessedToday, totalSavedToday);
            }

        } catch (Exception e) {
            log.error("🚨 Error during scheduled message processing", e);
        }
    }

    /**
     * Process messages manually (called via API endpoint)
     */
    public Map<String, Object> processMessagesManually() {
        log.info("🚀 Manual message processing triggered");
        
        try {
            List<TelegramMessage> newMessages = telegramService.fetchRecentMessages();
            
            int processedCount = 0;
            int relevantCount = 0;
            int savedCount = 0;

            for (TelegramMessage telegramMessage : newMessages) {
                try {
                    processedCount++;
                    
                    // Add delay before AI call to respect rate limits (except for first message)
                    if (processedCount > 1) {
                        int delaySeconds = properties.getAi().getGemini().getRateLimitDelaySeconds();
                        log.debug("⏳ Manual processing - waiting {} seconds before next Gemini AI call...", delaySeconds);
                        Thread.sleep(delaySeconds * 1000);
                    }
                    
                    boolean isRelevant = aiService.isRelevant(telegramMessage.getContent());
                    
                    if (isRelevant) {
                        relevantCount++;
                        
                        // Delay before detail extraction
                        int delaySeconds = properties.getAi().getGemini().getRateLimitDelaySeconds();
                        log.debug("⏳ Manual processing - waiting {} seconds before detail extraction...", delaySeconds);
                        Thread.sleep(delaySeconds * 1000);
                        
                        JobDetails jobDetails = extractionService.extract(telegramMessage.getContent(), 
                                "manual_telegram_" + telegramMessage.getChannelName());
                        
                        if (jobDetails != null) {
                            jobDetails.setPostedDate(telegramMessage.getTimestamp().toLocalDate().toString());
                            
                            // Generate resume for this job application
                            String resumeLink = null;
                            try {
                                log.info("📝 Generating custom resume for {} at {}...", jobDetails.getRole(), jobDetails.getCompany());
                                resumeLink = resumeGenerationService.generateCustomizedResume(jobDetails);
                                if (resumeLink != null) {
                                    log.info("✅ Resume generated: {}", resumeLink);
                                }
                            } catch (Exception e) {
                                log.error("❌ Resume generation failed: {}", e.getMessage());
                            }
                            
                            storageService.saveJob(jobDetails, resumeLink);
                            savedCount++;
                        }
                    }
                    
                } catch (InterruptedException e) {
                    log.warn("⚠️ Manual processing interrupted during delay");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("🚨 Error in manual processing for message from @{}", 
                            telegramMessage.getChannelName(), e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("message", "Messages processed successfully");
            result.put("processed_count", processedCount);
            result.put("relevant_count", relevantCount);
            result.put("saved_count", savedCount);
            result.put("new_messages_found", newMessages.size());
            result.put("processing_time", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info("🎯 Manual processing completed: {} processed, {} relevant, {} saved", 
                    processedCount, relevantCount, savedCount);
            
            return result;

        } catch (Exception e) {
            log.error("🚨 Error during manual message processing", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Error processing messages: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Process a single message manually
     */
    public Map<String, Object> processSingleMessage(String messageContent) {
        log.info("🔍 Processing single message manually");
        
        try {
            boolean isRelevant = aiService.isRelevant(messageContent);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message_content", messageContent.substring(0, Math.min(100, messageContent.length())) + "...");
            result.put("is_relevant", isRelevant);
            
            if (isRelevant) {
                // Add delay before second AI call (detail extraction)
                int delaySeconds = properties.getAi().getGemini().getRateLimitDelaySeconds();
                log.debug("⏳ Single message processing - waiting {} seconds before detail extraction...", delaySeconds);
                Thread.sleep(delaySeconds * 1000);
                
                JobDetails jobDetails = extractionService.extract(messageContent, "manual_input");
                
                if (jobDetails != null) {
                    jobDetails.setPostedDate(LocalDateTime.now().toLocalDate().toString());
                    
                    // Generate resume for this job application
                    String resumeLink = null;
                    try {
                        log.info("📝 Generating custom resume for {} at {}...", jobDetails.getRole(), jobDetails.getCompany());
                        resumeLink = resumeGenerationService.generateCustomizedResume(jobDetails);
                        if (resumeLink != null) {
                            log.info("✅ Resume generated: {}", resumeLink);
                        }
                    } catch (Exception e) {
                        log.error("❌ Resume generation failed: {}", e.getMessage());
                    }
                    
                    storageService.saveJob(jobDetails, resumeLink);
                    
                    result.put("status", "success");
                    result.put("message", "Job details extracted and saved");
                    result.put("job_details", jobDetails);
                    if (resumeLink != null) {
                        result.put("resume_link", resumeLink);
                    }
                    
                    log.info("💾 Manual message processed and saved: {} - {}", 
                            jobDetails.getCompany(), jobDetails.getRole());
                } else {
                    result.put("status", "warning");
                    result.put("message", "Message is relevant but failed to extract job details");
                    
                    log.warn("⚠️ Relevant message but extraction failed");
                }
            } else {
                result.put("status", "info");
                result.put("message", "Message is not job-relevant");
                
                log.info("❌ Manual message not job-relevant");
            }
            
            return result;

        } catch (InterruptedException e) {
            log.warn("⚠️ Single message processing interrupted during delay");
            Thread.currentThread().interrupt();
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Processing interrupted");
            return errorResult;

        } catch (Exception e) {
            log.error("🚨 Error processing single message", e);
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "error");
            errorResult.put("message", "Error processing message: " + e.getMessage());
            return errorResult;
        }
    }

    public void initializeStorage() {
        try {
            log.info("🚀 Initializing storage service...");
            storageService.init();
            storageService.createHeaders();
            log.info("✅ Storage service initialized successfully");
        } catch (Exception e) {
            log.error("🚨 Failed to initialize storage service", e);
        }
    }

    public Map<String, Object> getProcessingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service_name", "Telegram Job Notifier with Timestamp Filtering");
        status.put("last_processing_time", lastProcessingTime != null ? 
                  lastProcessingTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "Never");
        status.put("total_processed_today", totalProcessedToday);
        status.put("total_saved_today", totalSavedToday);
        status.put("telegram_service", telegramService.getServiceStatus());
        return status;
    }

    public void resetDailyCounters() {
        totalProcessedToday = 0;
        totalSavedToday = 0;
        log.info("🔄 Reset daily processing counters");
    }

    public void resetChannelTimestamps() {
        telegramService.resetChannelTimestamps();
        log.info("🔄 Reset all channel timestamps - next run will process recent messages as new");
    }
}