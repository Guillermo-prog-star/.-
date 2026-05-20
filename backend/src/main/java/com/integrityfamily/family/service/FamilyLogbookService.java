package com.integrityfamily.family.service;

import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLogbookEntry;
import com.integrityfamily.domain.LogbookStatus;
import com.integrityfamily.dto.CreateFamilyLogbookEntryRequest;
import com.integrityfamily.dto.FamilyLogbookEntryResponse;
import com.integrityfamily.dto.ResolveFamilyLogbookEntryRequest;
import com.integrityfamily.domain.repository.FamilyLogbookEntryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SDD: Servicio de Bitácora de Transformación Familiar.
 * Integrado con FamilyMemoryService para capturar situaciones y resoluciones
 * como memorias episódicas del sistema cognitivo.
 */
@Slf4j
@Service
public class FamilyLogbookService {

    private final FamilyRepository familyRepository;
    private final FamilyLogbookEntryRepository logbookRepository;
    private final FamilyMemoryService familyMemoryService;

    public FamilyLogbookService(
            FamilyRepository familyRepository,
            FamilyLogbookEntryRepository logbookRepository,
            FamilyMemoryService familyMemoryService
    ) {
        this.familyRepository = familyRepository;
        this.logbookRepository = logbookRepository;
        this.familyMemoryService = familyMemoryService;
    }

    @Transactional
    public FamilyLogbookEntryResponse create(CreateFamilyLogbookEntryRequest request) {
        Family family = familyRepository.findById(request.familyId())
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + request.familyId()));

        FamilyLogbookEntry entry = new FamilyLogbookEntry(
                family,
                request.situation(),
                request.difficultyDetected(),
                request.emotionIdentified(),
                request.understanding(),
                request.correctionAction(),
                request.familyAgreement(),
                request.createdBy()
        );

        FamilyLogbookEntry saved = logbookRepository.save(entry);
        try {
            familyMemoryService.captureLogbookOpenMemory(saved);
        } catch (Exception e) {
            log.warn("⚠️ [LOGBOOK] Error capturando memoria abierta: {}", e.getMessage());
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FamilyLogbookEntryResponse> findByFamily(Long familyId) {
        return logbookRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FamilyLogbookEntryResponse> findByFamilyAndStatus(Long familyId, LogbookStatus status) {
        return logbookRepository.findByFamilyIdAndStatusOrderByCreatedAtDesc(familyId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FamilyLogbookEntryResponse findById(Long id) {
        FamilyLogbookEntry entry = logbookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entrada de bitácora no encontrada: " + id));

        return toResponse(entry);
    }

    @Transactional
    public FamilyLogbookEntryResponse resolve(Long id, ResolveFamilyLogbookEntryRequest request) {
        FamilyLogbookEntry entry = logbookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entrada de bitácora no encontrada: " + id));

        entry.resolve(request.progressEvidence(), request.resolvedBy());
        FamilyLogbookEntry saved = logbookRepository.save(entry);
        try {
            familyMemoryService.captureLogbookResolutionMemory(saved);
        } catch (Exception e) {
            log.warn("⚠️ [LOGBOOK] Error capturando memoria de resolución: {}", e.getMessage());
        }
        return toResponse(saved);
    }

    private FamilyLogbookEntryResponse toResponse(FamilyLogbookEntry entry) {
        return new FamilyLogbookEntryResponse(
                entry.getId(),
                entry.getFamily().getId(),
                entry.getSituation(),
                entry.getDifficultyDetected(),
                entry.getEmotionIdentified(),
                entry.getUnderstanding(),
                entry.getCorrectionAction(),
                entry.getFamilyAgreement(),
                entry.getProgressEvidence(),
                entry.getStatus(),
                entry.getCreatedBy(),
                entry.getResolvedBy(),
                entry.getCreatedAt(),
                entry.getResolvedAt()
        );
    }
}
