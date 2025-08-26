package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class ResumeGenerationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ResumeGenerationService.class);
    
    private final AppProperties properties;
    private final LatexCompilationService latexCompilationService;
    private final CloudinaryService cloudinaryService;
    private final WebClient webClient;
    
    private String resumeTemplate;
    
    public ResumeGenerationService(AppProperties properties, 
                                 LatexCompilationService latexCompilationService,
                                 CloudinaryService cloudinaryService) {
        this.properties = properties;
        this.latexCompilationService = latexCompilationService;
        this.cloudinaryService = cloudinaryService;
        this.webClient = WebClient.builder().build();
    }
    
    @PostConstruct
    public void loadResumeTemplate() {
        try {
            Resource resource = new ClassPathResource(properties.getResume().getTemplatePath());
            this.resumeTemplate = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("✅ Resume template loaded successfully from {}", properties.getResume().getTemplatePath());
        } catch (IOException e) {
            log.error("❌ Failed to load resume template from {}", properties.getResume().getTemplatePath(), e);
            throw new RuntimeException("Failed to load resume template", e);
        }
    }
    
    /**
     * Generate a customized resume for a job application and return the public URL
     * @param jobDetails Job details to customize resume for
     * @return Public URL of the generated resume PDF, or null if generation fails
     */
    public String generateCustomizedResume(JobDetails jobDetails) {
        if (!properties.getResume().isGenerateEnabled()) {
            log.info("⏭️ Resume generation disabled in configuration");
            return null;
        }
        
        if (jobDetails == null) {
            log.warn("❌ Cannot generate resume - job details are null");
            return null;
        }
        
        log.info("🎯 Starting resume generation for {} at {}", jobDetails.getRole(), jobDetails.getCompany());
        
        try {
            // Add extra delay before resume generation to avoid rate limits
            // (This is the 3rd consecutive Gemini API call after relevance + extraction)
            int extraDelay = properties.getAi().getGemini().getRateLimitDelaySeconds();
            log.info("Adding {} second delay before resume generation to avoid rate limits", extraDelay);
            Thread.sleep(extraDelay * 1000);
            
            // Step 1: Customize LaTeX template using AI
            String customizedLatex = customizeResumeWithAI(jobDetails);
            
            if (customizedLatex == null || customizedLatex.trim().isEmpty()) {
                log.error("❌ AI customization failed - empty result");
                return null;
            }
            
            // Step 2: Validate customized LaTeX
            if (!latexCompilationService.validateLatexContent(customizedLatex)) {
                log.error("❌ Generated LaTeX content failed validation");
                return null;
            }
            
            // Step 3: Compile LaTeX to PDF
            byte[] pdfBytes = latexCompilationService.compileLatexToPdf(customizedLatex);
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                log.error("❌ LaTeX compilation returned empty PDF");
                return null;
            }
            
            // Step 4: Upload PDF to Cloudinary and get public URL
            String resumeUrl = cloudinaryService.uploadResumePdf(pdfBytes, jobDetails);
            
            if (resumeUrl == null || resumeUrl.trim().isEmpty()) {
                log.error("❌ Cloudinary upload failed - no URL returned");
                return null;
            }
            
            log.info("🎉 Resume generation completed successfully!");
            log.info("🔗 Resume URL: {}", resumeUrl);
            
            return resumeUrl;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ Resume generation interrupted during rate limit delay", e);
            return null;
        } catch (Exception e) {
            log.error("❌ Resume generation failed for {} at {}: {}", jobDetails.getRole(), jobDetails.getCompany(), e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }
    
    /**
     * Use AI (Gemini) to customize the resume template based on job details
     * @param jobDetails Job details to customize for
     * @return Customized LaTeX content
     */
    private String customizeResumeWithAI(JobDetails jobDetails) {
        log.info("🤖 Customizing resume template using AI for {} at {}", jobDetails.getRole(), jobDetails.getCompany());
        
        try {
            // Note: We already added delay in generateCustomizedResume() method
            // This is just internal rate limiting within resume generation
            // Thread.sleep(properties.getAi().getGemini().getRateLimitDelaySeconds() * 1000);
            
            // Build AI prompt for resume customization
            String prompt = buildResumeCustomizationPrompt(jobDetails);
            
            // Call Gemini API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", new Object[]{
                Map.of("parts", new Object[]{
                    Map.of("text", prompt)
                })
            });
            
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + 
                           properties.getAi().getGemini().getModel() + 
                           ":generateContent?key=" + properties.getAi().getGemini().getApiKey();
            
            Map<String, Object> response = webClient.post()
                .uri(apiUrl)
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            // Log the full response for debugging
            log.debug("Gemini API response: {}", response);
            
            // Extract customized LaTeX from response
            String customizedLatex = extractLatexFromResponse(response);
            
            if (customizedLatex != null && !customizedLatex.trim().isEmpty()) {
                log.info("✅ AI resume customization successful");
                return customizedLatex;
            } else {
                log.error("❌ AI returned empty customization result");
                log.error("Full API response: {}", response);
                return null;
            }
            
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.error("❌ Gemini API rate limit exceeded - Status: 429. Consider increasing delay time.");
                log.error("Current rate limit delay: {}s", properties.getAi().getGemini().getRateLimitDelaySeconds());
            } else {
                log.error("❌ Gemini API error - Status: {} - Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
            }
            return null;
        } catch (Exception e) {
            log.error("❌ AI resume customization failed", e);
            return null;
        }
    }
    
    /**
     * Build the AI prompt for resume customization
     * @param jobDetails Job details to customize for
     * @return AI prompt string
     */
    private String buildResumeCustomizationPrompt(JobDetails jobDetails) {
        return String.format("""
            You are a professional resume writer. I need you to customize this LaTeX resume template for a specific job application.
            
            JOB DETAILS:
            - Company: %s
            - Role: %s
            - Location: %s
            - Salary: %s
            - Description: %s
            
            RESUME TEMPLATE TO CUSTOMIZE:
            %s
            
            INSTRUCTIONS:
            1. Change only the necessary parts according to the role.
            2. Dont add so many extra lines so that it becomes too lengthy, it should remain on single page.
            8. Make the resume ATS-friendly and professional
            
            Return ONLY the complete, customized LaTeX document. Do not include any explanations or additional text outside the LaTeX document.
            """, 
            jobDetails.getCompany(), 
            jobDetails.getRole(),
            jobDetails.getLocation() != null ? jobDetails.getLocation() : "Remote",
            jobDetails.getSalary() != null ? jobDetails.getSalary() : "Competitive",
            jobDetails.getRawSnippet() != null ? jobDetails.getRawSnippet() : "Software development position",
            resumeTemplate
        );
    }
    
    /**
     * Extract LaTeX content from AI API response
     * @param response API response map
     * @return Extracted LaTeX content
     */
    @SuppressWarnings("unchecked")
    private String extractLatexFromResponse(Map<String, Object> response) {
        try {
            log.debug("Parsing response: {}", response);
            
            if (response == null) {
                log.error("Response is null");
                return null;
            }
            
            Object candidates = response.get("candidates");
            if (candidates == null) {
                log.error("No 'candidates' field in response");
                return null;
            }
            
            // Handle both List and Object[] cases for candidates
            Map<String, Object> candidate = null;
            if (candidates instanceof java.util.List) {
                java.util.List<Object> candidatesList = (java.util.List<Object>) candidates;
                if (!candidatesList.isEmpty()) {
                    candidate = (Map<String, Object>) candidatesList.get(0);
                }
            } else if (candidates instanceof Object[] && ((Object[]) candidates).length > 0) {
                candidate = (Map<String, Object>) ((Object[]) candidates)[0];
            }
            
            if (candidate == null) {
                log.error("No candidates found or candidates list is empty");
                return null;
            }
            
            Map<String, Object> content = (Map<String, Object>) candidate.get("content");
            if (content == null) {
                log.error("No 'content' field in candidate");
                return null;
            }
            
            Object parts = content.get("parts");
            if (parts == null) {
                log.error("No 'parts' field in content");
                return null;
            }
            
            // Handle both List and Object[] cases for parts
            Map<String, Object> part = null;
            if (parts instanceof java.util.List) {
                java.util.List<Object> partsList = (java.util.List<Object>) parts;
                if (!partsList.isEmpty()) {
                    part = (Map<String, Object>) partsList.get(0);
                }
            } else if (parts instanceof Object[] && ((Object[]) parts).length > 0) {
                part = (Map<String, Object>) ((Object[]) parts)[0];
            }
            
            if (part == null) {
                log.error("No parts found or parts list is empty");
                return null;
            }
            
            String text = (String) part.get("text");
            log.debug("Raw text length: {}", text != null ? text.length() : 0);
            
            if (text != null) {
                // Remove markdown code block formatting if present
                text = text.trim();
                if (text.startsWith("```latex")) {
                    text = text.substring(8); // Remove "```latex"
                } else if (text.startsWith("```")) {
                    text = text.substring(3); // Remove "```"
                }
                
                if (text.endsWith("```")) {
                    text = text.substring(0, text.length() - 3); // Remove trailing "```"
                }
                
                text = text.trim();
                log.debug("Cleaned LaTeX content length: {}", text.length());
                log.debug("LaTeX starts with: {}", text.length() > 50 ? text.substring(0, 50) + "..." : text);
                
                return text;
            } else {
                log.error("No 'text' field found in part");
                return null;
            }
            
        } catch (Exception e) {
            log.error("❌ Error extracting LaTeX from AI response", e);
            log.error("Response structure: {}", response);
            return null;
        }
    }
    
    /**
     * Get service status for health checks
     * @return Status information map
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Resume Generation");
        status.put("template_loaded", resumeTemplate != null && !resumeTemplate.isEmpty());
        status.put("generation_enabled", properties.getResume().isGenerateEnabled());
        status.put("template_path", properties.getResume().getTemplatePath());
        status.put("latex_online_available", latexCompilationService.isLatexOnlineAvailable());
        status.put("cloudinary_available", cloudinaryService.testConnection());
        
        return status;
    }
}
