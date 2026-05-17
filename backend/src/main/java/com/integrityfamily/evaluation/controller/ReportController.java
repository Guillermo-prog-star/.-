package com.integrityfamily.evaluation.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.dto.TerritorialEvolutionReportDto;
import com.integrityfamily.evaluation.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController("evaluationReportController")
@RequestMapping("/api/families/{id}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/territorial")
    public ApiResponse<TerritorialEvolutionReportDto> getTerritorialReport(@PathVariable Long id) {
        return ApiResponse.ok(reportService.getTerritorialReport(id));
    }
}
