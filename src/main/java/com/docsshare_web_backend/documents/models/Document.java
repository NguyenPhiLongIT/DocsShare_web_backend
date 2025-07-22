package com.docsshare_web_backend.documents.models;

import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.categories.models.Category;
import com.docsshare_web_backend.documents.enums.DocumentFileType;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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
    @Column(unique = true)
    private String slug;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private String filePath;
    private String fileHash;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentFileType fileType;
    private Long views;
    private Double price;
    private String copyrightPath;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentModerationStatus moderationStatus;
    private String rejectedReason;
    @Column(nullable = false)
    private boolean isPublic;
    @CreatedDate
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentCoAuthor> coAuthors;
}
