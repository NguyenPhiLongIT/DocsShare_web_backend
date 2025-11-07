package com.docsshare_web_backend.commons.services;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ToxicService {
    @Value("${ml.api.url}")
    private String apiUrl;    

    @Data
    public static class ToxicLabels {
        private Integer toxic;
        @JsonProperty("severe_toxic") private Integer severeToxic;
        private Integer obscene;
        private Integer threat;
        private Integer insult;
        @JsonProperty("identity_hate") private Integer identityHate;

        private static int v(Integer x) { return x == null ? 0 : x; }

        public boolean anyPositive() {
            return v(toxic)==1 || v(severeToxic)==1 || v(obscene)==1
                || v(threat)==1 || v(insult)==1 || v(identityHate)==1;
        }
    }

    private final RestTemplate restTemplate = new RestTemplate();

    public ToxicLabels predict(String text) {
        String url = apiUrl + "/predict";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("lang", "auto"); 

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ToxicLabels> res =
                restTemplate.postForEntity(url, entity, ToxicLabels.class);
            if (res.getBody() == null) throw new IllegalStateException("Empty response from ML API");
            return res.getBody();
        } catch (HttpStatusCodeException e) {
            String err = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            log.error("ML API /predict error: status={}, body={}", e.getStatusCode(), err, e);
            throw new RuntimeException("Machine learning service error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Error calling ML API /predict", e);
            throw new RuntimeException("Machine learning service unavailable", e);
        }
    }

    public boolean isToxic(String text) {
        ToxicLabels labels = predict(text);
        return labels.anyPositive();
    }

    public void validateTextSafety(String text, String fieldLabel) {
        if (isToxic(text)) {
            throw new IllegalArgumentException(fieldLabel + " contains inappropriate language and cannot be posted!");
        }
    }
}