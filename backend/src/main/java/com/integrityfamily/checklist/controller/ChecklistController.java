package com.integrityfamily.checklist.controller;

import com.integrityfamily.checklist.domain.ChecklistItem;
import com.integrityfamily.checklist.service.ChecklistService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/checklist")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    public ApiResponse<List<ChecklistItem>> getAll() {
        return ApiResponse.ok(checklistService.findAll());
    }

    /** Endpoint que usa el frontend: GET /api/checklist/family/{familyId} */
    @GetMapping("/family/{familyId}")
    public ApiResponse<List<ChecklistItem>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(checklistService.findByFamilyId(familyId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ChecklistItem> getById(@PathVariable Long id) {
        return ApiResponse.ok(checklistService.findById(id));
    }

    /** Endpoint que usa el frontend: POST /api/checklist/items */
    @PostMapping("/items")
    public ApiResponse<ChecklistItem> createItem(@RequestBody ChecklistItem item) {
        return ApiResponse.ok(checklistService.create(item));
    }

    @PostMapping
    public ApiResponse<ChecklistItem> create(@RequestBody ChecklistItem item) {
        return ApiResponse.ok(checklistService.create(item));
    }

    /** Endpoint que usa el frontend: PUT /api/checklist/items/{id}/complete */
    @PutMapping("/items/{id}/complete")
    public ApiResponse<ChecklistItem> completeItem(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean completed = Boolean.TRUE.equals(body.get("completed"));
        return ApiResponse.ok(checklistService.completeItem(id, completed));
    }

    @PutMapping("/{id}")
    public ApiResponse<ChecklistItem> update(@PathVariable Long id, @RequestBody ChecklistItem item) {
        return ApiResponse.ok(checklistService.update(id, item));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        checklistService.delete(id);
        return ApiResponse.ok(null);
    }

    /** Endpoint que usa el frontend: POST /api/checklist/generate-from-plan */
    @PostMapping("/generate-from-plan")
    public ApiResponse<List<ChecklistItem>> generateFromPlan(@RequestBody Map<String, Long> body) {
        Long planId = body.get("planId");
        return ApiResponse.ok(checklistService.generateFromPlan(planId));
    }
}