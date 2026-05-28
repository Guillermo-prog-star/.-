package com.integrityfamily.participation.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.participation.dto.ParticipationPulseResponse;
import com.integrityfamily.participation.service.ParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/families/{familyId}/participation")
@RequiredArgsConstructor
public class ParticipationController {

    private final ParticipationService participationService;

    @GetMapping("/pulse")
    public ApiResponse<ParticipationPulseResponse> getPulse(@PathVariable Long familyId) {
        return ApiResponse.ok(participationService.getPulse(familyId));
    }
}
