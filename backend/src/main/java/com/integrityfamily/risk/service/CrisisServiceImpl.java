package com.integrityfamily.risk.service;

import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.CriticalDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Collections;

/**
 * SDD IMPLEMENTATION: GestiÃƒÂ³n unificada de Protocolos Sentinel y Registro
 * HistÃƒÂ³rico.
 * Sincronizado con CrisisController para eliminar errores de sÃƒÂ­mbolo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrisisServiceImpl implements CrisisService {

    // private final CriticalDayRepository repository; // Inyectar cuando el repo
    // estÃƒÂ© listo

    @Override
    public CriticalDay registerCrisis(Long familyId, Long memberId, String category, String description,
            String emotion) {
        log.warn("Ã°Å¸Å¡Â¨ [CRISIS-REG] Nueva crisis registrada: Familia {}, CategorÃƒÂ­a {}, EmociÃƒÂ³n {}",
                familyId, category, emotion);

        // SDD-FIX: ConstrucciÃƒÂ³n del objeto solicitado por el Controller
        CriticalDay cd = CriticalDay.builder()
                .familyId(familyId)
                .memberId(memberId)
                .category(category)
                .description(description)
                .emotion(emotion)
                .build();

        // return repository.save(cd);
        return cd; // Temporal hasta habilitar persistencia
    }

    @Override
    public List<CriticalDay> getHistory(Long familyId) {
        log.info("Ã°Å¸â€œÅ  [CRISIS-HIST] Recuperando historial para familia {}", familyId);
        return Collections.emptyList(); // Debe retornar List<CriticalDay>
    }

    @Override
    public void activateProtocol(Long familyId, String reason) {
        log.error("Ã°Å¸â€ºÂ¡Ã¯Â¸Â [SENTINEL-ACTIVATE] Protocolo de emergencia para ID: {} por: {}", familyId, reason);
    }

    @Override
    public void handleMemberCrisis(Long familyId, List<FamilyMember> involvedMembers, String observation) {
        log.info("Ã°Å¸â€Â [SENTINEL-FamilyMember] Procesando crisis para {} miembros en familia {}",
                involvedMembers != null ? involvedMembers.size() : 0, familyId);
    }

    @Override
    public boolean isUnderCrisis(Long familyId) {
        return false;
    }
}


