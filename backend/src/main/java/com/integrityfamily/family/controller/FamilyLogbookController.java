package com.integrityfamily.family.controller;

import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.dto.CreateFamilyLogbookEntryRequest;
import com.integrityfamily.dto.FamilyLogbookEntryResponse;
import com.integrityfamily.dto.ResolveFamilyLogbookEntryRequest;
import com.integrityfamily.family.service.FamilyLogbookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-logbook")
public class FamilyLogbookController {

    private final FamilyLogbookService service;

    public FamilyLogbookController(FamilyLogbookService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyLogbookEntryResponse create(
            @Valid @RequestBody CreateFamilyLogbookEntryRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/family/{familyId}")
    public List<FamilyLogbookEntryResponse> findByFamily(
            @PathVariable Long familyId
    ) {
        return service.findByFamily(familyId);
    }

    @GetMapping("/family/{familyId}/status/{status}")
    public List<FamilyLogbookEntryResponse> findByFamilyAndStatus(
            @PathVariable Long familyId,
            @PathVariable LogbookStatus status
    ) {
        return service.findByFamilyAndStatus(familyId, status);
    }

    @GetMapping("/{id}")
    public FamilyLogbookEntryResponse findById(
            @PathVariable Long id
    ) {
        return service.findById(id);
    }

    @PutMapping("/{id}/resolve")
    public FamilyLogbookEntryResponse resolve(
            @PathVariable Long id,
            @Valid @RequestBody ResolveFamilyLogbookEntryRequest request
    ) {
        return service.resolve(id, request);
    }
}
