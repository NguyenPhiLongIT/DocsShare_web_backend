package com.docsshare_web_backend.documents.domain;

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.requests.DocumentRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.documents.services.DocumentService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    @Autowired
    private DocumentService documentService;

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

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getDocumentsByCategoryId(
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

    @PostMapping("/create")
    public ResponseEntity<DocumentResponse> createDocument(@RequestBody DocumentRequest documentRequest) {
        log.debug("[DocumentController] Create Document {}", documentRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createDocument(documentRequest));
    }
}
