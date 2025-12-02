package com.docsshare_web_backend.documents.events;

import org.springframework.context.ApplicationEvent;

public class DocumentCreatedEvent extends ApplicationEvent {
    private final Long documentId;

    public DocumentCreatedEvent(Object source, Long documentId) {
        super(source);
        this.documentId = documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }
}

