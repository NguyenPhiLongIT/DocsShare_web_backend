package com.docsshare_web_backend.forum_posts.dto.requests;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
public class ForumPostFilterRequest {
    private String q;
   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
   private LocalDate createdAt_from;
   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
   private LocalDate createdAt_to;
    private Boolean isPublic;
}
