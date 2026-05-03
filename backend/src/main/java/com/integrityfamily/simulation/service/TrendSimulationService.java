package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrendSimulationService {

    private final FamilyRepository familyRepository;
    private final AdminAlertRepository alertRepository;

    /**
     * Provoca un fallo masivo de hitos (Estancamiento Colectivo).
     * Simula que el 70% de las familias han fallado en su evaluaciÃƒÂ³n de progreso.
     */
    @Transactional
    public String triggerMassiveMilestoneFailure() {
        log.warn("Ã°Å¸Å¡Â¨ [SIMULATION-TREND] Iniciando Fallo Masivo de Hitos...");

        List<Family> families = familyRepository.findAll();
        int affectedCount = 0;

        for (int i = 0; i < families.size(); i++) {
            // Afectamos al grueso de la muestra (70%)
            if (i % 3 != 0) {
                Family f = families.get(i);
                // Simulamos estancamiento en el Hito Inicial o caÃƒÂ­da de ICF
                f.setCurrentMilestone("MES_00_STALLED"); 
                familyRepository.save(f);
                affectedCount++;
            }
        }

        // Generar Alerta de TENDENCIA CRÃƒÂTICA
        AdminAlert trendAlert = AdminAlert.builder()
                .title("ANOMALÃƒÂA SISTÃƒâ€°MICA: Estancamiento Masivo")
                .message("Se ha detectado que " + affectedCount + " de " + families.size() + 
                         " nÃƒÂºcleos familiares han fallado en la transiciÃƒÂ³n al siguiente hito pedagÃƒÂ³gico. Posible resistencia en el diseÃƒÂ±o de misiones.")
                .severity("CRITICAL")
                .viewed(false)
                .build();
        
        alertRepository.save(trendAlert);

        log.error("Ã°Å¸Å¡Â© [WATCHDOG] Tendencia CrÃƒÂ­tica inyectada: {} familias estancadas.", affectedCount);
        return "Fallo masivo activado en " + affectedCount + " familias. Verifica el impacto en el Global Stats.";
    }
}


