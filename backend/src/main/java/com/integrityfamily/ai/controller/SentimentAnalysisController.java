package com.integrityfamily.ai.controller;

import com.integrityfamily.ai.dto.LogbookCorrelationResult;
import com.integrityfamily.ai.service.SentimentAnalysisService;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/sentiment")
@RequiredArgsConstructor
public class SentimentAnalysisController {

    private final SentimentAnalysisService sentimentAnalysisService;

    @GetMapping("/family/{familyId}/correlation")
    public ApiResponse<LogbookCorrelationResult> getFamilyLogbookCorrelation(@PathVariable Long familyId) {
        log.info("🎯 [SENTIMENT-CONTROLLER] Petición de correlación de bitácora recibida para Familia ID: {}", familyId);
        LogbookCorrelationResult result = sentimentAnalysisService.correlateFamilySentiment(familyId);
        return ApiResponse.ok(result);
    }
}
