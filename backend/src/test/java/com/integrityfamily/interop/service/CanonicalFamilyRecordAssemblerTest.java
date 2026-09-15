package com.integrityfamily.interop.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.consent.domain.ConsentPurpose;
import com.integrityfamily.consent.domain.ConsentStatus;
import com.integrityfamily.consent.repository.ConsentRepository;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.interop.canonical.CanonicalFamilyRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CanonicalFamilyRecordAssembler — Unit Tests")
class CanonicalFamilyRecordAssemblerTest {

    @Mock FamilyRepository familyRepository;
    @Mock MemberRepository memberRepository;
    @Mock EvaluationRepository evaluationRepository;
    @Mock FamilyRiskTrajectoryRepository trajectoryRepository;
    @Mock ConsentRepository consentRepository;

    @InjectMocks
    CanonicalFamilyRecordAssembler assembler;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia López").municipio("Bogotá").build();
    }

    @Test
    @DisplayName("familia inexistente → BusinessException NOT_FOUND")
    void shouldThrow_whenFamilyNotFound() {
        when(familyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assembler.assemble(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    @Test
    @DisplayName("ensambla household, assessments, risks y consents desde todos los repositorios")
    void shouldAssembleFullRecord() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

        FamilyMember member = new FamilyMember();
        member.setId(10L); member.setFullName("Ana"); member.setActive(true);
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(member));

        Evaluation evaluation = Evaluation.builder()
                .id(100L).family(family).status(EvaluationStatus.FINALIZED)
                .startedAt(LocalDateTime.now()).icf(70.0)
                .dimensionScores(List.of())
                .build();
        when(evaluationRepository.findByFamilyIdOrderByStartedAtDesc(1L)).thenReturn(List.of(evaluation));

        RiskTrajectory bankItem = RiskTrajectory.builder()
                .id(5L).code("EMBARAZO_ADOLESCENTE").name("Embarazo adolescente")
                .macrodomain(RiskMacrodomain.CRIANZA_ADOLESCENCIA).severityDefault("HIGH")
                .requiresSafetyProtocol(false).build();
        FamilyRiskTrajectory trajectory = FamilyRiskTrajectory.builder()
                .id(20L).family(family).trajectory(bankItem).status(TrajectoryStatus.DETECTED)
                .detectedAt(LocalDateTime.now()).build();
        when(trajectoryRepository.findByFamilyId(1L)).thenReturn(List.of(trajectory));

        com.integrityfamily.consent.domain.Consent consent = com.integrityfamily.consent.domain.Consent.builder()
                .id(1L).familyId(1L).purpose(ConsentPurpose.HEALTH_INTEROPERABILITY)
                .scope("ICF_SCORE").granteeReference("Ministerio de Salud")
                .status(ConsentStatus.GRANTED).grantedAt(LocalDateTime.now()).build();
        when(consentRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(consent));

        CanonicalFamilyRecord record = assembler.assemble(1L);

        assertThat(record.canonicalId()).isEqualTo("family-1");
        assertThat(record.household().members()).hasSize(1);
        assertThat(record.assessments()).hasSize(1);
        assertThat(record.risks()).hasSize(1);
        assertThat(record.consents()).hasSize(1);
        assertThat(record.interventions()).isEmpty();
        assertThat(record.outcomes()).isEmpty();
        assertThat(record.professionalNotes()).isEmpty();
        assertThat(record.evidences()).isEmpty();
    }

    @Test
    @DisplayName("familia sin evaluaciones/riesgos/consentimientos → listas vacías, no null ni excepción")
    void shouldAssembleEmptyRecord_whenNoRelatedData() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of());
        when(evaluationRepository.findByFamilyIdOrderByStartedAtDesc(1L)).thenReturn(List.of());
        when(trajectoryRepository.findByFamilyId(1L)).thenReturn(List.of());
        when(consentRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        CanonicalFamilyRecord record = assembler.assemble(1L);

        assertThat(record.household().members()).isEmpty();
        assertThat(record.assessments()).isEmpty();
        assertThat(record.risks()).isEmpty();
        assertThat(record.consents()).isEmpty();
    }
}
