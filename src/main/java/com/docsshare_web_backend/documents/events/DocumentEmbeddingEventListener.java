package com.docsshare_web_backend.documents.events;

import com.docsshare_web_backend.commons.services.SemanticEmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentEmbeddingEventListener {

    private final SemanticEmbeddingService semanticEmbeddingService;

    @EventListener
    public void handleDocumentCreated(DocumentCreatedEvent event) {
        semanticEmbeddingService.triggerEmbedding(event.getDocumentId());
    }
}

