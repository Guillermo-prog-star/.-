package com.integrityfamily.legado.service;

import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.domain.FamilyValue;
import com.integrityfamily.legado.dto.LegacyRequest;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.legado.repository.FamilyValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegacyService {

    private final FamilyLegacyRepository legacyRepo;
    private final FamilyValueRepository  valueRepo;

    /**
     * Retorna el legado de la familia o crea uno vacío si no existe.
     */
    public FamilyLegacy getOrCreate(Long familyId) {
        return legacyRepo.findByFamilyId(familyId)
                .orElseGet(() -> legacyRepo.save(
                        FamilyLegacy.builder().familyId(familyId).build()
                ));
    }

    public List<FamilyValue> getValues(Long familyId) {
        return valueRepo.findByFamilyIdOrderBySortOrder(familyId);
    }

    /**
     * Guarda el legado completo (upsert): historia, constitución, misión/visión, carta y valores.
     */
    @Transactional
    public FamilyLegacy save(Long familyId, LegacyRequest req) {
        FamilyLegacy legacy = legacyRepo.findByFamilyId(familyId)
                .orElseGet(() -> FamilyLegacy.builder().familyId(familyId).build());

        // Historia
        legacy.setHistoryLessons(req.getHistoryLessons());
        legacy.setHistoryConserve(req.getHistoryConserve());
        legacy.setHistoryAvoidErrors(req.getHistoryAvoidErrors());
        legacy.setHistoryToLeave(req.getHistoryToLeave());
        legacy.setHistoryRecognition(req.getHistoryRecognition());
        // Constitución
        legacy.setConstitutionFamilyName(req.getConstitutionFamilyName());
        legacy.setConstitutionYear(req.getConstitutionYear());
        legacy.setFoundingPrinciple(req.getFoundingPrinciple());
        legacy.setCommitments(req.getCommitments());
        legacy.setNeverDo(req.getNeverDo());
        legacy.setConflictResolution(req.getConflictResolution());
        // Misión & Visión
        legacy.setFamilyMission(req.getFamilyMission());
        legacy.setFamilyVision(req.getFamilyVision());
        legacy.setFamilyTagline(req.getFamilyTagline());
        // Carta
        legacy.setLetterFrom(req.getLetterFrom());
        legacy.setLetterTo(req.getLetterTo());
        legacy.setLetterOpenInYear(req.getLetterOpenInYear());
        legacy.setLetterSealed(req.isLetterSealed());
        if (!legacy.isLetterSealed()) {          // carta sellada no se sobreescribe
            legacy.setLetterContent(req.getLetterContent());
        }

        legacyRepo.save(legacy);

        // Valores: reemplazar todos
        if (req.getValues() != null) {
            valueRepo.deleteByFamilyId(familyId);
            List<FamilyValue> values = req.getValues().stream().map(v ->
                FamilyValue.builder()
                    .familyId(familyId)
                    .icon(v.getIcon())
                    .name(v.getName())
                    .description(v.getDescription())
                    .sortOrder(v.getSortOrder())
                    .build()
            ).collect(Collectors.toList());
            valueRepo.saveAll(values);
        }

        return legacy;
    }
}
