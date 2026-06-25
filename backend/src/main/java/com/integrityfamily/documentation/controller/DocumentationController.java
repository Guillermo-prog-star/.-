package com.integrityfamily.documentation.controller;

import com.integrityfamily.documentation.domain.DocumentCategory;
import com.integrityfamily.documentation.dto.DocumentationDtos.*;
import com.integrityfamily.documentation.service.DocumentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documentation")
@RequiredArgsConstructor
public class DocumentationController {

    private final DocumentationService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentListResponse> listAll() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentListResponse> listByCategory(@PathVariable DocumentCategory category) {
        return ResponseEntity.ok(service.listByCategory(category));
    }

    @GetMapping("/{code}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentDetailResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(service.getByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentListResponse> search(@RequestParam String q) {
        return ResponseEntity.ok(service.search(q));
    }

    @PostMapping("/query")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<QueryResponse> query(@RequestBody QueryRequest request) {
        return ResponseEntity.ok(service.query(request.getQuestion()));
    }
}
