package com.integrityfamily.checklist.controller;

import com.integrityfamily.domain.ChecklistItem;
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

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<ChecklistItem>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(checklistService.getFamilyChecklist(familyId));
    }

    @PostMapping("/family/{familyId}")
    public ApiResponse<ChecklistItem> createItem(@PathVariable Long familyId, @RequestBody Map<String, String> body) {
        ChecklistItem item = checklistService.createChecklistItem(
            familyId,
            body.get("description"),
            body.get("dimension"),
            body.get("source")
        );
        return ApiResponse.ok(item);
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String who = body.get("completedBy");
        checklistService.markAsCompleted(id, who);
        return ApiResponse.ok(null);
    }
}


