package com.docsshare_web_backend.reasons.domain;

import com.docsshare_web_backend.reasons.dto.requests.ReasonRequest;
import com.docsshare_web_backend.reasons.dto.responses.ReasonResponse;
import com.docsshare_web_backend.reasons.services.ReasonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reasons")
public class ReasonController {
    @Autowired
    private ReasonService reasonService;

    @GetMapping
    public ResponseEntity<List<ReasonResponse>> getAllReasons() {
        return ResponseEntity.ok(reasonService.getAllReasons());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReasonResponse> getReasonById(@PathVariable long id) {
        return ResponseEntity.ok(reasonService.getReasonById(id));
    }

    @PostMapping("/create")
    public ResponseEntity<ReasonResponse> createReason(@RequestBody ReasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reasonService.createReason(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReasonResponse> updateReason(
            @PathVariable long id,
            @RequestBody ReasonRequest request) {
        return ResponseEntity.ok(reasonService.updateReason(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteReason(@PathVariable long id) {
        reasonService.deleteReason(id);
        return ResponseEntity.ok("Xóa lý do thành công với ID: " + id);
    }
}
