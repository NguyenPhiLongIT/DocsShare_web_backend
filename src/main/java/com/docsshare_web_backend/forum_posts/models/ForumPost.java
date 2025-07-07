package com.docsshare_web_backend.forum_posts.models;

import com.docsshare_web_backend.categories.models.Category;
import com.docsshare_web_backend.comments.models.Comment;
import com.docsshare_web_backend.forum_posts.enums.ForumPostStatus;
import com.docsshare_web_backend.forum_posts.enums.ForumPostType;
import com.docsshare_web_backend.users.models.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ForumPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForumPostType type;

    @Column(nullable = false)
    private Boolean isPublic;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updateAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "forumPost", cascade = CascadeType.ALL)
    private List<Comment> comments;

}
