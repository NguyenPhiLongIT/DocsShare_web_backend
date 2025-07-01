package com.docsshare_web_backend.reasons.dto.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonResponse {
    private int id;
    private String name;
    private String description;
}
