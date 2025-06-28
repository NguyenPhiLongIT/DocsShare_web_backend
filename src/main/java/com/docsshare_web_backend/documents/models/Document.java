package com.docsshare_web_backend.documents.models;

import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.categories.models.Category;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.policy.models.Policy;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String slug;
    @Column(nullable = false)
    private String title;
    private String description;
    @Column(nullable = false)
    private String filePath;
    private Double price;
    private String copyrightPath;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentModerationStatus moderationStatus;
    @Column(nullable = false)
    private boolean isPublic;
    @Column(columnDefinition = "json")
    private String coAuthor;
    @CreatedDate
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;
}
