package com.integrityfamily.documentary.controller;

import com.integrityfamily.documentary.dto.SubmitDocumentaryRequest;
import com.integrityfamily.documentary.service.DocumentaryService;
import com.integrityfamily.domain.FamilyDocumentary;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documentaries")
@RequiredArgsConstructor
public class DocumentaryController {

    private final DocumentaryService documentaryService;

    @PostMapping
    public ResponseEntity<ApiResponse<FamilyDocumentary>> createDocumentary(@RequestBody SubmitDocumentaryRequest request) {
        FamilyDocumentary doc = documentaryService.createDocumentary(request);
        return ResponseEntity.ok(ApiResponse.ok(doc, "Documental creado exitosamente"));
    }

    @GetMapping("/family/{familyId}")
    public ResponseEntity<ApiResponse<java.util.List<com.integrityfamily.documentary.dto.FamilyDocumentaryDTO>>> getFamilyDocumentaries(@PathVariable Long familyId) {
        java.util.List<com.integrityfamily.documentary.dto.FamilyDocumentaryDTO> docs = documentaryService.getFamilyDocumentaries(familyId);
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }
}
