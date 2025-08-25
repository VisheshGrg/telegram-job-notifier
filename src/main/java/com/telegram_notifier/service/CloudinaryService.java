package com.telegram_notifier.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

@Service
public class CloudinaryService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CloudinaryService.class);
    
    private final AppProperties properties;
    private Cloudinary cloudinary;
    
    public CloudinaryService(AppProperties properties) {
        this.properties = properties;
    }
    
    @PostConstruct
    public void initializeCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", properties.getCloudinary().getCloudName());
            config.put("api_key", properties.getCloudinary().getApiKey());
            config.put("api_secret", properties.getCloudinary().getApiSecret());
            
            this.cloudinary = new Cloudinary(config);
            log.info("✅ Cloudinary service initialized successfully");
            
            // Test connection
            testConnection();
            
        } catch (Exception e) {
            log.error("❌ Failed to initialize Cloudinary service", e);
            throw new RuntimeException("Cloudinary initialization failed", e);
        }
    }
    
    /**
     * Upload PDF resume to Cloudinary and return public URL
     * @param pdfBytes PDF file as byte array
     * @param jobDetails Job details for metadata and filename
     * @return Public Cloudinary URL for the uploaded PDF
     * @throws RuntimeException if upload fails
     */
    public String uploadResumePdf(byte[] pdfBytes, JobDetails jobDetails) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF bytes cannot be null or empty");
        }
        
        if (jobDetails == null) {
            throw new IllegalArgumentException("Job details cannot be null");
        }
        
        log.info("📤 Uploading resume PDF to Cloudinary for {} at {}", jobDetails.getRole(), jobDetails.getCompany());
        
        try {
            // Generate unique filename based on job details
            String fileName = generateFileName(jobDetails);
            
            // Prepare upload options
            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                "public_id", fileName,
                "resource_type", "raw", // Important: use 'raw' for PDF files
                "format", "pdf",
                "context", ObjectUtils.asMap(
                    "company", jobDetails.getCompany(),
                    "role", jobDetails.getRole(),
                    "source_channel", jobDetails.getSourceChannel(),
                    "generated_at", String.valueOf(System.currentTimeMillis())
                ),
                "tags", new String[]{"resume", "job-application", cleanString(jobDetails.getCompany())}
            );
            
            // Upload PDF to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(pdfBytes, uploadOptions);
            
            // Extract the secure URL from the result
            String secureUrl = (String) uploadResult.get("secure_url");
            
            if (secureUrl == null || secureUrl.isEmpty()) {
                throw new RuntimeException("Cloudinary upload succeeded but returned no URL");
            }
            
            log.info("✅ Resume PDF uploaded successfully to Cloudinary");
            log.info("🔗 Resume URL: {}", secureUrl);
            log.info("📊 Upload details - Size: {} bytes, Public ID: {}", pdfBytes.length, fileName);
            
            return secureUrl;
            
        } catch (IOException e) {
            log.error("❌ Cloudinary upload failed due to IO error", e);
            throw new RuntimeException("Failed to upload PDF to Cloudinary: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error during Cloudinary upload", e);
            throw new RuntimeException("Failed to upload PDF to Cloudinary: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate a unique filename for the resume PDF
     * @param jobDetails Job details to use for filename
     * @return Unique filename string
     */
    private String generateFileName(JobDetails jobDetails) {
        String company = cleanString(jobDetails.getCompany());
        String role = cleanString(jobDetails.getRole());
        long timestamp = System.currentTimeMillis();
        
        return String.format("resumes/%s_%s_%d", company, role, timestamp);
    }
    
    /**
     * Clean string for use in filenames (remove special characters)
     * @param input Input string to clean
     * @return Cleaned string suitable for filenames
     */
    private String cleanString(String input) {
        if (input == null) return "unknown";
        
        return input
            .toLowerCase()
            .replaceAll("[^a-zA-Z0-9]", "_") // Replace non-alphanumeric with underscore
            .replaceAll("_{2,}", "_") // Replace multiple underscores with single
            .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
    }
    
    /**
     * Delete resume from Cloudinary (cleanup method)
     * @param publicId The public ID of the file to delete
     * @return true if deletion was successful
     */
    public boolean deleteResume(String publicId) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, 
                ObjectUtils.asMap("resource_type", "raw"));
            
            String resultStatus = (String) result.get("result");
            boolean success = "ok".equals(resultStatus);
            
            if (success) {
                log.info("✅ Resume deleted from Cloudinary: {}", publicId);
            } else {
                log.warn("⚠️ Failed to delete resume from Cloudinary: {} (status: {})", publicId, resultStatus);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ Error deleting resume from Cloudinary: {}", publicId, e);
            return false;
        }
    }
    
    /**
     * Test Cloudinary connection and configuration
     * @return true if connection is working
     */
    public boolean testConnection() {
        try {
            // Test with a minimal API call
            Map<String, Object> result = cloudinary.api().ping(ObjectUtils.emptyMap());
            
            if (result != null && result.containsKey("status") && "ok".equals(result.get("status"))) {
                log.info("✅ Cloudinary connection test successful");
                return true;
            } else {
                log.warn("⚠️ Cloudinary connection test returned unexpected result: {}", result);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Cloudinary connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get service status for health checks
     * @return Status information map
     */
    public Map<String, Object> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Cloudinary File Upload");
        status.put("cloud_name", properties.getCloudinary().getCloudName());
        status.put("api_key_configured", !properties.getCloudinary().getApiKey().isEmpty());
        status.put("api_secret_configured", !properties.getCloudinary().getApiSecret().isEmpty());
        status.put("connection_test", testConnection());
        
        return status;
    }
}
