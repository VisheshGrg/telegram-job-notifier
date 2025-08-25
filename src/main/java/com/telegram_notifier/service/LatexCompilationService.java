package com.telegram_notifier.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;

@Service
public class LatexCompilationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LatexCompilationService.class);
    
    private final WebClient webClient;
    private static final String LATEX_ONLINE_URL = "https://latexonline.cc/compile";
    
    public LatexCompilationService() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(10 * 1024 * 1024)) // 10MB for PDF files
            .build();
    }
    
    /**
     * Compile LaTeX content to PDF using LaTeX Online service
     * @param latexContent The complete LaTeX document content
     * @return PDF file as byte array
     * @throws RuntimeException if compilation fails
     */
    public byte[] compileLatexToPdf(String latexContent) {
        if (latexContent == null || latexContent.trim().isEmpty()) {
            throw new IllegalArgumentException("LaTeX content cannot be null or empty");
        }
        
        log.info("🔨 Compiling LaTeX document using LaTeX Online service...");
        log.debug("LaTeX content length: {} characters", latexContent.length());
        
        try {
            byte[] pdfBytes = webClient.post()
                .uri(LATEX_ONLINE_URL)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(BodyInserters.fromFormData("text", latexContent))
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(30)) // 30 second timeout for compilation
                .block();
            
            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("LaTeX Online returned empty PDF");
            }
            
            log.info("✅ LaTeX compilation successful. PDF size: {} bytes", pdfBytes.length);
            return pdfBytes;
            
        } catch (WebClientResponseException e) {
            log.error("❌ LaTeX Online compilation failed with HTTP {}: {}", e.getStatusCode(), e.getMessage());
            
            // Try to extract error details from response body
            String errorBody = e.getResponseBodyAsString();
            if (errorBody != null && !errorBody.isEmpty()) {
                log.error("LaTeX compilation error details: {}", errorBody);
            }
            
            throw new RuntimeException("LaTeX compilation failed: HTTP " + e.getStatusCode() + " - " + e.getMessage());
            
        } catch (Exception e) {
            log.error("❌ Unexpected error during LaTeX compilation", e);
            throw new RuntimeException("LaTeX compilation failed: " + e.getMessage(), e);
        }
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
     * Test connection to LaTeX Online service
     * @return true if service is reachable
     */
    public boolean isLatexOnlineAvailable() {
        try {
            String testLatex = "\\documentclass{article}\\begin{document}Test\\end{document}";
            byte[] result = compileLatexToPdf(testLatex);
            return result != null && result.length > 0;
        } catch (Exception e) {
            log.warn("LaTeX Online service test failed: {}", e.getMessage());
            return false;
        }
    }
}
