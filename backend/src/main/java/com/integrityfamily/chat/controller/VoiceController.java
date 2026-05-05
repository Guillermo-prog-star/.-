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
            @PathVariable("familyId") Long familyId) throws IOException {

        SonicService sonicService = sonicServiceProvider.getIfAvailable();
        if (sonicService == null) {
            return ResponseEntity.status(503).build(); // feature deshabilitado
        }
        
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new NotFoundException("Familia no encontrada"));
        
        // Sincronizado para devolver SonicResponse (texto) segÃƒÂºn el nuevo contrato
        SonicResponse response = sonicService.processVoiceChat(
                audio.getBytes(), audio.getContentType(), family);
                
        return ResponseEntity.ok(response);
    }
}


