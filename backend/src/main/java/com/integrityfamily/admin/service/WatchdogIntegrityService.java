package com.integrityfamily.admin.service;

import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WatchdogIntegrityService {

    private final FamilyRepository familyRepository;
    private final SecurityWatchdogService watchdogService;
    private final AdminAlertRepository alertRepository;

    /**
     * Prueba de fuego del Modo de Vigilancia.
     * Simula una crisis en el primer Nodo Alfa y verifica la generaciÃƒÂ³n de alertas.
     */
    @Transactional
    public String testWatchdogActivation() {
        log.info("Ã°Å¸â€ºÂ¡Ã¯Â¸Â [WATCHDOG-TEST] Iniciando prueba de integridad de vigilancia...");

        // 1. Encontrar un objetivo Alfa (IF-CO Pattern)
        Family target = familyRepository.findAll().stream()
                .filter(f -> f.getFamilyCode() != null && f.getFamilyCode().startsWith("IF-CO-"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No hay familias Alfa para probar."));

        // 2. Forzar Crisis Sentinel
        target.setSentinelActive(true);
        familyRepository.save(target);
        log.info("Ã°Å¸Å¡Â¨ [WATCHDOG-TEST] Crisis simulada en: {}", target.getFamilyCode());

        // 3. Ejecutar escaneo manualmente para la prueba
        watchdogService.scanForAnomalies();

        // 4. Verificar alerta
        boolean alertCreated = alertRepository.findAllByOrderByCreatedAtDesc().stream()
                .anyMatch(a -> a.getTitle().contains(target.getFamilyCode()));

        if (alertCreated) {
            log.info("Ã¢Å“â€¦ [WATCHDOG-TEST] Alerta detectada y registrada exitosamente.");
            return "Vigilancia Activa: Alerta de crisis generada para " + target.getFamilyCode();
        } else {
            return "Error: El watchdog no generÃƒÂ³ la alerta esperada.";
        }
    }
}


