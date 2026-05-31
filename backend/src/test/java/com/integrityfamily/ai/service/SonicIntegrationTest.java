package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.SonicResponse;
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
 * Solo activo bajo el perfil "sonic-test" — no corre en CI.
 *
 * Actualizado para firma actual:
 *   processVoiceChat(audioBytes, mimeType, family, memberId)
 */
@Configuration
@Slf4j
@Profile("sonic-test")
public class SonicIntegrationTest {

    @Bean
    CommandLineRunner testSonicFlow(SonicService sonicService, FamilyRepository familyRepository) {
        return args -> {
            log.info("🧪 [SONIC-TEST] Iniciando prueba de integración de audio real...");

            Family family = familyRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay familias para la prueba"));

            // Silencio MP3 mínimo en Base64
            byte[] audioBytes = Base64.getDecoder().decode(
                    "SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA");

            try {
                log.info("🧪 [SONIC-TEST] Enviando payload de audio a SonicService...");
                // memberId = null para prueba sin miembro específico
                SonicResponse response = sonicService.processVoiceChat(
                        audioBytes, "audio/mpeg", family, null);

                log.info("🧪 [SONIC-TEST] RESULTADO STT: \"{}\"", response.transcript());
                log.info("🧪 [SONIC-TEST] RESULTADO AI: \"{}\"", response.assistantReply());
                log.info("🧪 [SONIC-TEST] Prueba finalizada exitosamente.");
            } catch (Exception e) {
                log.error("🧪 [SONIC-TEST] Fallo en la prueba de integración", e);
            }
        };
    }
}
