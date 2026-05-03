package com.integrityfamily.milestone.controller;

import com.integrityfamily.domain.Milestone;
import com.integrityfamily.milestone.service.MilestoneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @GetMapping
    public List<Milestone> getAll() {
        return milestoneService.findAll();
    }

    @GetMapping("/{id}")
    public Milestone getById(@PathVariable Long id) {
        return milestoneService.findById(id);
    }

    @PostMapping
    public Milestone create(@RequestBody Milestone milestone) {
        return milestoneService.create(milestone);
    }

    @PutMapping("/{id}")
    public Milestone update(@PathVariable Long id, @RequestBody Milestone milestone) {
        return milestoneService.update(id, milestone);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        milestoneService.delete(id);
    }

    @GetMapping("/family/{familyId}/current")
    public String getCurrentMilestoneLabel(@PathVariable Long familyId) {
        return milestoneService.getCurrentMilestoneLabel(familyId);
    }

    @GetMapping("/family/{familyId}/check-advance")
    public boolean canAdvance(@PathVariable Long familyId) {
        return milestoneService.canAdvance(familyId);
    }

    @PostMapping("/family/{familyId}/advance")
    public String advanceMilestone(@PathVariable Long familyId) {
        return milestoneService.advanceMilestone(familyId);
    }
}


