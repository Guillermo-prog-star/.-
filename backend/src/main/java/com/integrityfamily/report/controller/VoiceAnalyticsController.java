package com.integrityfamily.report.controller;

import com.integrityfamily.report.service.VoiceAnalyticsService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * SDD-VOICE-ANALYTICS-01: Admin endpoints for voice monitoring.
 * Secured with ROLE_ADMIN to protect institutional analytics.
 */
@RestController
@RequestMapping("/api/admin/voice")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class VoiceAnalyticsController {

    private final VoiceAnalyticsService analyticsService;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.ok(analyticsService.getSummaryStats());
    }

    @GetMapping("/recent")
    public ApiResponse<List<Map<String, Object>>> getRecent() {
        return ApiResponse.ok(analyticsService.getRecentInteractions());
    }

    @GetMapping("/regional")
    public ApiResponse<List<Map<String, Object>>> getRegional() {
        return ApiResponse.ok(analyticsService.getRegionalStats());
    }
}


