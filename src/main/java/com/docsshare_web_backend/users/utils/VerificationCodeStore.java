package com.docsshare_web_backend.users.utils;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class VerificationCodeStore {
    private final Map<String, String> codeMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> expiryMap = new ConcurrentHashMap<>();

    public void saveCode(String email, String code) {
        codeMap.put(email, code);
        expiryMap.put(email, LocalDateTime.now().plusMinutes(10));
    }

    public boolean isValid(String email, String code) {
        return code.equals(codeMap.get(email)) &&
                expiryMap.getOrDefault(email, LocalDateTime.MIN).isAfter(LocalDateTime.now());
    }

    public void remove(String email) {
        codeMap.remove(email);
        expiryMap.remove(email);
    }
}
