package com.integrityfamily.bitacora.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.bitacora.dto.SprintDtos.*;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests para SprintService — Sprint de Evolución Familiar.
 *
 * Cubre los flujos principales:
 *  - Consulta de sprint activo e historial
 *  - Creación de sprint (validaciones de duración y unicidad)
 *  - Toggle de misión (PENDING ↔ COMPLETED)
 *  - Check-in diario (unicidad por fecha + miembro)
 *  - Cierre con retrospectiva: cálculo del consistencyScore y fallback IA
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SprintService")
class SprintServiceTest {

    // ── Repositories ────────────────────────────────────────────────────────
    @Mock private FamilyRepository        familyRepository;
    @Mock private FamilySprintRepository  sprintRepository;
    @Mock private SprintMissionRepository missionRepository;
    @Mock private SprintDailyRepository   dailyRepository;
    @Mock private SprintRetrospectiveRepository retrospectiveRepository;

    // ── AI layer ────────────────────────────────────────────────────────────
    @Mock private AiProvider        aiProvider;
    @Mock private ContextSynthesizer contextSynthesizer;

    @InjectMocks
    private SprintService service;

    // ── Fixtures ────────────────────────────────────────────────────────────
    private Family     family;
    private FamilySprint activeSprint;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(1L);
        family.setName("Familia Lopez");

        activeSprint = FamilySprint.builder()
                .id(10L)
                .family(family)
                .objective("Mejorar comunicación")
                .riskDimension("comunicacion")
                .durationDays(7)
                .startDate(LocalDate.now().minusDays(3))
                .endDate(LocalDate.now().plusDays(4))
                .status("ACTIVE")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getActiveSprint()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getActiveSprint()")
    class GetActiveSprint {

        @Test
        @DisplayName("Devuelve null cuando no hay sprint activo")
        void shouldReturnNull_whenNoActiveSprint() {
            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.empty());

            SprintResponse result = service.getActiveSprint(1L);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Devuelve el sprint activo cuando existe")
        void shouldReturnActiveSprint_whenExists() {
            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.of(activeSprint));
            when(missionRepository.findBySprintId(10L)).thenReturn(List.of());
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(10L)).thenReturn(List.of());
            when(retrospectiveRepository.findBySprintId(10L)).thenReturn(Optional.empty());

            SprintResponse result = service.getActiveSprint(1L);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo("ACTIVE");
            assertThat(result.objective()).isEqualTo("Mejorar comunicación");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  createSprint()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createSprint()")
    class CreateSprint {

        @Test
        @DisplayName("Crea sprint con duración por defecto (7 días) cuando no se especifica")
        void shouldCreateSprintWithDefaultDuration() {
            CreateSprintRequest request = new CreateSprintRequest(
                    "Objetivo test", "comunicacion", null, List.of());

            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.empty());
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(sprintRepository.save(any(FamilySprint.class))).thenAnswer(inv -> {
                FamilySprint s = inv.getArgument(0);
                s.setId(99L);
                return s;
            });
            when(missionRepository.findBySprintId(99L)).thenReturn(List.of());
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(99L)).thenReturn(List.of());
            when(retrospectiveRepository.findBySprintId(99L)).thenReturn(Optional.empty());

            SprintResponse result = service.createSprint(1L, request);

            assertThat(result.durationDays()).isEqualTo(7);
            assertThat(result.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Crea sprint de 15 días con misiones iniciales")
        void shouldCreateSprintWith15DaysAndMissions() {
            List<String> missionDescs = List.of("Hablar 10 min por día", "Sin pantallas a las 9pm");
            CreateSprintRequest request = new CreateSprintRequest(
                    "Mejorar hábitos", "habitos", 15, missionDescs);

            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.empty());
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

            FamilySprint savedSprint = FamilySprint.builder()
                    .id(20L).family(family).objective("Mejorar hábitos")
                    .riskDimension("habitos").durationDays(15)
                    .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(15))
                    .status("ACTIVE").build();
            when(sprintRepository.save(any(FamilySprint.class))).thenReturn(savedSprint);

