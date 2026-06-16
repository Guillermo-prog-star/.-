package com.integrityfamily.documentary.service;

import com.integrityfamily.documentary.dto.SubmitDocumentaryRequest;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentaryService {

    private final FamilyDocumentaryRepository documentaryRepository;
    private final FamilyRepository familyRepository;
    private final PlanTaskRepository planTaskRepository;
    private final TaskEvidenceRepository taskEvidenceRepository;

    @Transactional
    public FamilyDocumentary createDocumentary(SubmitDocumentaryRequest request) {
        Family family = familyRepository.findById(request.getFamilyId())
                .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        PlanTask task = null;
        if (request.getTaskId() != null) {
            task = planTaskRepository.findById(request.getTaskId())
                    .orElseThrow(() -> new BusinessException("Tarea no encontrada", "TASK_NOT_FOUND", HttpStatus.NOT_FOUND));
        }

        // Crear la entidad
        FamilyDocumentary documentary = FamilyDocumentary.builder()
                .family(family)
                .task(task)
                .title(request.getTitle())
                .content(request.getContent())
                .sourceType(request.getSourceType() != null ? request.getSourceType() : DocumentarySourceType.SPONTANEOUS)
                .status(DocumentaryStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        FamilyDocumentary saved = documentaryRepository.save(documentary);

        // Si mandaron IDs de evidencia, vincularlas al documental recién creado
        if (request.getEvidenceIds() != null && !request.getEvidenceIds().isEmpty()) {
            for (Long evId : request.getEvidenceIds()) {
                TaskEvidence ev = taskEvidenceRepository.findById(evId).orElse(null);
                if (ev != null && ev.getFamily().getId().equals(family.getId())) {
                    ev.setDocumentary(saved);
                    taskEvidenceRepository.save(ev);
                }
            }
        }

        log.info("🎬 Documental creado: {} (Familia: {}, Tipo: {})", saved.getTitle(), family.getId(), saved.getSourceType());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<com.integrityfamily.documentary.dto.FamilyDocumentaryDTO> getFamilyDocumentaries(Long familyId) {
        return documentaryRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(com.integrityfamily.documentary.dto.FamilyDocumentaryDTO::fromEntity)
                .toList();
    }
}
