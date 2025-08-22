package com.docsshare_web_backend.documents.repositories;

import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.models.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository 
    extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    Optional<Document> findBySlug(String slug);
    List<Document> findByFileHash(String fileHash);
    boolean existsByFilePath(String filePath);

    @Query(value = """
        SELECT 
            d.id AS id,
            d.title,
            d.description,
            d.file_type AS fileType,
            d.slug,
            d.price,
            d.created_at AS createdAt,
            u.name AS authorName,
            c.name AS category,
            COALESCE(d.views, 0) AS viewCount,
            COUNT(DISTINCT sd.id) AS saveCount,
            COUNT(DISTINCT fp.id) AS relatedPostCount,
            COUNT(DISTINCT cm.id) AS relatedCommentCount,
            (COALESCE(d.views, 0) + COUNT(DISTINCT sd.id) + COUNT(DISTINCT fp.id) + COUNT(DISTINCT cm.id)) AS totalInteraction
        FROM document d
        JOIN user u ON d.author_id = u.id
        JOIN category c ON d.category_id = c.id
        LEFT JOIN saved_documents sd ON d.id = sd.document_id
        LEFT JOIN forum_post fp ON d.id = fp.document_id
        LEFT JOIN comment cm ON fp.id = cm.forum_post_id
        WHERE d.created_at BETWEEN :from AND :to
            AND d.moderation_status = 'APPROVED'
            AND d.is_public = true
        GROUP BY d.id, d.title, d.description, d.file_type, d.slug, d.price, d.created_at, u.name, c.name, d.views
        ORDER BY totalInteraction DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopDocumentsBetweenDates(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("limit") int top);
    @Query(value = """
        SELECT 
            d.author_id AS userId,
            u.name AS userName,
            COUNT(d.id) AS documentCount
        FROM document d
        JOIN user u ON d.author_id = u.id
        WHERE d.created_at BETWEEN :from AND :to
            AND d.moderation_status = 'APPROVED'
            AND d.is_public = true
        GROUP BY d.author_id, u.name
        ORDER BY documentCount DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopUsersAddDocumentBetweenDates(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("limit") int top);


}
