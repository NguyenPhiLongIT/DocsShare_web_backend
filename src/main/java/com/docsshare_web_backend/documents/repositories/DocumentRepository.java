package com.docsshare_web_backend.documents.repositories;

import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.models.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DocumentRepository 
    extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    Optional<Document> findBySlug(String slug);
    Optional<Document> findByFileHash(String fileHash);
}
