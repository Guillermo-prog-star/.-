package com.integrityfamily.chat.controller;

import com.integrityfamily.ai.dto.SonicResponse;
import com.integrityfamily.ai.service.SonicService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.family.service.FamilyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final ObjectProvider<SonicService> sonicServiceProvider;
    private final FamilyService familyService;

    public VoiceController(ObjectProvider<SonicService> sonicServiceProvider,
                           FamilyService familyService) {
        this.sonicServiceProvider = sonicServiceProvider;
        this.familyService = familyService;
    }

    @PostMapping(path = "/chat", consumes = "multipart/form-data")
    public ResponseEntity<SonicResponse> chat(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("familyId") Long familyId) throws IOException {

        SonicService sonicService = sonicServiceProvider.getIfAvailable();
        if (sonicService == null) {
            return ResponseEntity.status(503).build(); // feature deshabilitado
        }
        
        Family family = familyService.findById(familyId);
        
        // Sincronizado para devolver SonicResponse (texto) segÃƒÂºn el nuevo contrato
        SonicResponse response = sonicService.processVoiceChat(
                audio.getBytes(), audio.getContentType(), family);
                
        return ResponseEntity.ok(response);
    }
}


