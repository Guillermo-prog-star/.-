package com.integrityfamily.weeklyplan.controller;

import com.integrityfamily.weeklyplan.domain.WeeklyPlan;
import com.integrityfamily.weeklyplan.service.WeeklyPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * WeeklyPlanController — API REST de planeación semanal familiar.
 *
 * GET  /api/families/{familyId}/weekly-plans          → todos los planes del sprint activo
 * PUT  /api/families/{familyId}/weekly-plans/{phase}  → guardar fase (upsert)
 */
@RestController
@RequestMapping("/api/families/{familyId}/weekly-plans")
@RequiredArgsConstructor
public class WeeklyPlanController {

    private final WeeklyPlanService service;

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping
    public ResponseEntity<List<WeeklyPlan>> getAll(@PathVariable Long familyId) {
        return ResponseEntity.ok(service.getAll(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PutMapping("/{phase}")
    public ResponseEntity<WeeklyPlan> save(
            @PathVariable Long familyId,
            @PathVariable String phase,
            @RequestBody Map<String, Object> body) {
        body.put("phase", phase);
        return ResponseEntity.ok(service.save(familyId, body));
    }
}
