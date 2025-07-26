package com.docsshare_web_backend.documents.domain;

import com.docsshare_web_backend.documents.dto.requests.DocumentCoAuthorRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentUpdateRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentUpdateStatusRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentCoAuthorResponse;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.enums.DocumentModerationStatus;
import com.docsshare_web_backend.documents.services.DocumentCoAuthorService;
import com.docsshare_web_backend.documents.services.DocumentService;
import com.docsshare_web_backend.commons.services.SummaryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    @Autowired
    private DocumentService documentService;
    @Autowired
    private SummaryService summaryService;
    @Autowired
    private DocumentCoAuthorService documentCoAuthorService;

    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> getAllDocuments(
            @ModelAttribute DocumentFilterRequest request, 
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        log.debug("Received request to get all documents with filter: {}, page: {}, size: {}, sort: {}", 
                request, page, size, sort);
        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<DocumentResponse> documents = documentService.getAllDocuments(request, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable long documentId) {
        log.debug("[DocumentController] Get Document with id {}", documentId);
        return ResponseEntity.ok(documentService.getDocument(documentId));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<DocumentResponse> getDocumentBySlug(@PathVariable String slug) {
        log.debug("[DocumentController] Get Document by slug {}", slug);
        return ResponseEntity.ok(documentService.getDocumentBySlug(slug));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByUserId(
            @PathVariable long userId,
            @ModelAttribute DocumentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<DocumentResponse> documents = documentService.getDocumentsByUserId(request, userId, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/author/{userId}")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByAuthorOrCoAuthorId(
            @PathVariable long userId,
            @ModelAttribute DocumentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<DocumentResponse> documents = documentService.getDocumentsByAuthorOrCoAuthorId(request, userId, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByCategoryId(
            @PathVariable long categoryId,
            @ModelAttribute DocumentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {

        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<DocumentResponse> documents = documentService.getDocumentsByCategoryId(request, categoryId, pageable);
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/need-approved")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsNeedApproved(
            @ModelAttribute DocumentFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort){

        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<DocumentResponse> documents = documentService.getDocumentsNeedApproved(request, pageable);
        return ResponseEntity.ok(documents);       
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> createDocument(
        @RequestPart("data") String dataJson,
        @RequestPart("file") MultipartFile file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            DocumentRequest documentRequest = objectMapper.readValue(dataJson, DocumentRequest.class);
            documentRequest.setFile(file); // set file vào sau

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(documentService.createDocument(documentRequest));
        } catch (Exception e) {
            log.error("Error creating document", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{documentId}/update")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable long documentId,
            @RequestBody DocumentUpdateRequest request) {
        log.debug("[DocumentController] Update Document with id {}", documentId);
        return ResponseEntity.ok(documentService.updateDocument(documentId, request));
    }

    @PutMapping("/{documentId}/updateStatus")
    public ResponseEntity<DocumentResponse> updateStatus(@PathVariable Long documentId, @RequestBody DocumentUpdateStatusRequest request) {
        DocumentResponse updatedDoc = documentService.updateDocumentStatus(documentId, request);
        return ResponseEntity.ok(updatedDoc);
    }

    @PostMapping("/{documentId}/incrementView")
    public ResponseEntity<DocumentResponse> incrementView(@PathVariable long documentId) {
        return ResponseEntity.ok(documentService.incrementView(documentId));
    }

    @PostMapping(value = "/generateSummary", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> generateDescription(@RequestParam("file") MultipartFile file) {
        String summary = summaryService.summarizeFile(file);
        if (summary != null) {
            return ResponseEntity.ok(Map.of("description", summary));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not generate summary"));
        }
    }

    @PostMapping("/addCoAuthor")
    public ResponseEntity<DocumentCoAuthorResponse> addCoAuthor(@RequestParam long documentId, 
                @RequestBody @Valid DocumentCoAuthorRequest request) {
        DocumentCoAuthorResponse response = documentCoAuthorService.addCoAuthor(documentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/removeCoAuthor")
    public ResponseEntity<Void> removeCoAuthor(
            @RequestParam Long documentId,
            @RequestParam String email) {
        documentCoAuthorService.removeCoAuthor(documentId, email);
        return ResponseEntity.noContent().build();
    }
}



