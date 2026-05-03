package com.integrityfamily.analytics.controller;

import com.integrityfamily.analytics.service.AdminAnalyticsService;
import com.integrityfamily.analytics.service.SentimentAnalyticsService;
import com.integrityfamily.common.service.BackupService;
import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.admin.service.WatchdogIntegrityService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final SentimentAnalyticsService sentimentAnalyticsService;
    private final BackupService backupService;
    private final AdminAlertRepository adminAlertRepository;
    private final WatchdogIntegrityService watchdogIntegrityService;

    @GetMapping("/alpha-stats")
    public ApiResponse<AdminAnalyticsService.GlobalStats> getAlphaStats() {
        return ApiResponse.ok(adminAnalyticsService.getAlphaPhaseStats());
    }

    @GetMapping("/sentiment")
    public ApiResponse<SentimentAnalyticsService.SentimentReport> getSentimentAnalysis() {
        return ApiResponse.ok(sentimentAnalyticsService.analyzeGlobalFeedback());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<AdminAlert>> getAlerts() {
        return ApiResponse.ok(adminAlertRepository.findAllByOrderByCreatedAtDesc());
    }

    @PostMapping("/watchdog/test")
    public ApiResponse<String> testWatchdog() {
        return ApiResponse.ok(watchdogIntegrityService.testWatchdogActivation());
    }

    @PostMapping("/backup")
    public ApiResponse<String> triggerBackup() throws IOException {
        String path = backupService.performSecurityBackup();
        return ApiResponse.ok("Backup de Seguridad generado en: " + path);
    }
}


