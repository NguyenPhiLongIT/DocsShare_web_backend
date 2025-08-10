package com.docsshare_web_backend.saved_documents.domain;

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

import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;
import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsFilterRequest;
import com.docsshare_web_backend.saved_documents.dto.requests.SavedDocumentsRequest;
import com.docsshare_web_backend.saved_documents.dto.responses.SavedDocumentsResponse;
import com.docsshare_web_backend.saved_documents.services.SavedDocumentsService;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.MediaType;

@Slf4j
@RestController
@RequestMapping("/api/v1/saved-documents")
public class SavedDocumentsController {
    @Autowired
    private SavedDocumentsService savedDocumentsService;

    @GetMapping("/{userId}")
    public ResponseEntity<Page<SavedDocumentsResponse>> getSavedDocumentsByUserId(
            @PathVariable long userId,
            @ModelAttribute SavedDocumentsFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort){
        
        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<SavedDocumentsResponse> savedDocuments = savedDocumentsService.getSavedDocumentsByUserId(request, userId, pageable);
        return ResponseEntity.ok(savedDocuments);
    }

    @PostMapping("/save")
    public ResponseEntity<SavedDocumentsResponse> saveDocument(@RequestBody SavedDocumentsRequest request) {
        return ResponseEntity.ok(savedDocumentsService.saveDocument(request));
    }

    @PostMapping("/unsave")
    public ResponseEntity<?> unsaveDocument(@RequestBody SavedDocumentsRequest request){
        savedDocumentsService.unsaveDocument(request);
        return ResponseEntity.ok(Map.of("message", "Document unsaved successfully"));
    }

    @PostMapping("/saved/check")
    public ResponseEntity<Map<String, Boolean>> checkDocumentSaved(
            @RequestBody SavedDocumentsRequest request) {
        boolean saved = savedDocumentsService.isDocumentSaved(request);
        return ResponseEntity.ok(Collections.singletonMap("saved", saved));
    }

}
