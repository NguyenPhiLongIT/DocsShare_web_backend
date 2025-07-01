package com.docsshare_web_backend.documents.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class DocumentCoAuthorRequest {
    @NotBlank(message = "name should not be blank")
    @NotNull(message = "name should not be null")
    private String name;
    @NotBlank(message = "email should not be blank")
    @NotNull(message = "email should not be null")
    private String email;
}
