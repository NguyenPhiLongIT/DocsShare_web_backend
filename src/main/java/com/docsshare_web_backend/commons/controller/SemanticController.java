package com.docsshare_web_backend.commons.controller;

import com.docsshare_web_backend.commons.services.SemanticSearchService;
import com.docsshare_web_backend.commons.services.SemanticSearchService.SemanticResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/semantic")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SemanticController {

    private final SemanticSearchService semanticSearchService;

    @GetMapping("/search")
    public SemanticResponse search(@RequestParam String query) {
        return semanticSearchService.search(query);
    }
}
