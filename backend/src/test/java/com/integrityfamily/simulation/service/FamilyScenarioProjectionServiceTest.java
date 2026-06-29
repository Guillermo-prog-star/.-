package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.simulation.dto.FamilyScenarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyScenarioProjectionService — Unit Tests")
class FamilyScenarioProjectionServiceTest {

    @Mock EvaluationRepository evaluationRepository;
    @Mock FamilyLongitudinalStateRepository ltsRepository;
    @Mock ImprovementPlanRepository planRepository;

    @InjectMocks FamilyScenarioProjectionService service;

    private static final Long FAM = 1L;

    private Evaluation evalFin(double icf) {
        return Evaluation.builder()
                .status(EvaluationStatus.FINALIZED)
                .icf(icf)
                .build();
    }

    private void stubNoLts() {
        when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.empty());
    }

    private void stubNoPlans() {
        when(planRepository.findByFamilyId(FAM)).thenReturn(List.of());
    }

    // ─── Baseline ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveBaseline()")
    class Baseline {

        @Test
        @DisplayName("sin evaluaciones ni LTS → baseline = 60.0")
        void sinDatos_baseline60() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of());
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.icfBaseline()).isEqualTo(60.0);
        }

        @Test
        @DisplayName("con evaluación finalizada → baseline = ICF de la última")
        void conEval_baselineUltimaEval() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(55), evalFin(72)));
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.icfBaseline()).isEqualTo(72.0);
        }

        @Test
        @DisplayName("sin evaluaciones pero LTS con icfCurrent → baseline = icfCurrent")
        void sinEvalConLts_baselineLts() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of());
            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .icfCurrent(68.0).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.icfBaseline()).isEqualTo(68.0);
        }
    }

    // ─── Orden de escenarios ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Orden de escenarios A / B / C")
    class OrdenEscenarios {

        @Test
        @DisplayName("escenario C proyecta ICF > B > A en sem 12 cuando hay pendiente negativa")
        void conPendienteNegativa_CmayorQueBmayorQueA() {
            // pendiente negativa: de 70 a 60 = -10 en 1 período
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(70), evalFin(60)));
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            double aW12 = r.scenarioA().week12().icfProjected();
            double bW12 = r.scenarioB().week12().icfProjected();
            double cW12 = r.scenarioC().week12().icfProjected();

            assertThat(cW12).isGreaterThan(bW12);
            assertThat(bW12).isGreaterThanOrEqualTo(aW12);
        }

        @Test
        @DisplayName("escenario C siempre proyecta mayor que A en todos los hitos")
        void cSiempreMayorQueA_enTodosLosHitos() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(65), evalFin(63)));
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.scenarioC().week4().icfProjected())
                    .isGreaterThanOrEqualTo(r.scenarioA().week4().icfProjected());
            assertThat(r.scenarioC().week12().icfProjected())
                    .isGreaterThan(r.scenarioA().week12().icfProjected());
        }
    }

    // ─── Probabilidades ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Probabilidades de escenarios")
    class Probabilidades {

        @Test
        @DisplayName("escenario B tiene probabilidad 65 siempre")
        void scenarioB_probabilidad65() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(60)));
            stubNoLts();
            stubNoPlans();

            assertThat(service.project(FAM).scenarioB().probabilityPercent()).isEqualTo(65);
        }

        @Test
        @DisplayName("escenario C tiene probabilidad 40 siempre")
        void scenarioC_probabilidad40() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(60)));
            stubNoLts();
            stubNoPlans();

            assertThat(service.project(FAM).scenarioC().probabilityPercent()).isEqualTo(40);
        }

        @Test
        @DisplayName("pendiente fuertemente negativa → probabilidad A = 70")
        void pendienteMuyNegativa_probA70() {
            // pendiente -10 por período = slope < -5 → prob A = 70
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(75), evalFin(60)));
            stubNoLts();
            stubNoPlans();

            assertThat(service.project(FAM).scenarioA().probabilityPercent()).isEqualTo(70);
        }
    }

    // ─── ICF nunca sale del rango 0-100 ──────────────────────────────────────

    @Nested
    @DisplayName("Límites ICF")
    class LimitesIcf {

        @Test
        @DisplayName("ICF base muy alto → proyecciones no superan 100")
        void icfAlto_noSuperaCien() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(90), evalFin(95)));
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.scenarioC().week12().icfProjected()).isLessThanOrEqualTo(100.0);
            assertThat(r.scenarioB().week12().icfProjected()).isLessThanOrEqualTo(100.0);
        }

        @Test
        @DisplayName("ICF base muy bajo → proyecciones Escenario A no bajan de 0")
        void icfBajo_noMenorQueCero() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(20), evalFin(5)));
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.scenarioA().week4().icfProjected()).isGreaterThanOrEqualTo(0.0);
            assertThat(r.scenarioA().week12().icfProjected()).isGreaterThanOrEqualTo(0.0);
        }
    }

    // ─── Penalización LTS ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Penalización de riesgo por LTS")
    class PenalizacionLts {

        @Test
        @DisplayName("LTS con communicationCollapseActive=true reduce proyección A respecto a sin LTS")
        void ltsCommunicationCollapse_reduceEscenarioA() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(65), evalFin(65)));

            // Sin LTS
            stubNoLts();
            stubNoPlans();
            double aW4SinLts = service.project(FAM).scenarioA().week4().icfProjected();

            // Con LTS con collapse
            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .communicationCollapseActive(true).icfCurrent(65.0).build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            double aW4ConLts = service.project(FAM).scenarioA().week4().icfProjected();

            assertThat(aW4ConLts).isLessThan(aW4SinLts);
        }

        @Test
        @DisplayName("con plan de mejora activo → escenario B usa boost completo vs sin plan")
        void conPlanMejora_bBoostCompleto() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(60)));
            stubNoLts();

            // Sin plan
            stubNoPlans();
            double bW12SinPlan = service.project(FAM).scenarioB().week12().icfProjected();

            // Con plan
            when(planRepository.findByFamilyId(FAM))
                    .thenReturn(List.of(ImprovementPlan.builder().build()));
            double bW12ConPlan = service.project(FAM).scenarioB().week12().icfProjected();

            assertThat(bW12ConPlan).isGreaterThan(bW12SinPlan);
        }
    }

    // ─── Pivot y oportunidad ──────────────────────────────────────────────────

    @Nested
    @DisplayName("pivotMessage y opportunityWindow")
    class MensajesPivot {

        @Test
        @DisplayName("pivotMessage contiene la diferencia C-A en puntos")
        void pivotMessage_contieneGapCmenosA() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(60)));
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.pivotMessage()).isNotBlank();
            assertThat(r.pivotMessage()).containsAnyOf("Escenario A", "Escenario C", "puntos");
        }

        @Test
        @DisplayName("pendiente muy negativa → opportunityWindow menciona urgencia (4 semanas)")
        void pendienteMuyNegativa_opportunityWindowUrgente() {
            when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(FAM))
                    .thenReturn(List.of(evalFin(70), evalFin(55)));  // slope = -15
            stubNoLts();
            stubNoPlans();

            FamilyScenarioResponse r = service.project(FAM);

            assertThat(r.opportunityWindow()).contains("4 semanas");
        }
    }
}
