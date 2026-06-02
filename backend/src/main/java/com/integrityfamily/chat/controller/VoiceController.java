package com.integrityfamily.chat.controller;

import com.integrityfamily.ai.dto.SonicResponse;
import com.integrityfamily.ai.service.SonicService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.common.exception.NotFoundException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/chat/voice")
public class VoiceController {

    private final ObjectProvider<SonicService> sonicServiceProvider;
    private final FamilyRepository familyRepository;

    public VoiceController(ObjectProvider<SonicService> sonicServiceProvider,
                           FamilyRepository familyRepository) {
        this.sonicServiceProvider = sonicServiceProvider;
        this.familyRepository = familyRepository;
    }

    @PostMapping(path = "/{familyId}", consumes = "multipart/form-data")
    public ResponseEntity<SonicResponse> chat(
            @RequestParam("audio") MultipartFile audio,
            @PathVariable("familyId") Long familyId,
            @RequestParam(value = "memberId", required = false) Long memberId,
            @RequestParam(value = "clientTranscript", required = false) String clientTranscript,
            @RequestParam(value = "currentPillar", required = false) String currentPillar,
            @RequestParam(value = "currentMonth", required = false) Integer currentMonth,
            @RequestParam(value = "milestoneLabel", required = false) String milestoneLabel,
            @RequestParam(value = "currentPhase", required = false) String currentPhase,
            @RequestParam(value = "sprintNumber", required = false) Integer sprintNumber,
            @RequestParam(value = "activeMissionId", required = false) String activeMissionId,
            @RequestParam(value = "progressPercent", required = false) Integer progressPercent,
            @RequestParam(value = "onboardingCompleted", required = false) Boolean onboardingCompleted) throws IOException {

        SonicService sonicService = sonicServiceProvider.getIfAvailable();
        if (sonicService == null) {
            return ResponseEntity.status(503).build();
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));

        ChatController.ChatRequestV2.TransformationContextDto context = null;
        if (currentPillar != null || currentMonth != null || milestoneLabel != null || currentPhase != null ||
            sprintNumber != null || activeMissionId != null || progressPercent != null || onboardingCompleted != null) {
            context = new ChatController.ChatRequestV2.TransformationContextDto();
            context.setCurrentPillar(currentPillar);
            context.setCurrentMonth(currentMonth);
            context.setMilestoneLabel(milestoneLabel);
            context.setCurrentPhase(currentPhase);
            context.setSprintNumber(sprintNumber);
            context.setActiveMissionId(activeMissionId);
            context.setProgressPercent(progressPercent);
            context.setOnboardingCompleted(onboardingCompleted);
        }

        SonicResponse response = sonicService.processVoiceChat(
                audio.getBytes(), audio.getContentType(), family, memberId, context, clientTranscript);

        return ResponseEntity.ok(response);
    }
}


