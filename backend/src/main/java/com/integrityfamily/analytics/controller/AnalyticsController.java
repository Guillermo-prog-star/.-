package com.integrityfamily.analytics.controller;

import com.integrityfamily.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardSummary() {
        return analyticsService.getDashboardSummary();
    }

    @GetMapping("/family/{familyId}")
    public Map<String, Object> getFamilySummary(@PathVariable Long familyId) {
        return analyticsService.getFamilySummary(familyId);
    }
}