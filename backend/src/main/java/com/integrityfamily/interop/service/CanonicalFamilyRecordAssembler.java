package com.integrityfamily.interop.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.consent.repository.ConsentRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.interop.canonical.Assessment;
import com.integrityfamily.interop.canonical.CanonicalFamilyRecord;
import com.integrityfamily.interop.canonical.Household;
import com.integrityfamily.interop.canonical.Risk;
import com.integrityfamily.interop.mapper.AssessmentMapper;
import com.integrityfamily.interop.mapper.ConsentMapper;
import com.integrityfamily.interop.mapper.HouseholdMapper;
import com.integrityfamily.interop.mapper.RiskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Ensambla el {@link CanonicalFamilyRecord} completo de una familia a partir
 * del dominio real (Fase 3 del programa de interoperabilidad). Es el único
 * punto donde el modelo canónico "toca" repositorios JPA — los mappers
 * individuales ({@code interop.mapper}) son funciones puras sin I/O.
 *
 * Interventions/Outcomes/ProfessionalNotes/Evidences quedan vacíos
 * deliberadamente en esta fase: mapearlos requiere entrar a los módulos
 * plan/checklist/support, que no se tocaron aquí para mantener el alcance
 * acotado. El agregado ya es válido y útil sin ellos (Household, Assessments,
 * Risks, Consents cubren identidad, evaluación, riesgo y consentimiento).
 */
@Service
@RequiredArgsConstructor
public class CanonicalFamilyRecordAssembler {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final EvaluationRepository evaluationRepository;
    private final FamilyRiskTrajectoryRepository trajectoryRepository;
    private final ConsentRepository consentRepository;

    @Transactional(readOnly = true)
    public CanonicalFamilyRecord assemble(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada.", "INTEROP_FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        Household household = HouseholdMapper.toCanonical(family, memberRepository.findByFamilyId(familyId));

        List<Assessment> assessments = evaluationRepository.findByFamilyIdOrderByStartedAtDesc(familyId)
                .stream()
                .map(this::toAssessment)
                .toList();

        List<Risk> risks = trajectoryRepository.findByFamilyId(familyId)
                .stream()
                .map(RiskMapper::toCanonical)
                .toList();

        var consents = consentRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream()
                .map(ConsentMapper::toCanonical)
                .toList();

        return CanonicalFamilyRecord.builder()
                .canonicalId("family-" + familyId)
                .household(household)
                .assessments(assessments)
                .risks(risks)
                .interventions(Collections.emptyList())
                .outcomes(Collections.emptyList())
                .professionalNotes(Collections.emptyList())
                .evidences(Collections.emptyList())
                .consents(consents)
                .build();
    }

    private Assessment toAssessment(Evaluation evaluation) {
        return AssessmentMapper.toCanonical(evaluation, evaluation.getDimensionScores());
    }
}
