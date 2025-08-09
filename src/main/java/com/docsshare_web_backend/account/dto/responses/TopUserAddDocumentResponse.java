package com.docsshare_web_backend.account.dto.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUserAddDocumentResponse {
    private Long userId;
    private String userName;
    private int documentCount;
}
