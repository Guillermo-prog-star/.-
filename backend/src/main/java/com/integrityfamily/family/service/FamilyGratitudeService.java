package com.integrityfamily.family.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyGratitudeEntry;
import com.integrityfamily.dto.CreateFamilyGratitudeRequest;
import com.integrityfamily.dto.FamilyGratitudeResponse;
import com.integrityfamily.domain.repository.FamilyGratitudeEntryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FamilyGratitudeService {

    private final FamilyRepository familyRepository;
    private final FamilyGratitudeEntryRepository gratitudeRepository;

    public FamilyGratitudeService(
            FamilyRepository familyRepository,
            FamilyGratitudeEntryRepository gratitudeRepository
    ) {
        this.familyRepository = familyRepository;
        this.gratitudeRepository = gratitudeRepository;
    }

    @Transactional
    public FamilyGratitudeResponse create(CreateFamilyGratitudeRequest request) {
        log.info("[GRATITUD] Registrando nota de agradecimiento de {} para {} en familia ID: {}", 
                request.fromMember(), request.toMember(), request.familyId());

        Family family = familyRepository.findById(request.familyId())
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + request.familyId()));

        FamilyGratitudeEntry entry = new FamilyGratitudeEntry(
                family,
                request.fromMember(),
                request.toMember(),
                request.description()
        );

        FamilyGratitudeEntry saved = gratitudeRepository.save(entry);
        return FamilyGratitudeResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FamilyGratitudeResponse> findByFamily(Long familyId) {
        log.info("[GRATITUD] Buscando agradecimientos para familia ID: {}", familyId);
        return gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(FamilyGratitudeResponse::from)
                .toList();
    }
}
