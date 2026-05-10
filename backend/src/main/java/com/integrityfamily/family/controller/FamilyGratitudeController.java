package com.integrityfamily.family.controller;

import com.integrityfamily.dto.CreateFamilyGratitudeRequest;
import com.integrityfamily.dto.FamilyGratitudeResponse;
import com.integrityfamily.family.service.FamilyGratitudeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-gratitude")
public class FamilyGratitudeController {

    private final FamilyGratitudeService service;

    public FamilyGratitudeController(FamilyGratitudeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyGratitudeResponse create(
            @Valid @RequestBody CreateFamilyGratitudeRequest request
    ) {
        return service.create(request);
    }

    @GetMapping("/family/{familyId}")
    public List<FamilyGratitudeResponse> findByFamily(
            @PathVariable Long familyId
    ) {
        return service.findByFamily(familyId);
    }
}
