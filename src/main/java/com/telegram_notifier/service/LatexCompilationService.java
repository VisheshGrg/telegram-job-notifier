package com.telegram_notifier.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class LatexCompilationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LatexCompilationService.class);
    
    private final WebClient webClient;
    
    // LaTeX compilation service endpoint
    private static final String LATEX_SERVICE_URL = "https://latex.ytotech.com/builds/sync";
    
    public LatexCompilationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB for PDF files
            .build();
    }
    
    /**
     * Compile LaTeX content to PDF using LaTeX Online service with fallback options
     * @param latexContent The complete LaTeX document content
     * @return PDF file as byte array
     * @throws RuntimeException if compilation fails on all services
     */
    public byte[] compileLatexToPdf(String latexContent) {
        if (latexContent == null || latexContent.trim().isEmpty()) {
            throw new IllegalArgumentException("LaTeX content cannot be null or empty");
        }
        
        log.info("🔨 Compiling LaTeX document using YToTech service...");
        log.debug("LaTeX content length: {} characters", latexContent.length());
        
        try {
            byte[] pdfBytes = compileWithYToTechService(latexContent);
            
            if (pdfBytes != null && pdfBytes.length > 0) {
                log.info("✅ LaTeX compilation successful. PDF size: {} bytes", pdfBytes.length);
                return pdfBytes;
            } else {
                throw new RuntimeException("LaTeX compilation returned empty or null PDF");
            }
            
        } catch (Exception e) {
            log.error("❌ LaTeX compilation failed: {}", e.getMessage());
            throw new RuntimeException("LaTeX compilation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Compile LaTeX content using YToTech service
     * @param latexContent LaTeX content to compile
     * @return PDF bytes
     * @throws Exception if compilation fails
     */
    private byte[] compileWithYToTechService(String latexContent) throws Exception {
        try {
            // YToTech structured API format
            Map<String, Object> resource = Map.of(
                "main", true,
                "content", latexContent
            );
            Map<String, Object> jsonBody = Map.of(
                "compiler", "pdflatex",
                "resources", java.util.List.of(resource)
            );
            
            log.debug("Sending LaTeX compilation request to YToTech service...");
            
            byte[] responseBytes = webClient.post()
                .uri(LATEX_SERVICE_URL)
                .header("Content-Type", "application/json")
                .body(BodyInserters.fromValue(jsonBody))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(20))
                .block();
                
            // Validate that we received a valid PDF
            if (!isValidPdf(responseBytes)) {
                String responseText = responseBytes != null ? new String(responseBytes, StandardCharsets.UTF_8) : "null";
                log.warn("YToTech service returned non-PDF content. Response: {}", 
                        responseText.length() > 200 ? responseText.substring(0, 200) + "..." : responseText);
                throw new RuntimeException("Service returned non-PDF content (likely HTML error page)");
            }
            
            return responseBytes;
            
        } catch (Exception e) {
            log.error("🚨 YToTech LaTeX compilation failed: {}", e.getMessage());
            throw new RuntimeException("YToTech service error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate that the response is a valid PDF file
     * @param bytes Response bytes to validate
     * @return true if the bytes represent a valid PDF file
     */
    private boolean isValidPdf(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        
        // PDF files start with the magic bytes: %PDF
        String header = new String(bytes, 0, Math.min(4, bytes.length), StandardCharsets.ISO_8859_1);
        boolean isPdf = header.equals("%PDF");
        
        if (!isPdf) {
            // Log first few bytes for debugging
            String preview = new String(bytes, 0, Math.min(100, bytes.length), StandardCharsets.UTF_8);
            log.debug("Invalid PDF header. First 100 chars: {}", preview);
        } else {
            log.debug("Valid PDF detected. Size: {} bytes", bytes.length);
        }
        
        return isPdf;
    }
    
    /**
     * Validate LaTeX content before compilation
     * @param latexContent LaTeX document content to validate
     * @return true if content appears valid
     */
    public boolean validateLatexContent(String latexContent) {
        if (latexContent == null || latexContent.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation checks
        boolean hasDocumentClass = latexContent.contains("\\documentclass");
        boolean hasBeginDocument = latexContent.contains("\\begin{document}");
        boolean hasEndDocument = latexContent.contains("\\end{document}");
        
        boolean isValid = hasDocumentClass && hasBeginDocument && hasEndDocument;
        
        if (!isValid) {
            log.warn("❌ LaTeX validation failed - missing required elements:");
            log.warn("  - \\documentclass: {}", hasDocumentClass);
            log.warn("  - \\begin{{document}}: {}", hasBeginDocument);
            log.warn("  - \\end{{document}}: {}", hasEndDocument);
        }
        
        return isValid;
    }
    
    /**
     * Test connection to YToTech LaTeX service
     * @return true if the service is reachable
     */
    public boolean isLatexOnlineAvailable() {
        log.info("Testing YToTech LaTeX service availability...");
        
        String testLatex = "\\documentclass{article}\\begin{document}Test\\end{document}";
        
        try {
            log.debug("Testing YToTech service: {}", LATEX_SERVICE_URL);
            byte[] result = compileWithYToTechService(testLatex);
            if (result != null && result.length > 0) {
                log.info("✅ YToTech LaTeX service is available");
                return true;
            }
        } catch (Exception e) {
            log.warn("❌ YToTech LaTeX service test failed: {}", e.getMessage());
        }
        
        log.warn("❌ YToTech LaTeX service is currently unavailable");
        return false;
    }
}
