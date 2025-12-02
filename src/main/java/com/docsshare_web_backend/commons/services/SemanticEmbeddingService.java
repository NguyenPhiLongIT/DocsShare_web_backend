package com.docsshare_web_backend.commons.services;

import com.docsshare_web_backend.documents.models.Document;
import com.docsshare_web_backend.documents.repositories.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticEmbeddingService {

    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;
    private final ReentrantLock modelLock = new ReentrantLock();

    @Value("${ml.embedding.enabled:true}")
    private boolean embeddingEnabled;

    @Value("${ml.embedding.script-path:flask_server/semantic/update_semantic_model.py}")
    private String embeddingScriptPath;

    @Value("${ml.embedding.python-bin:python}")
    private String pythonBinary;

    @Value("${ml.embedding.timeout-seconds:900}")
    private long timeoutSeconds;

    @Value("${ml.embedding.working-dir:flask_server}")
    private String workingDirectory;

    @PostConstruct
    void logConfig() {
        if (!embeddingEnabled) {
            log.warn("Semantic embedding automation is disabled via configuration.");
        } else {
            log.info("Semantic embedding automation enabled. Script: {}", embeddingScriptPath);
        }
    }


    public void triggerEmbedding(Long documentId) {
        if (!embeddingEnabled) {
            log.debug("Embedding disabled; skip document {}", documentId);
            return;
        }

        documentRepository.findById(documentId)
                .ifPresentOrElse(this::processDocumentEmbedding,
                        () -> log.warn("Cannot trigger embedding; document {} not found", documentId));
    }

    public void reconcilePendingDocuments() {
        if (!embeddingEnabled) {
            return;
        }

        List<Document> pendingDocs = documentRepository.findTop20BySemanticEmbeddedFalseOrderByCreatedAtAsc();
        if (pendingDocs.isEmpty()) {
            return;
        }
        log.info("Found {} documents pending embedding. Triggering updates...", pendingDocs.size());
        pendingDocs.stream()
                .map(Document::getId)
                .forEach(this::triggerEmbedding);
    }

    private void processDocumentEmbedding(Document document) {
        if (document.isSemanticEmbedded()) {
            log.debug("Document {} already embedded; skip", document.getId());
            return;
        }

        if (!modelLock.tryLock()) {
            log.warn("Embedding script already running. Document {} will be retried later.", document.getId());
            return;
        }

        try {
            runEmbeddingScript(document);
        } finally {
            modelLock.unlock();
        }
    }

    private void runEmbeddingScript(Document document) {
        try {
            List<String> command = buildCommand(document.getId());
            ProcessBuilder builder = new ProcessBuilder(command);
            if (workingDirectory != null && !workingDirectory.isBlank()) {
                builder.directory(new File(workingDirectory));
            }
            builder.redirectErrorStream(false);

            Process process = builder.start();
            sendPayload(document, process);

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.error("Embedding script timeout ({}) for document {}", timeoutSeconds, document.getId());
                return;
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                markEmbedded(document);
                log.info("Embedding updated for document {}. Script output: {}", document.getId(), stdout.trim());
            } else {
                log.error("Embedding script failed for doc {} (exit {}). stdout: {} stderr: {}",
                        document.getId(), exitCode, stdout, stderr);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Embedding script interrupted for document {}", document.getId(), e);
        } catch (IOException e) {
            log.error("Embedding script IO error for document {}", document.getId(), e);
        }
    }

    private void sendPayload(Document document, Process process) throws IOException {
        EmbeddingPayload payload = EmbeddingPayload.builder()
                .docId(document.getId())
                .categoryId(document.getCategory() != null ? document.getCategory().getId() : null)
                .categoryName(document.getCategory() != null ? document.getCategory().getName() : null)
                .title(document.getTitle())
                .description(Optional.ofNullable(document.getDescription()).orElse(""))
                .summary(Optional.ofNullable(document.getDescription()).orElse(document.getTitle()))
                .publicDocument(document.isPublic())
                .moderationStatus(document.getModerationStatus() != null ? document.getModerationStatus().name() : null)
                .build();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(objectMapper.writeValueAsString(payload));
            writer.flush();
        }
    }

    private void markEmbedded(Document document) {
        document.setSemanticEmbedded(true);
        document.setSemanticEmbeddedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    private List<String> buildCommand(Long documentId) {
        List<String> command = new ArrayList<>();
        command.add(pythonBinary);
        command.add(embeddingScriptPath);
        command.add("--doc-id");
        command.add(String.valueOf(documentId));
        return command;
    }

    private String readStream(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            char[] buffer = new char[2048];
            StringBuilder builder = new StringBuilder();
            int len;
            while ((len = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, len);
            }
            return builder.toString();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class EmbeddingPayload {
        private Long docId;
        private Long categoryId;
        private String categoryName;
        private String title;
        private String description;
        private String summary;
        private boolean publicDocument;
        private String moderationStatus;
    }
}
