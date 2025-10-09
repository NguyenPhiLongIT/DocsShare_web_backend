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
import com.docsshare_web_backend.documents.models.DocumentImage;
import com.docsshare_web_backend.documents.repositories.DocumentImageRepository;
import com.docsshare_web_backend.documents.services.DocumentImageService;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentImageServiceImpl implements DocumentImageService {
    @Autowired
    private DocumentImageRepository documentImageRepository;
    @Autowired
    private CbirService cbirService;

    public static class DocumentImageMapper {
        public static DocumentImageResponse toDocumentImageResponse(DocumentImage img) {
            List<Double> featureVector = new ArrayList<>();

            try {
                // parse string lưu trong database thành list<Double>
                featureVector = Arrays.stream(
                        img.getFeatureVector()
                                .replace("[", "")
                                .replace("]", "")
                                .split(",")
                )
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Double::parseDouble)
                .collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi parse featureVector cho ảnh {}: {}", img.getImagePath(), e.getMessage());
            }

            return DocumentImageResponse.builder()
                    .id(img.getId())
                    .imagePath(img.getImagePath())
                    .uploadedAt(img.getUploadedAt())
                    .documentId(img.getDocument().getId())
                    .featureVector(featureVector)
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
            results.add(DocumentImageResponse.builder()
                    .id(item.get("id").asLong())
                    .imagePath(item.get("imagePath").asText())
                    .build());
        }

        return results;
    }
}