            List<SprintMission> savedMissions = List.of(
                    SprintMission.builder().id(1L).sprint(savedSprint)
                            .description("Hablar 10 min por día").status("PENDING").build(),
                    SprintMission.builder().id(2L).sprint(savedSprint)
                            .description("Sin pantallas a las 9pm").status("PENDING").build()
            );
            when(missionRepository.saveAll(anyList())).thenReturn(savedMissions);
            when(missionRepository.findBySprintId(20L)).thenReturn(savedMissions);
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(20L)).thenReturn(List.of());
            when(retrospectiveRepository.findBySprintId(20L)).thenReturn(Optional.empty());

            SprintResponse result = service.createSprint(1L, request);

            assertThat(result.durationDays()).isEqualTo(15);
            verify(missionRepository).saveAll(argThat(list ->
                    ((List<?>) list).size() == 2
            ));
        }

        @Test
        @DisplayName("Lanza BusinessException cuando ya existe un sprint activo")
        void shouldThrow_whenActiveSprintAlreadyExists() {
            when(sprintRepository.findActiveSprintForFamily(1L))
                    .thenReturn(Optional.of(activeSprint));

            CreateSprintRequest request = new CreateSprintRequest(
                    "Otro objetivo", "emociones", 7, List.of());

            assertThatThrownBy(() -> service.createSprint(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya existe un sprint activo");
        }

        @Test
        @DisplayName("Lanza BusinessException cuando la familia no existe")
        void shouldThrow_whenFamilyNotFound() {
            when(sprintRepository.findActiveSprintForFamily(99L)).thenReturn(Optional.empty());
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            CreateSprintRequest request = new CreateSprintRequest(
                    "Objetivo", "comunicacion", 7, List.of());

            assertThatThrownBy(() -> service.createSprint(99L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("Lanza BusinessException para duración inválida (10 días)")
        void shouldThrow_whenDurationIsInvalid() {
            when(sprintRepository.findActiveSprintForFamily(1L)).thenReturn(Optional.empty());
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

            CreateSprintRequest request = new CreateSprintRequest(
                    "Objetivo", "comunicacion", 10, List.of());

            assertThatThrownBy(() -> service.createSprint(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("duración del sprint debe ser de 7 o 15");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  toggleMission()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("toggleMission()")
    class ToggleMission {

        private SprintMission pendingMission;

        @BeforeEach
        void setUpMission() {
            pendingMission = SprintMission.builder()
                    .id(5L).sprint(activeSprint)
                    .description("Misión de prueba").status("PENDING")
                    .build();
        }

        @Test
        @DisplayName("PENDING → COMPLETED: guarda la misión completada con timestamp")
        void shouldToggle_pendingToCompleted() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));
            when(missionRepository.findById(5L)).thenReturn(Optional.of(pendingMission));
            when(missionRepository.save(any(SprintMission.class))).thenReturn(pendingMission);
            when(missionRepository.findBySprintId(10L)).thenReturn(List.of(pendingMission));
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(10L)).thenReturn(List.of());
            when(retrospectiveRepository.findBySprintId(10L)).thenReturn(Optional.empty());

            service.toggleMission(10L, 5L);

            assertThat(pendingMission.getStatus()).isEqualTo("COMPLETED");
            assertThat(pendingMission.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("COMPLETED → PENDING: limpia el campo completedAt")
        void shouldToggle_completedToPending() {
            pendingMission.setStatus("COMPLETED");
            pendingMission.setCompletedAt(LocalDateTime.now().minusHours(1));

            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));
            when(missionRepository.findById(5L)).thenReturn(Optional.of(pendingMission));
            when(missionRepository.save(any(SprintMission.class))).thenReturn(pendingMission);
            when(missionRepository.findBySprintId(10L)).thenReturn(List.of(pendingMission));
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(10L)).thenReturn(List.of());
            when(retrospectiveRepository.findBySprintId(10L)).thenReturn(Optional.empty());

            service.toggleMission(10L, 5L);

            assertThat(pendingMission.getStatus()).isEqualTo("PENDING");
            assertThat(pendingMission.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("Lanza BusinessException cuando la misión no pertenece al sprint")
        void shouldThrow_whenMissionDoesNotBelongToSprint() {
            FamilySprint otherSprint = FamilySprint.builder().id(99L).build();
            pendingMission.setSprint(otherSprint);

            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));
            when(missionRepository.findById(5L)).thenReturn(Optional.of(pendingMission));

            assertThatThrownBy(() -> service.toggleMission(10L, 5L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no pertenece a este sprint");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  submitDailyCheckin()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("submitDailyCheckin()")
    class SubmitDailyCheckin {

        private CreateDailyCheckinRequest dailyRequest;

        @BeforeEach
        void setUpRequest() {
            dailyRequest = new CreateDailyCheckinRequest(
                    "Ayer avancé en la tarea.",
                    "Hoy seguiré con la comunicación.",
                    "Ningún bloqueo.",
                    "Sin resolución pendiente.",
                    "HAPPY",
                    "William"
            );
        }

        @Test
        @DisplayName("Guarda el check-in correctamente")
        void shouldSaveCheckin_whenFirstOfDay() {
            SprintDaily savedDaily = SprintDaily.builder()
                    .id(7L).sprint(activeSprint).memberName("William")
                    .checkinDate(LocalDate.now())
                    .yesterdayText(dailyRequest.yesterdayText())
                    .todayText(dailyRequest.todayText())
                    .blockagesText(dailyRequest.blockagesText())
                    .resolutionText(dailyRequest.resolutionText())
                    .emotionalIndicator(dailyRequest.emotionalIndicator())
                    .createdAt(LocalDateTime.now()).build();

            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));
            when(dailyRepository.existsBySprintIdAndMemberNameAndCheckinDate(
                    10L, "William", LocalDate.now())).thenReturn(false);
            when(dailyRepository.save(any(SprintDaily.class))).thenReturn(savedDaily);

            SprintDailyResponse result = service.submitDailyCheckin(10L, dailyRequest);

            assertThat(result.memberName()).isEqualTo("William");
            assertThat(result.emotionalIndicator()).isEqualTo("HAPPY");
        }

        @Test
        @DisplayName("Lanza BusinessException cuando ya se hizo check-in hoy")
        void shouldThrow_whenCheckinAlreadySubmittedToday() {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));
            when(dailyRepository.existsBySprintIdAndMemberNameAndCheckinDate(
                    10L, "William", LocalDate.now())).thenReturn(true);

            assertThatThrownBy(() -> service.submitDailyCheckin(10L, dailyRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya registraste tu Check-in Diario");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  closeSprintAndCreateRetrospective() — consistencyScore
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("closeSprintAndCreateRetrospective() — consistencyScore")
    class ConsistencyScore {

        /**
         * Fórmula:
         *   missionPoints  = (completedCount / totalMissions) * 4          [0–4]
         *   dailyPoints    = min(1.0, dailies / (durationDays * 1.5)) * 4  [0–4]
         *   emotionalPoints = (positiveInteractions/10 * 2) + ((1 – tension/10) * 1) [0–3]
         *   score = clamp(round(sum), 1, 10)
         */

        private CloseSprintRequest retroWith(int positiveInteractions, int tensionLevel) {
            return new CloseSprintRequest(
                    "Bien", "Difícil", "Aprendimos", "Ajustaremos",
                    tensionLevel, 8, 7, positiveInteractions, 6
            );
        }

        /** misiones completadas, misiones totales, dailies registrados */
        private void mockSprintData(int completedMissions, int totalMissions, int dailyCount) {
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));

            List<SprintMission> missions = new java.util.ArrayList<>();
            for (int i = 0; i < totalMissions; i++) {
                String status = i < completedMissions ? "COMPLETED" : "PENDING";
                missions.add(SprintMission.builder()
                        .id((long)(i + 1)).sprint(activeSprint)
                        .description("Misión " + i).status(status).build());
            }
            when(missionRepository.findBySprintId(10L)).thenReturn(missions);

            List<SprintDaily> dailies = new java.util.ArrayList<>();
            for (int i = 0; i < dailyCount; i++) {
                dailies.add(SprintDaily.builder()
                        .id((long)(i + 1)).sprint(activeSprint)
                        .memberName("Miembro").checkinDate(LocalDate.now().minusDays(i))
                        .yesterdayText("a").todayText("b").blockagesText("c").resolutionText("d")
                        .createdAt(LocalDateTime.now()).build());
            }
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(10L)).thenReturn(dailies);

            when(retrospectiveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sprintRepository.save(any(FamilySprint.class))).thenReturn(activeSprint);
            when(missionRepository.findBySprintId(10L)).thenReturn(missions); // called again in mapToResponse
            when(dailyRepository.findBySprintIdOrderByCheckinDateDesc(10L)).thenReturn(dailies);
            when(retrospectiveRepository.findBySprintId(10L)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("Score máximo: 3/3 misiones, 15 dailies, interactions=10, tension=0 → 10")
        void shouldReturnMaxScore_whenPerfectPerformance() {
            // missionPoints = 1.0*4 = 4
            // dailyPoints = min(1.0, 15/(7*1.5)) = min(1.0, 1.43) = 1.0 → 4
            // emotionalPoints = (10/10*2) + (1 - 0/10)*1 = 2 + 1 = 3
            // total = 11 → clamp(11, 1, 10) = 10
            mockSprintData(3, 3, 15);
            when(aiProvider.generateResponse(anyString(), any())).thenReturn("AI feedback");

            SprintResponse result = service.closeSprintAndCreateRetrospective(
                    10L, retroWith(10, 0));

            ArgumentCaptor<SprintRetrospective> retroCaptor = ArgumentCaptor.forClass(SprintRetrospective.class);
            verify(retrospectiveRepository).save(retroCaptor.capture());
            assertThat(retroCaptor.getValue().getConsistencyScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("Score mínimo: 0/3 misiones, 0 dailies, interactions=1, tension=9 → 1")
        void shouldReturnMinScore_whenPoorPerformance() {
            // missionPoints = 0*4 = 0
            // dailyPoints = 0 → 0
            // emotionalPoints = (1/10*2) + (1-9/10)*1 = 0.2 + 0.1 = 0.3
            // total = 0.3 → max(1, 0.3) = 1
            mockSprintData(0, 3, 0);
            when(aiProvider.generateResponse(anyString(), any())).thenReturn("AI feedback");

            service.closeSprintAndCreateRetrospective(10L, retroWith(1, 9));

            ArgumentCaptor<SprintRetrospective> retroCaptor = ArgumentCaptor.forClass(SprintRetrospective.class);
            verify(retrospectiveRepository).save(retroCaptor.capture());
            assertThat(retroCaptor.getValue().getConsistencyScore()).isEqualTo(1);
        }

        @Test
        @DisplayName("Sin misiones: missionRatio = 1.0 (asume perfecto), score depende de dailies+emocional")
        void shouldAssumeFullMissionRatio_whenNoMissions() {
            // missionRatio = 1.0 (vacío → 4 puntos)
            // dailyPoints = min(1.0, 7/(7*1.5)) = min(1.0, 0.667) → 2.667
            // emotionalPoints = (7/10*2) + (1-5/10)*1 = 1.4 + 0.5 = 1.9
            // total ≈ 4 + 2.667 + 1.9 = 8.567 → round → 9
            mockSprintData(0, 0, 7);
            when(aiProvider.generateResponse(anyString(), any())).thenReturn("AI feedback");

            service.closeSprintAndCreateRetrospective(10L, retroWith(7, 5));

            ArgumentCaptor<SprintRetrospective> captor = ArgumentCaptor.forClass(SprintRetrospective.class);
            verify(retrospectiveRepository).save(captor.capture());
            assertThat(captor.getValue().getConsistencyScore()).isEqualTo(9);
        }

        @Test
        @DisplayName("Usa fallback de texto cuando el AiProvider lanza excepción")
        void shouldUseFallbackFeedback_whenAiProviderFails() {
            mockSprintData(2, 2, 10);
            when(aiProvider.generateResponse(anyString(), any()))
                    .thenThrow(new RuntimeException("AI service unavailable"));

            service.closeSprintAndCreateRetrospective(10L, retroWith(8, 3));

            ArgumentCaptor<SprintRetrospective> captor = ArgumentCaptor.forClass(SprintRetrospective.class);
            verify(retrospectiveRepository).save(captor.capture());
            // El fallback siempre produce texto no nulo
            assertThat(captor.getValue().getAiFeedback()).isNotBlank();
            assertThat(captor.getValue().getAiFeedback())
                    .contains("Análisis del Microciclo");
        }

        @Test
        @DisplayName("Lanza BusinessException cuando el sprint ya está COMPLETED")
        void shouldThrow_whenSprintAlreadyCompleted() {
            activeSprint.setStatus("COMPLETED");
            when(sprintRepository.findById(10L)).thenReturn(Optional.of(activeSprint));

            CloseSprintRequest request = retroWith(5, 5);
            assertThatThrownBy(() ->
                    service.closeSprintAndCreateRetrospective(10L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya no está activo");
        }

        @Test
        @DisplayName("Marca el sprint como COMPLETED y guarda la fecha de cierre")
        void shouldMarkSprintAsCompleted_afterClose() {
            mockSprintData(1, 2, 5);
            when(aiProvider.generateResponse(anyString(), any())).thenReturn("feedback");

            service.closeSprintAndCreateRetrospective(10L, retroWith(6, 4));

            assertThat(activeSprint.getStatus()).isEqualTo("COMPLETED");
            assertThat(activeSprint.getEndDate()).isEqualTo(LocalDate.now());
        }
    }
}
