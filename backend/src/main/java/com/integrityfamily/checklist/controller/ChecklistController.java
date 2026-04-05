package com.integrityfamily.checklist.controller;

import com.integrityfamily.checklist.domain.ChecklistItem;
import com.integrityfamily.checklist.service.ChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/checklist")
@RequiredArgsConstructor
public class ChecklistController {

    private final ChecklistService checklistService;

    @GetMapping
    public List<ChecklistItem> getAll() {
        return checklistService.findAll();
    }

    @GetMapping("/{id}")
    public ChecklistItem getById(@PathVariable Long id) {
        return checklistService.findById(id);
    }

    @PostMapping
    public ChecklistItem create(@RequestBody ChecklistItem item) {
        return checklistService.create(item);
    }

    @PutMapping("/{id}")
    public ChecklistItem update(@PathVariable Long id, @RequestBody ChecklistItem item) {
        return checklistService.update(id, item);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        checklistService.delete(id);
    }
}