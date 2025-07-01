package com.docsshare_web_backend.payment.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
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
