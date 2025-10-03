package com.docsshare_web_backend.documents.repositories;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.docsshare_web_backend.documents.models.DocumentImage;

@Repository
public interface DocumentImageRepository 
    extends JpaRepository<DocumentImage, Long>, JpaSpecificationExecutor<DocumentImage>{
    
}
