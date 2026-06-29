package com.integrityfamily.family.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.family.dto.FamilyJourneyResponse;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FamilyWeeklyDigestService — Unit Tests")
class FamilyWeeklyDigestServiceTest {

    @Mock FamilyRepository              familyRepository;
    @Mock EvaluationRepository          evaluationRepository;
    @Mock PlanTaskRepository            planTaskRepository;
    @Mock FamilySprintRepository        sprintRepository;
    @Mock FamilyJourneySnapshotRepository snapshotRepository;
    @Mock SubtleSignalRadarService      radarService;
    @Mock FamilyJourneyService          journeyService;
    @Mock WhatsAppService               whatsAppService;

    @InjectMocks FamilyWeeklyDigestService service;

    private static final Long FAM_ID = 1L;
    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
        family.setName("Familia Pérez");
        family.setWhatsapp("+573009876543");

        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID)).thenReturn(List.of());
        when(planTaskRepository.countByFamilyId(FAM_ID)).thenReturn(0L);
        when(planTaskRepository.countCompletedByFamilyId(FAM_ID)).thenReturn(0L);
        when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(List.of());
        when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID)).thenReturn(List.of());

        var journey = new FamilyJourneyResponse(FAM_ID, "Familia Pérez",
                3, 29, List.of(), "Realiza el primer diagnóstico.", 5);
        when(journeyService.evaluate(FAM_ID)).thenReturn(journey);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Evaluation finalizedEval(double icf, LocalDateTime at) {
        Evaluation e = new Evaluation();
        e.setStatus(EvaluationStatus.FINALIZED);
        e.setIcf(icf);
        e.setFinalizedAt(at);
        return e;
    }

    private FamilyJourneySnapshot levelUpSnap(int toLevel, LocalDate date) {
        FamilyJourneySnapshot s = new FamilyJourneySnapshot();
        s.setFamilyId(FAM_ID);
        s.setJourneyLevel(toLevel);
        s.setLevelUp(true);
        s.setPreviousLevel(toLevel - 1);
        s.setSnapshotDate(date);
        return s;
    }

    private SubtleSignalRadarResponse radarWith(String phase, int highCount) {
        var icf = new SubtleSignalRadarResponse.IcfTrend(72.0, 2.0, 4.0, "IMPROVING", phase);
        List<SubtleSignalRadarResponse.MicroSignal> sigs = new java.util.ArrayList<>();
        for (int i = 0; i < highCount; i++) {
            sigs.add(new SubtleSignalRadarResponse.MicroSignal("emociones", "C" + i, "Desc.", "HIGH", 0.8));
        }
        return new SubtleSignalRadarResponse(
                FAM_ID, 3, null, null, null, null, icf,
                sigs, List.of(), List.of(), 80, "Narrativa.", LocalDateTime.now());
    }

    private String captureMessage() {
        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(whatsAppService).sendToFamily(any(), cap.capture());
        return cap.getValue();
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("lanza IllegalArgumentException cuando la familia no existe")
        void throwsWhenNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.sendDigest(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — sin WhatsApp")
    class NoWhatsApp {

        @Test
        @DisplayName("devuelve false y no envía cuando la familia no tiene WhatsApp")
        void skipsFamilyWithoutWhatsApp() {
            family.setWhatsapp(null);
            boolean result = service.sendDigest(FAM_ID);
            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }

        @Test
        @DisplayName("devuelve false cuando WhatsApp está en blanco")
        void skipsFamilyWithBlankWhatsApp() {
            family.setWhatsapp("   ");
            boolean result = service.sendDigest(FAM_ID);
            assertThat(result).isFalse();
            verifyNoInteractions(whatsAppService);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — envío exitoso")
    class SuccessfulSend {

        @Test
        @DisplayName("devuelve true y llama sendToFamily cuando todo está configurado")
        void returnsTrueAndSends() {
            boolean result = service.sendDigest(FAM_ID);
            assertThat(result).isTrue();
            verify(whatsAppService).sendToFamily(eq(family), anyString());
        }

        @Test
        @DisplayName("el mensaje incluye el nombre de la familia")
        void messageIncludesFamilyName() {
            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("Familia Pérez");
        }

        @Test
        @DisplayName("el mensaje incluye el nivel del viaje")
        void messageIncludesJourneyLevel() {
            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("Nivel 3");
        }

        @Test
        @DisplayName("el mensaje incluye el porcentaje del viaje")
        void messageIncludesJourneyProgress() {
            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("29%");
        }

        @Test
        @DisplayName("el mensaje incluye el próximo paso")
        void messageIncludesNextAction() {
            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("Realiza el primer diagnóstico.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — contenido ICF")
    class IcfContent {

        @Test
        @DisplayName("incluye el ICF actual cuando hay evaluaciones")
        void includesCurrentIcf() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(finalizedEval(74.0, LocalDateTime.now().minusDays(2))));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("74");
        }

        @Test
        @DisplayName("incluye delta positivo vs 7 días cuando mejoró")
        void includesPositiveDelta() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(65.0, LocalDateTime.now().minusDays(10)),
                            finalizedEval(72.0, LocalDateTime.now().minusDays(1))
                    ));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("+7.0");
        }

        @Test
        @DisplayName("incluye delta negativo cuando bajó")
        void includesNegativeDelta() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(80.0, LocalDateTime.now().minusDays(12)),
                            finalizedEval(72.0, LocalDateTime.now().minusDays(1))
                    ));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("-8.0");
        }

        @Test
        @DisplayName("no incluye ICF cuando no hay evaluaciones")
        void omitsIcfWithoutEvals() {
            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).doesNotContain("Índice ICF");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — level-up de la semana")
    class LevelUpContent {

        @Test
        @DisplayName("menciona el level-up cuando ocurrió esta semana")
        void mentionsLevelUpThisWeek() {
            when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(levelUpSnap(4, LocalDate.now().minusDays(2))));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("nivel 4").contains("¡Subiste");
        }

        @Test
        @DisplayName("no menciona level-up cuando fue hace más de 7 días")
        void omitsOldLevelUp() {
            when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(levelUpSnap(4, LocalDate.now().minusDays(10))));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).doesNotContain("¡Subiste");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — tareas y sprint")
    class TasksAndSprint {

        @Test
        @DisplayName("incluye tasa de completitud cuando hay tareas")
        void includesTaskCompletionRate() {
            when(planTaskRepository.countByFamilyId(FAM_ID)).thenReturn(10L);
            when(planTaskRepository.countCompletedByFamilyId(FAM_ID)).thenReturn(7L);

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("7/10").contains("70%");
        }

        @Test
        @DisplayName("menciona sprint activo cuando existe")
        void mentionsActiveSprint() {
            FamilySprint sprint = new FamilySprint();
            sprint.setStatus("ACTIVE");
            when(sprintRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(sprint));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("Sprint activo");
        }

        @Test
        @DisplayName("no menciona sprint cuando no hay ninguno activo")
        void omitsSprintWhenNone() {
            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).doesNotContain("Sprint activo");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — radar y señales")
    class RadarContent {

        @Test
        @DisplayName("incluye fase de evolución cuando el radar está disponible")
        void includesEvolutionPhase() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(60.0, LocalDateTime.now().minusDays(30)),
                            finalizedEval(72.0, LocalDateTime.now().minusDays(5))
                    ));
            when(radarService.analyze(FAM_ID)).thenReturn(radarWith("consciente", 0));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).containsIgnoringCase("consciente");
        }

        @Test
        @DisplayName("incluye alerta de señales altas cuando las hay")
        void includesHighSignalAlert() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(60.0, LocalDateTime.now().minusDays(30)),
                            finalizedEval(72.0, LocalDateTime.now().minusDays(5))
                    ));
            when(radarService.analyze(FAM_ID)).thenReturn(radarWith("reactivo", 2));

            service.sendDigest(FAM_ID);
            assertThat(captureMessage()).contains("2").contains("Radar");
        }

        @Test
        @DisplayName("no falla cuando el radar lanza excepción")
        void gracefullyHandlesRadarFailure() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM_ID))
                    .thenReturn(List.of(
                            finalizedEval(60.0, LocalDateTime.now().minusDays(30)),
                            finalizedEval(72.0, LocalDateTime.now().minusDays(5))
                    ));
            when(radarService.analyze(FAM_ID)).thenThrow(new RuntimeException("Sin datos"));

            assertThatCode(() -> service.sendDigest(FAM_ID)).doesNotThrowAnyException();
            verify(whatsAppService).sendToFamily(any(), any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendDigest() — resiliencia")
    class Resilience {

        @Test
        @DisplayName("devuelve false y no propaga excepción cuando WhatsApp falla")
        void returnsFalseOnWhatsAppError() {
            doThrow(new RuntimeException("Fallo de red"))
                    .when(whatsAppService).sendToFamily(any(), any());

            boolean result = service.sendDigest(FAM_ID);
            assertThat(result).isFalse();
        }
    }
}
