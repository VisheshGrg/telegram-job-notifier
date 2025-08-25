package com.telegram_notifier.service;

import com.telegram_notifier.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.List;

@Service
public class AiService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AiService.class);

    private final AppProperties props;

    public AiService(AppProperties props) {
        this.props = props;
    }

    private WebClient client() {
        return WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public boolean isRelevant(String text) {
        String prompt = props.getAi().getGemini().getRelevancePrompt();
        String model = props.getAi().getGemini().getModel();
        String apiKey = props.getAi().getGemini().getApiKey();

        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt + "\n\nPOST:\n" + text)
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

            String textOut = extractText(res);
            boolean yes = textOut != null && textOut.trim().toUpperCase().startsWith("YES");
            log.debug("Gemini relevance response: {}", textOut);
            return yes;
        } catch (Exception e) {
            log.error("Gemini relevance call failed, defaulting to NO", e);
            return false;
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
            log.error("Failed to extract text from Gemini response", e);
            return null;
        }
    }
}