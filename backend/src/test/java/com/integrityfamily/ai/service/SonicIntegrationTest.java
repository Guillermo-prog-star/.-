package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.VoiceChatResponse;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Base64;

/**
 * SDD-SONIC-TEST: Integration test for real STT/TTS flow.
 * Movidp a src/test y actualizado para el nuevo DTO VoiceChatResponse.
 */
@Configuration
@Slf4j
@Profile("sonic-test")
public class SonicIntegrationTest {

    @Bean
    CommandLineRunner testSonicFlow(SonicService sonicService, FamilyRepository familyRepository) {
        return args -> {
            log.info("ðŸ§ª [SONIC-TEST] Iniciando prueba de integraciÃ³n de audio real...");
            
            // 1. Obtener familia de prueba
            Family family = familyRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay familias para la prueba"));

            // 2. Cargar audio sintÃ©tico (Silencio MP3 en Base64)
            byte[] audioBytes = Base64.getDecoder().decode("SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA");

            try {
                log.info("ðŸ§ª [SONIC-TEST] Enviando payload de audio a SonicService...");
                // Actualizado para usar el nuevo DTO y la firma del servicio
                com.integrityfamily.ai.dto.SonicResponse response = sonicService.processVoiceChat(audioBytes, "audio/mpeg", family);
                
                log.info("ðŸ§ª [SONIC-TEST] RESULTADO STT: \"{}\"", response.transcript());
                log.info("ðŸ§ª [SONIC-TEST] RESULTADO AI: \"{}\"", response.assistantReply());
                
                log.info("ðŸ§ª [SONIC-TEST] Prueba finalizada exitosamente.");
            } catch (Exception e) {
                log.error("ðŸ§ª [SONIC-TEST] Fallo en la prueba de integraciÃ³n", e);
            }
        };
    }
}

