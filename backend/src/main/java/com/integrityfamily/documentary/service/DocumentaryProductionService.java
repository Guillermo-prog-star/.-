package com.integrityfamily.documentary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.documentary.domain.DocumentaryProduction;
import com.integrityfamily.documentary.domain.DocumentaryScope;
import com.integrityfamily.documentary.domain.ProductionStatus;
import com.integrityfamily.documentary.repository.DocumentaryProductionRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentaryProductionService {

    private final DocumentaryProductionRepository productionRepository;
    private final FamilyRepository familyRepository;
    private final TaskEvidenceRepository evidenceRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentaryProduction createDraft(Long familyId, String title, DocumentaryScope scope, Long referenceId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Family not found"));

        DocumentaryProduction production = DocumentaryProduction.builder()
                .family(family)
                .title(title)
                .scope(scope)
                .referenceId(referenceId)
                .status(ProductionStatus.DRAFT)
                .build();

        return productionRepository.save(production);
    }

    @Transactional
    public DocumentaryProduction updateCuration(Long productionId, List<Long> evidenceIds) {
        DocumentaryProduction production = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        if (production.getStatus() == ProductionStatus.APPROVED || production.getStatus() == ProductionStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot update curation for an approved or published production");
        }

        List<TaskEvidence> evidences = evidenceRepository.findAllById(evidenceIds);
        production.setCuratedEvidences(evidences);
        production.setStatus(ProductionStatus.CURATED);

        return productionRepository.save(production);
    }

    @Transactional
    public DocumentaryProduction generateScript(Long productionId) {
        DocumentaryProduction production = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        if (production.getStatus() != ProductionStatus.CURATED && production.getStatus() != ProductionStatus.GENERATED) {
            throw new IllegalStateException("Production must be curated before generating script");
        }

        // Preparar el prompt con las evidencias curadas
        String curatedContext = production.getCuratedEvidences().stream()
                .map(e -> String.format("[%s] %s: %s", e.getEvidenceType(), e.getTitle(), e.getTextContent() != null ? e.getTextContent() : "Media adjunta"))
                .collect(Collectors.joining("\n"));

        String prompt = "Actúa como un director de documentales. Basado en las siguientes evidencias curadas de una familia, escribe un guion narrativo estructurado en 3 capítulos (Inicio, Nudo/Desarrollo, Desenlace) y un mensaje final (Legado). Crea un relato emotivo y coherente.\n\nEvidencias:\n" + curatedContext;

        try {
            String aiScript = aiProvider.generateRawResponse(prompt);
            production.setScriptData(aiScript);
            production.setStatus(ProductionStatus.GENERATED);
        } catch (Exception e) {
            log.error("Error al generar el guion IA para la producción {}", productionId, e);
            throw new RuntimeException("Error al generar guion", e);
        }

        return productionRepository.save(production);
    }

    @Transactional
    public DocumentaryProduction approveProduction(Long productionId) {
        DocumentaryProduction production = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        production.setStatus(ProductionStatus.APPROVED);
        DocumentaryProduction saved = productionRepository.save(production);
        org.hibernate.Hibernate.initialize(saved.getCuratedEvidences());
        return saved;
    }

    @Transactional
    public DocumentaryProduction publishProduction(Long productionId, String exportUrl) {
        DocumentaryProduction production = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));

        production.setStatus(ProductionStatus.PUBLISHED);
        production.setExportUrl(exportUrl);
        DocumentaryProduction saved = productionRepository.save(production);
        org.hibernate.Hibernate.initialize(saved.getCuratedEvidences());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DocumentaryProduction> getProductionsByFamily(Long familyId) {
        List<DocumentaryProduction> list = productionRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        list.forEach(p -> org.hibernate.Hibernate.initialize(p.getCuratedEvidences()));
        return list;
    }

    @Transactional(readOnly = true)
    public DocumentaryProduction getProduction(Long productionId) {
        DocumentaryProduction prod = productionRepository.findById(productionId)
                .orElseThrow(() -> new IllegalArgumentException("Production not found"));
        org.hibernate.Hibernate.initialize(prod.getCuratedEvidences());
        return prod;
    }
}
