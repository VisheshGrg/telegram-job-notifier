package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import com.telegram_notifier.model.JobDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class ExtractionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExtractionService.class);

    private final AppProperties props;

    public ExtractionService(AppProperties props) {
        this.props = props;
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WebClient client() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public JobDetails extract(String post, String sourceChannel) {
        String model = props.getAi().getGemini().getModel();
        String apiKey = props.getAi().getGemini().getApiKey();
        
        String prompt = "Extract the following fields from the job post, and return in JSON format:\n" +
                "{\n" +
                "  \"company\": \"company name\",\n" +
                "  \"role\": \"job role/title\",\n" +
                "  \"location\": \"job location\",\n" +
                "  \"url\": \"application URL or company URL\",\n" +
                "  \"salary\": \"salary information\",\n" +
                "  \"rawSnippet\": \"a short snippet from the post (max 200 chars)\"\n" +
                "}\n" +
                "If any field is missing, return empty string for that field.\n" +
                "Return ONLY valid JSON, no additional text.\n\n" +
                "POST:\n" + post;

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
            )
        );

        try {
            Map<?, ?> res = client()
                    .post()
                    .uri("/v1beta/models/" + model + ":generateContent?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String json = extractText(res);
            if (json == null) {
                log.warn("No response from Gemini extraction API");
                return null;
            }

            // Clean JSON response (remove markdown formatting if present)
            json = json.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7);
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3);
            }
            json = json.trim();

            @SuppressWarnings("unchecked")
            Map<String, String> m = objectMapper.readValue(json, Map.class);

            return JobDetails.builder()
                    .company(s(m.get("company")))
                    .role(s(m.get("role")))
                    .location(s(m.get("location")))
                    .url(s(m.get("url")))
                    .salary(s(m.get("salary")))
                    .rawSnippet(s(m.get("rawSnippet")))
                    .sourceChannel(sourceChannel)
                    .postedAt(java.time.OffsetDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("Gemini extraction call failed, returning null", e);
            return null;
        }
    }

    private String extractText(Map<?, ?> res) {
        if (res == null) return null;
        try {
            var candidates = (List<?>) res.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;
            var content = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            var parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;
            var first = (Map<?, ?>) parts.get(0);
            return (String) first.get("text");
        } catch (Exception e) {
            log.error("Failed to extract text from response", e);
            return null;
        }
    }

    private String s(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}