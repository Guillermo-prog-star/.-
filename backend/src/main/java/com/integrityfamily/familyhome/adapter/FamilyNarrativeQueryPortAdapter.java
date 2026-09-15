package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.domain.FamilyDocumentary;
import com.integrityfamily.domain.repository.FamilyDocumentaryRepository;
import com.integrityfamily.dto.home.GeneratorType;
import com.integrityfamily.dto.home.ReviewStatus;
import com.integrityfamily.familyhome.port.FamilyNarrativeQueryPort;
import com.integrityfamily.familyhome.port.NarrativeCandidate;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * FamilyDocumentary no registra metadatos de procedencia IA (generatorType/promptVersion/
 * reviewStatus); se reportan como generados por motor de reglas y auto-aprobados hasta que
 * el módulo documentary persista esa procedencia.
 */
@Component
public class FamilyNarrativeQueryPortAdapter implements FamilyNarrativeQueryPort {

    private final FamilyDocumentaryRepository documentaryRepository;
    private final FamilyIdentifierBridge idBridge;

    public FamilyNarrativeQueryPortAdapter(FamilyDocumentaryRepository documentaryRepository, FamilyIdentifierBridge idBridge) {
        this.documentaryRepository = documentaryRepository;
        this.idBridge = idBridge;
    }

    @Override
    public NarrativeCandidate getTodayNarrative(UUID familyId) {
        Optional<Long> fId = idBridge.resolveFamilyId(familyId);
        if (fId.isEmpty()) {
            return null;
        }

        FamilyDocumentary today = documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(fId.get()).stream()
                .filter(d -> d.getCreatedAt() != null && d.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .findFirst()
                .orElse(null);
        if (today == null) {
            return null;
        }

        return new NarrativeCandidate(
                today.getContent(),
                GeneratorType.RULE_ENGINE,
                "documentary-" + today.getId(),
                null,
                List.of(String.valueOf(today.getId())),
                ReviewStatus.AUTO_APPROVED,
                "FAMILY_APPROVED",
                today.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                null,
                "es",
                false
        );
    }
}
