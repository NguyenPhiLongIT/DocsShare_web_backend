package com.docsshare_web_backend.saved_documents.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsCountProjection;
import com.docsshare_web_backend.saved_documents.models.SavedDocuments;

import java.util.Optional;
import java.util.List;

@Repository
public interface SavedDocumentsRepository 
    extends JpaRepository<SavedDocuments, Long>, JpaSpecificationExecutor<SavedDocuments>{
    
    Optional<SavedDocuments> findByUserIdAndDocumentId(Long userId, Long documentId);
    boolean existsByUserIdAndDocumentId(Long userId, Long documentId);
    
    @Query("""
        SELECT sd.document.id AS documentId, COUNT(sd) AS saveCount
        FROM SavedDocuments sd
        WHERE sd.document.id IN :documentIds
        GROUP BY sd.document.id
    """)
    List<SavedDocumentsCountProjection> countSavesByDocumentIds(@Param("documentIds") List<Long> documentIds);

    @Query("SELECT sd.document.id FROM SavedDocuments sd WHERE sd.user.id = :userId AND sd.document.id IN :documentIds")
    List<Long> findSavedDocumentIdsByUserId(@Param("userId") Long userId, @Param("documentIds") List<Long> documentIds);

}
