package com.docsshare_web_backend.documents.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.filters.DocumentFilter;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.docsshare_web_backend.documents.services.DocumentService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentServiceImpl implements DocumentService{
    @Autowired
    private DocumentRepository documentRepository;
    
    private Pageable getPageable(Pageable pageable) {
        return pageable != null ? pageable : Pageable.unpaged();
    }

    public static class DocumentMapper {
        public static DocumentResponse toDocumentResponse(Document document) {
            log.debug("Mapping Document to DocumentResponse: {}", document);
            return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .description(document.getDescription())
                .typeFile(document.getTypeFile() != null ? document.getTypeFile().toString() : null)
                .filePath(document.getFilePath())
                .price(document.getPrice())
                .moderationStatus(document.getModerationStatus() != null ? document.getModerationStatus().toString() : null)
                .isPublic(document.isPublic())
                .coAuthor(document.getCoAuthor() != null ? document.getCoAuthor().toString() : null)
                .createdDate(document.getCreatedDate())
                // .author(UserMapper.toResponse(document.getAuthor()))
                // .category(CategoryMapper.toResponse(document.getCategory()))
                .build();
        }
    }

    @Override
    public Page<DocumentResponse> getAllDocuments(DocumentFilterRequest request, Pageable pageable) {
        log.debug("Fetching all documents with filter: {}, pageable: {}", request, pageable);
        Specification<Document> spec = DocumentFilter.filterByRequest(request);
        return documentRepository.findAll(spec, getPageable(pageable))
                .map(DocumentMapper::toDocumentResponse);
    }
}
