package com.docsshare_web_backend.commons.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ToxicService {
    @Value("${ml.api.url}")
    private String apiUrl;    

    @Data
    public static class ToxicResult {
        private String text;
        private double toxic;
        private double severe_toxic;
        private double obscene;
        private double threat;
        private double insult;
        private double identity_hate;
    }

    private final RestTemplate restTemplate = new RestTemplate();

    private Set<String> vietnameseBadWords = new HashSet<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("vn_offensive_words.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    vietnameseBadWords.add(line.trim().toLowerCase());
                }
            }
            log.info("Loaded {} Vietnamese offensive words.", vietnameseBadWords.size());
        } catch (Exception e) {
            log.error("Failed to load vn_offensive_words.txt", e);
        }
    }

    public ToxicResult predict(String text) {
        String url = apiUrl + "/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("text", text);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ToxicResult> response =
                    restTemplate.postForEntity(url, entity, ToxicResult.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling ML API /predict", e);
            throw new RuntimeException("Machine learning service unavailable", e);
        }
    }

    public void validateTextSafety(String text, String label) {
        if (containsVietnameseOffensiveWords(text)) {
            throw new IllegalArgumentException(label + " chứa ngôn ngữ không phù hợp và không thể đăng.");
        }

        ToxicResult result = predict(text);
        if (isToxic(result)) {
            throw new IllegalArgumentException(label + " contains inappropriate language and cannot be posted.");
        }
    }

    private boolean containsVietnameseOffensiveWords(String text) {
        String normalized = text.toLowerCase();
        for (String badWord : vietnameseBadWords) {
            if (normalized.contains(badWord)) {
                return true;
            }
        }
        return false;
    }

    private boolean isToxic(ToxicResult result) {
        return result.getToxic() > 60 || result.getInsult() > 60 || result.getObscene() > 60 || result.getThreat() > 30 || result.getSevere_toxic() > 30 || result.getIdentity_hate() > 30;
    }
}
