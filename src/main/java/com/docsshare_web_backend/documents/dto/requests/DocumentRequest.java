package com.docsshare_web_backend.documents.dto.requests;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String description;
    @JsonIgnore
    private MultipartFile file;
    @NotNull(message = "Slug should not be null")
    private String slug;
    private Double price;
    private String copyrightPath;
    private String moderationStatus;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private Long userId;
    private Long categoryId;
    private List<DocumentCoAuthorRequest> coAuthor;
}
