package com.integrityfamily.trajectory.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.RiskTrajectoryRepository;
import com.integrityfamily.trajectory.service.TrajectorySuggestionService.TrajectorySuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TrajectorySuggestionService — Unit Tests")
class TrajectorySuggestionServiceTest {

    @Mock FamilyLongitudinalStateRepository ltsRepository;
    @Mock FamilyRiskTrajectoryRepository    familyTrajectoryRepo;
    @Mock RiskTrajectoryRepository          trajectoryRepo;

    @InjectMocks TrajectorySuggestionService service;

    private static final Long FAM_ID = 1L;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private RiskTrajectory trajectory(String code, String name, RiskMacrodomain domain) {
        RiskTrajectory t = new RiskTrajectory();
        t.setCode(code);
        t.setName(name);
        t.setMacrodomain(domain);
        t.setSeverityDefault("ALTA");
        t.setActive(true);
        return t;
    }

    private FamilyLongitudinalState lts() {
        FamilyLongitudinalState l = new FamilyLongitudinalState();
        l.setCurrentRiskLevel("MEDIO");
        l.setRiskTrend("STABLE");
        l.setCommunicationCollapseActive(false);
        l.setCrisisCount30d(0);
        l.setConsecutiveDeteriorations(0);
        l.setDimEmociones(60.0);
        l.setDimComunicacion(60.0);
        l.setDimHabitos(60.0);
        l.setDimTiempos(60.0);
        l.setIcfCurrent(65.0);
        return l;
    }

    /** Configura bank con trayectorias básicas y sin asignaciones previas */
    private void stubBankWith(RiskTrajectory... trajectories) {
        when(trajectoryRepo.findByActiveTrue()).thenReturn(List.of(trajectories));
        when(familyTrajectoryRepo.findByFamilyId(FAM_ID)).thenReturn(List.of());
    }

    private List<RiskTrajectory> fullBank() {
        return List.of(
            trajectory("IDEACION_SUICIDA",       "Ideación suicida",      RiskMacrodomain.SALUD_MENTAL),
            trajectory("AISLAMIENTO_SOCIAL",     "Aislamiento social",    RiskMacrodomain.SALUD_MENTAL),
            trajectory("AUTOLESIONES",            "Autolesiones",          RiskMacrodomain.SALUD_MENTAL),
            trajectory("VIOLENCIA_INTRAFAMILIAR", "Violencia intrafam.",   RiskMacrodomain.RELACIONES_PAREJA),
            trajectory("CRISIS_PAREJA",          "Crisis de pareja",       RiskMacrodomain.RELACIONES_PAREJA),
            trajectory("DIVORCIO_SEPARACION",    "Divorcio",               RiskMacrodomain.RELACIONES_PAREJA),
            trajectory("CRIANZA_AUTORITARIA",    "Crianza autoritaria",    RiskMacrodomain.CRIANZA_ADOLESCENCIA),
            trajectory("CRIANZA_PERMISIVA",      "Crianza permisiva",      RiskMacrodomain.CRIANZA_ADOLESCENCIA),
            trajectory("CONSUMO_ALCOHOL_ADULTO", "Consumo alcohol",        RiskMacrodomain.ADICCIONES),
            trajectory("CONSUMO_MARIHUANA",      "Consumo marihuana",      RiskMacrodomain.ADICCIONES),
            trajectory("USO_PROBLEMATICO_VIDEOJUEGOS", "Videojuegos",      RiskMacrodomain.ADICCIONES),
            trajectory("ABANDONO_ADULTO_MAYOR",  "Abandono adulto mayor",  RiskMacrodomain.ADULTO_MAYOR),
            trajectory("JOVEN_SIN_PROYECTO",     "Joven sin proyecto",     RiskMacrodomain.EDUCACION_DESARROLLO),
            trajectory("DUELO_COMPLICADO",       "Duelo complicado",       RiskMacrodomain.SALUD_MENTAL),
            trajectory("DESEMPLEO_PROLONGADO",   "Desempleo prolongado",   RiskMacrodomain.ECONOMIA_FAMILIAR),
            trajectory("ENDEUDAMIENTO_FAMILIAR", "Endeudamiento",          RiskMacrodomain.ECONOMIA_FAMILIAR),
            trajectory("RUPTURA_GENERACIONAL",   "Ruptura generacional",   RiskMacrodomain.LEGADO)
        );
    }

