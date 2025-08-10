package com.docsshare_web_backend.documents.dto.requests;

import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateStatusRequest {
    private Long senderId;
    private DocumentModerationStatus status;
    private String rejectedReason;
}