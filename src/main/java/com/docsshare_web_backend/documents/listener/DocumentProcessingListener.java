package com.docsshare_web_backend.documents.listener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.docsshare_web_backend.commons.services.SemanticEmbeddingService;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.docsshare_web_backend.commons.services.CbirService;
import com.docsshare_web_backend.commons.services.GoogleDriveService;
import com.docsshare_web_backend.documents.enums.DocumentProcessingStatus;
import com.docsshare_web_backend.documents.events.DocumentCreatedEvent;
import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.models.DocumentImage;
import com.docsshare_web_backend.documents.repositories.DocumentImageRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingListener {

    private final DocumentRepository documentRepository;
    private final SemanticEmbeddingService semanticEmbeddingService;
    private final CbirService documentImageService;
    private final DocumentImageRepository documentImageRepository;
    private final GoogleDriveService googleDriveService;
    
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DocumentCreatedEvent event) {
        Document document = documentRepository.findById(event.getDocumentId())
            .orElse(null);

        if (document == null) return;

        try {
            document.setProcessingStatus(DocumentProcessingStatus.PROCESSING);
            documentRepository.save(document);

            boolean embeddedOk = false;
            try {
                embeddedOk = semanticEmbeddingService.embedDocument(
                    document.getId(),
                    document.getTitle(),
                    document.getDescription(), // summary tạm
                    document.getDescription(),
                    document.getCategory() != null ? document.getCategory().getId() : null
                );

                log.info("Semantic embedding triggered for document {}", document.getId());

            } catch (Exception e) {
                log.error("Semantic embedding failed for document {}", document.getId(), e);
            }

            if (embeddedOk) {
                document.setSemanticEmbedded(true);
                document.setSemanticEmbeddedAt(LocalDateTime.now());
                documentRepository.save(document);
            }

            // 🔑 Tải file gốc từ Google Drive
            File localFile = googleDriveService.downloadToTempFile(
                document.getFilePath(),
                document.getFileType()
            );
            log.info("Downloading file from path: {}", document.getFilePath());
            log.info("Extracting images from file: {}", localFile.getAbsolutePath());
            List<CbirService.ImageFeatureResult> imageResults =
                documentImageService.extractImagesAndFeatures(localFile);
            log.info("Found {} images", imageResults.size());
            for (CbirService.ImageFeatureResult img : imageResults) {
                try {
                    List<Double> features = img.getFeatures();

                    DocumentImage docImage = DocumentImage.builder()
                        .document(document)
                        .imagePath(img.getUrl())
                        .featureVector(new ObjectMapper().writeValueAsString(features))
                        .build();

                    DocumentImage savedDocImage =
                        documentImageRepository.save(docImage);

                    // Push feature sang Flask
                    documentImageService.pushFeatureToFlask(
                        savedDocImage.getId(),
                        document.getId(),
                        savedDocImage.getImagePath(),
                        features
                    );

                } catch (Exception e) {
                    log.error("Failed processing image for document {}", document.getId(), e);
                }
            }

            document.setProcessingStatus(DocumentProcessingStatus.DONE);
            documentRepository.save(document);

            log.info("AI processing DONE for document {}", document.getId());

        } catch (Exception e) {
            document.setProcessingStatus(DocumentProcessingStatus.FAILED);
            documentRepository.save(document);
            log.error("AI processing FAILED for document {}", document.getId(), e);
        }
    }
}