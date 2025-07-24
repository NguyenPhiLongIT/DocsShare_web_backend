package com.docsshare_web_backend.documents.dto.requests;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class DocumentUpdateRequest {
    private String title;
    private String description;
    private String slug;
    private Double price;
    private String copyrightPath;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private Long userId;
    private Long categoryId;
    private List<DocumentCoAuthorRequest> coAuthor;
}
