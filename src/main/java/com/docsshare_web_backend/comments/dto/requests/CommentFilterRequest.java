package com.docsshare_web_backend.comments.dto.requests;

import com.docsshare_web_backend.comments.enums.CommentType;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
public class CommentFilterRequest {
    private String q;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt_from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt_to;
    private CommentType type;
}
