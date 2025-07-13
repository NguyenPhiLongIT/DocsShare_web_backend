package com.docsshare_web_backend.order.dto.responses;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {
    private Long id;
    private Long documentId;
    private String documentTitle;
    private double price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
