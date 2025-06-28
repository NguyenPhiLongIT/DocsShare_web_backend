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
public class DocumentRequest {
    @NotBlank(message = "Title should not be blank")
    @NotNull(message = "Title should not be null")
    private String title;
    private String desciption;
    @NotBlank(message = "File should not be blank")
    @NotNull(message = "File should not be null")
    private String filePath;
    @NotBlank(message = "Slug should not be blank")
    @NotNull(message = "Slug should not be null")
    private String slug;
    private Double price;
    private String copyrightPath;
    private String moderationStatus;
    private boolean isPublic;
    private String coAuthor;
    private Long userId;
    private Long categoryId;
}
