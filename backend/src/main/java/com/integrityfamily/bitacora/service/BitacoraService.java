package com.integrityfamily.bitacora.service;

import com.integrityfamily.bitacora.dto.BitacoraRequest;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.repository.FamilyLogbookRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SDD: Servicio de Bitácora Cognitiva.
 * Gestiona el registro de aprendizajes y acuerdos familiares.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final FamilyLogbookRepository repository;
    private final FamilyRepository familyRepository;

    @Transactional
    public FamilyLogbookEntry createEntry(BitacoraRequest req) {
        log.info("[BITACORA] Registrando hito cognitivo para familia: {}", req.familyId());
        
        FamilyLogbookEntry entry = new FamilyLogbookEntry();
        entry.setFamily(familyRepository.findById(req.familyId())
                .orElseThrow(() -> new RuntimeException("Familia no encontrada")));
        
        // SDD-MAPPING: Mapeo de Insight de IA a campos de Bitácora
        entry.setSituation("Evento: " + req.relatedEntity() + " ID: " + req.relatedId());
        entry.setDifficultyDetected("Hipótesis: " + req.hypothesis());
        entry.setEmotionIdentified("Expectativa Evolutiva");
        entry.setUnderstanding("Análisis IA: " + req.learning());
        entry.setCorrectionAction(req.action());
        entry.setFamilyAgreement(req.learning());
        entry.setCreatedBy("IA-MOTOR");
        
        entry.setStatus(com.integrityfamily.domain.LogbookStatus.OPEN);
        
        return repository.save(entry);
    }
}
