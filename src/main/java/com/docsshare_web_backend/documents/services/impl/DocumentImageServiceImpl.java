package com.docsshare_web_backend.documents.services.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.docsshare_web_backend.commons.services.CbirService;
import com.docsshare_web_backend.documents.dto.responses.DocumentImageResponse;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.models.DocumentImage;
import com.docsshare_web_backend.documents.repositories.DocumentImageRepository;
import com.docsshare_web_backend.documents.services.DocumentImageService;
import com.docsshare_web_backend.documents.services.DocumentService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentImageServiceImpl implements DocumentImageService {
    @Autowired
    private DocumentImageRepository documentImageRepository;
    @Autowired
    private CbirService cbirService;
    @Autowired
    private DocumentService documentService;

    public static class DocumentImageMapper {
        public static DocumentImageResponse toDocumentImageResponse(DocumentImage img) {
            return DocumentImageResponse.builder()
                    .id(img.getId())
                    .imagePath(img.getImagePath())
                    .documentId(img.getDocument().getId())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentImageResponse> getAllImagesWithFeatures() {
        List<DocumentImage> images = documentImageRepository.findAll();
        return images.stream()
                .map(DocumentImageMapper::toDocumentImageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<DocumentImageResponse> searchSimilarImages(MultipartFile file) {
        JsonNode response = cbirService.searchImage(file);

        List<DocumentImageResponse> results = new ArrayList<>();
        for (JsonNode item : response.get("results")) {
            Long documentId = item.has("documentId") && !item.get("documentId").isNull()
                ? item.get("documentId").asLong()
                : null;

        DocumentResponse documentResponse = null;
        if (documentId != null) {
            try {
                documentResponse = documentService.getDocument(documentId);
            } catch (Exception e) {
                System.out.println("Document not found for ID: " + documentId);
            }
        }
            results.add(DocumentImageResponse.builder()
                    .id(item.get("id").asLong())
                    .imagePath(item.get("imagePath").asText())
                    .documentId(documentId)
                    .document(documentResponse)
                    .build());
        }

        return results;
    }
}
