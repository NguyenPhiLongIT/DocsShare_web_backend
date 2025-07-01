package com.docsshare_web_backend.payment.dto.responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private String slug;
    private Double price;
    private String copyrightPath;
    private String moderationStatus;
    private boolean isPublic;
    private String coAuthor;
    private LocalDateTime createdAt;
    private String authorName;
    private String category;
}
