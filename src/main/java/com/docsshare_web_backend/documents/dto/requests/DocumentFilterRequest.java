package com.docsshare_web_backend.documents.dto.requests;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.enums.DocumentFileType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DocumentFilterRequest {
    private String q;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt_from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt_to;
    private Boolean isPublic;
    private DocumentModerationStatus moderationStatus;
    private DocumentFileType fileType;
    private Double price;
}
