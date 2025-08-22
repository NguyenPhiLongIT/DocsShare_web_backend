package com.docsshare_web_backend.documents.models;

import com.docsshare_web_backend.users.models.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Table(
    name = "document_co_author",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"document_id", "email"})
    }
)
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class DocumentCoAuthor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = true)
    private String name;

    @Column(nullable = true)
    private String email;

    private Boolean isConfirmed;
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
