package com.docsshare_web_backend.reasons.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasonRequest {
    @NotBlank(message = "Name should not be blank")
    @NotNull(message = "Name should not be null")
    private String name;
    private String description;
}
