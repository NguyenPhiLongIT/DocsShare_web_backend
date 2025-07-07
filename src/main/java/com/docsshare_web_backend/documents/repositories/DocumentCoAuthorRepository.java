package com.docsshare_web_backend.documents.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.docsshare_web_backend.documents.models.DocumentCoAuthor;

import java.util.List;

@Repository
public interface DocumentCoAuthorRepository 
    extends JpaRepository<DocumentCoAuthor, Long>, JpaSpecificationExecutor<DocumentCoAuthor> {
        
    boolean existsByDocumentIdAndEmail(Long documentId, String email);
    List<DocumentCoAuthor> findByNameAndEmail(String name, String email);
}
