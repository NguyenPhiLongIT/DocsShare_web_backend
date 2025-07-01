package com.docsshare_web_backend.forum_posts.dto.requests;

import com.docsshare_web_backend.forum_posts.enums.ForumPostStatus;
import com.docsshare_web_backend.forum_posts.enums.ForumPostType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
public class ForumPostFilterRequest {
//    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
//    private LocalDate createdDate_from;
//    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
//    private LocalDate createdDate_to;
    private ForumPostType type;
    private Boolean isPublic;
}
