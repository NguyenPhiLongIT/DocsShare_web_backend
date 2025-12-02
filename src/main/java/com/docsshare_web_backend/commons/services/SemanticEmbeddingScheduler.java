package com.docsshare_web_backend.commons.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SemanticEmbeddingScheduler {

    private final SemanticEmbeddingService semanticEmbeddingService;

    @Scheduled(fixedDelayString = "${ml.embedding.reconcile-interval-ms:600000}")
    public void reconcilePendingDocuments() {
        semanticEmbeddingService.reconcilePendingDocuments();
    }
}