    @BeforeEach
    void setUp() {
        when(trajectoryRepo.findByActiveTrue()).thenReturn(fullBank());
        when(familyTrajectoryRepo.findByFamilyId(FAM_ID)).thenReturn(List.of());
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("suggest() — sin LTS")
    class NoLts {

        @Test
        @DisplayName("devuelve lista vacía cuando no existe LTS")
        void returnsEmptyWithoutLts() {
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
            assertThat(service.suggest(FAM_ID)).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("suggest() — dimensión emocional crítica (< 40)")
    class EmotionalDimension {

        @Test
        @DisplayName("dim. emocional < 40 sugiere IDEACION_SUICIDA")
        void suggestsIdeacionSuicida() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(30.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            List<TrajectorySuggestion> result = service.suggest(FAM_ID);
            assertThat(result).extracting(TrajectorySuggestion::code)
                    .contains("IDEACION_SUICIDA");
        }

        @Test
        @DisplayName("dim. emocional < 40 sugiere AISLAMIENTO_SOCIAL")
        void suggestsAislamientoSocial() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(25.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("AISLAMIENTO_SOCIAL");
        }

        @Test
        @DisplayName("risk CRITICO + dim. emocional < 40 → IDEACION_SUICIDA con confidence 80")
        void criticoLevelBoostsConfidence() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(30.0);
            l.setCurrentRiskLevel("CRITICO");
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            TrajectorySuggestion s = service.suggest(FAM_ID).stream()
                    .filter(t -> "IDEACION_SUICIDA".equals(t.code())).findFirst().orElseThrow();
            assertThat(s.confidenceScore()).isEqualTo(80);
        }

        @Test
        @DisplayName("dim. emocional >= 40 → no sugiere IDEACION_SUICIDA")
        void noBelowThresholdNoIdeacion() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(50.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .doesNotContain("IDEACION_SUICIDA");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("suggest() — colapso comunicacional")
    class CommunicationCollapse {

        @Test
        @DisplayName("colapso activo sugiere VIOLENCIA_INTRAFAMILIAR")
        void suggestsViolenciaWhenCollapse() {
            FamilyLongitudinalState l = lts();
            l.setCommunicationCollapseActive(true);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("VIOLENCIA_INTRAFAMILIAR");
        }

        @Test
        @DisplayName("colapso activo sugiere CRISIS_PAREJA con confidence 70")
        void suggestsCrisisParejaWith70() {
            FamilyLongitudinalState l = lts();
            l.setCommunicationCollapseActive(true);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            TrajectorySuggestion s = service.suggest(FAM_ID).stream()
                    .filter(t -> "CRISIS_PAREJA".equals(t.code())).findFirst().orElseThrow();
            assertThat(s.confidenceScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("dim. comunicación < 40 sugiere CRIANZA_AUTORITARIA")
        void lowComunicacionSuggestsCrianza() {
            FamilyLongitudinalState l = lts();
            l.setDimComunicacion(35.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("CRIANZA_AUTORITARIA");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("suggest() — hábitos y tiempos")
    class HabitsAndTime {

        @Test
        @DisplayName("hábitos < 40 sugiere CONSUMO_ALCOHOL_ADULTO")
        void lowHabitsSuggestsAlcohol() {
            FamilyLongitudinalState l = lts();
            l.setDimHabitos(30.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("CONSUMO_ALCOHOL_ADULTO");
        }

        @Test
        @DisplayName("tiempos < 35 sugiere ABANDONO_ADULTO_MAYOR")
        void lowTimeSuggestsAbandono() {
            FamilyLongitudinalState l = lts();
            l.setDimTiempos(30.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("ABANDONO_ADULTO_MAYOR");
        }

        @Test
        @DisplayName("tiempos >= 35 no sugiere ABANDONO_ADULTO_MAYOR")
        void aboveThresholdNoAbandono() {
            FamilyLongitudinalState l = lts();
            l.setDimTiempos(40.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .doesNotContain("ABANDONO_ADULTO_MAYOR");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("suggest() — señales sistémicas")
    class SystemicSignals {

        @Test
        @DisplayName("crisis reciente (≥1 en 30d) sugiere VIOLENCIA_INTRAFAMILIAR con confidence 70")
        void recentCrisisSuggestsViolencia() {
            FamilyLongitudinalState l = lts();
            l.setCrisisCount30d(2);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            TrajectorySuggestion s = service.suggest(FAM_ID).stream()
                    .filter(t -> "VIOLENCIA_INTRAFAMILIAR".equals(t.code())).findFirst().orElseThrow();
            assertThat(s.confidenceScore()).isEqualTo(70);
        }

        @Test
        @DisplayName("≥3 deterioros consecutivos sugiere DESEMPLEO_PROLONGADO")
        void sustainedDeteriorationSuggestsDesempleo() {
            FamilyLongitudinalState l = lts();
            l.setConsecutiveDeteriorations(4);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("DESEMPLEO_PROLONGADO");
        }

        @Test
        @DisplayName("ICF < 35 sugiere RUPTURA_GENERACIONAL")
        void veryLowIcfSuggestsRuptura() {
            FamilyLongitudinalState l = lts();
            l.setIcfCurrent(30.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("RUPTURA_GENERACIONAL");
        }

        @Test
        @DisplayName("riesgo CRITICO + tendencia deteriorante → CRISIS_PAREJA con confidence 85")
        void criticoDeterioratingGivesHighConfidence() {
            FamilyLongitudinalState l = lts();
            l.setCurrentRiskLevel("CRITICO");
            l.setRiskTrend("DETERIORATING");
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            TrajectorySuggestion s = service.suggest(FAM_ID).stream()
                    .filter(t -> "CRISIS_PAREJA".equals(t.code())).findFirst().orElseThrow();
            assertThat(s.confidenceScore()).isEqualTo(85);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("suggest() — filtros y límites")
    class FiltersAndLimits {

        @Test
        @DisplayName("máximo 5 sugerencias aunque haya más señales activas")
        void limitsResultTo5() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(20.0);
            l.setDimComunicacion(20.0);
            l.setDimHabitos(20.0);
            l.setDimTiempos(20.0);
            l.setCommunicationCollapseActive(true);
            l.setCrisisCount30d(3);
            l.setConsecutiveDeteriorations(5);
            l.setCurrentRiskLevel("CRITICO");
            l.setRiskTrend("DETERIORATING");
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID)).hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("sugerencias ordenadas por confidenceScore descendente")
        void sortedByConfidenceDesc() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(25.0);
            l.setCurrentRiskLevel("CRITICO");
            l.setRiskTrend("DETERIORATING");
            l.setCommunicationCollapseActive(true);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            List<TrajectorySuggestion> result = service.suggest(FAM_ID);
            for (int i = 0; i < result.size() - 1; i++) {
                assertThat(result.get(i).confidenceScore())
                        .isGreaterThanOrEqualTo(result.get(i + 1).confidenceScore());
            }
        }

        @Test
        @DisplayName("no sugiere trayectoria ya asignada (status != CLOSED)")
        void skipsAlreadyAssigned() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(25.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            // Simular IDEACION_SUICIDA ya asignada y activa
            FamilyRiskTrajectory existing = new FamilyRiskTrajectory();
            existing.setStatus(TrajectoryStatus.IN_PROGRESS);
            RiskTrajectory rt = trajectory("IDEACION_SUICIDA", "Ideación suicida", RiskMacrodomain.SALUD_MENTAL);
            existing.setTrajectory(rt);
            when(familyTrajectoryRepo.findByFamilyId(FAM_ID)).thenReturn(List.of(existing));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .doesNotContain("IDEACION_SUICIDA");
        }

        @Test
        @DisplayName("sugiere trayectoria si está en estado CLOSED (ya terminada)")
        void includesClosedTrajectories() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(25.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            // IDEACION_SUICIDA cerrada → se puede volver a sugerir
            FamilyRiskTrajectory closed = new FamilyRiskTrajectory();
            closed.setStatus(TrajectoryStatus.CLOSED);
            RiskTrajectory rt = trajectory("IDEACION_SUICIDA", "Ideación suicida", RiskMacrodomain.SALUD_MENTAL);
            closed.setTrajectory(rt);
            when(familyTrajectoryRepo.findByFamilyId(FAM_ID)).thenReturn(List.of(closed));

            assertThat(service.suggest(FAM_ID))
                    .extracting(TrajectorySuggestion::code)
                    .contains("IDEACION_SUICIDA");
        }

        @Test
        @DisplayName("código ausente del banco → se ignora sin error")
        void missingCodeInBankIsIgnored() {
            FamilyLongitudinalState l = lts();
            l.setDimEmociones(25.0);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            // Banco vacío → ninguna sugerencia generada
            when(trajectoryRepo.findByActiveTrue()).thenReturn(List.of());

            assertThatCode(() -> service.suggest(FAM_ID)).doesNotThrowAnyException();
            assertThat(service.suggest(FAM_ID)).isEmpty();
        }

        @Test
        @DisplayName("no duplica el mismo código en la lista de sugerencias")
        void noDuplicateCodes() {
            FamilyLongitudinalState l = lts();
            // Colapso + crisis reciente → VIOLENCIA_INTRAFAMILIAR por dos caminos
            l.setCommunicationCollapseActive(true);
            l.setCrisisCount30d(2);
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            List<TrajectorySuggestion> result = service.suggest(FAM_ID);
            long uniqueCodes = result.stream().map(TrajectorySuggestion::code).distinct().count();
            assertThat(uniqueCodes).isEqualTo(result.size());
        }

        @Test
        @DisplayName("familia sana → lista vacía (no supera ningún umbral)")
        void healthyFamilyReturnsEmpty() {
            FamilyLongitudinalState l = lts(); // todo en 60, sin señales
            when(ltsRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.of(l));

            assertThat(service.suggest(FAM_ID)).isEmpty();
        }
    }
}
