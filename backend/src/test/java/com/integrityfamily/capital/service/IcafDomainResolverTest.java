package com.integrityfamily.capital.service;

import com.integrityfamily.capital.service.IcafScoringEngine.IcafDomains;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IcafDomainResolver — Unit Tests")
class IcafDomainResolverTest {

    @Mock FamilyLongitudinalStateRepository longitudinalRepo;
    @Mock FamilyCapitalSnapshotRepository   snapshotRepo;
    @Mock FamilyIcafAnswerRepository        icafAnswerRepo;
    @Mock IcafResilienciaEngine             resilienciaEngine;

    @InjectMocks IcafDomainResolver resolver;

    private static final Long FAM_ID         = 1L;
    private static final double DEFAULT      = 50.0;
    private static final double NO_ANSWERS   = 0.0;

    // ── helpers ──────────────────────────────────────────────────────────────

    private FamilyLongitudinalState stateWith(Double icfCurrent) {
        FamilyLongitudinalState s = new FamilyLongitudinalState();
        s.setIcfCurrent(icfCurrent);
        return s;
    }

    private void stubNoAnswers() {
        when(icafAnswerRepo.avgScoreByDomain(eq(FAM_ID), anyString())).thenReturn(NO_ANSWERS);
    }

    private void stubResiliencia(double value) {
        when(resilienciaEngine.compute(eq(FAM_ID), any())).thenReturn(value);
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("sin estado longitudinal")
    class NoState {

        @Test
        @DisplayName("→ todos los dominios en 50.0 (default)")
        void allDefaults() {
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.cohesion()).isEqualTo(DEFAULT);
            assertThat(d.confianza()).isEqualTo(DEFAULT);
            assertThat(d.resiliencia()).isEqualTo(DEFAULT);
            assertThat(d.comunicacion()).isEqualTo(DEFAULT);
            assertThat(d.autonomia()).isEqualTo(DEFAULT);
            assertThat(d.bienestar()).isEqualTo(DEFAULT);
            assertThat(d.proposito()).isEqualTo(DEFAULT);
            assertThat(d.integracion()).isEqualTo(DEFAULT);
            assertThat(d.emprendimiento()).isEqualTo(DEFAULT);
            assertThat(d.legado()).isEqualTo(DEFAULT);
            assertThat(d.madurezScore()).isEqualTo(DEFAULT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominio cohesion")
    class Cohesion {

        @Test
        @DisplayName("usa icfCurrent cuando disponible")
        void usesIcfCurrent() {
            FamilyLongitudinalState s = stateWith(75.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.cohesion()).isCloseTo(75.0, within(0.01));
        }

        @Test
        @DisplayName("icfCurrent null → default 50.0")
        void nullIcfCurrent() {
            FamilyLongitudinalState s = stateWith(null);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.cohesion()).isEqualTo(DEFAULT);
        }

        @Test
        @DisplayName("icfCurrent = 0 → default 50.0")
        void zeroIcfCurrent() {
            FamilyLongitudinalState s = stateWith(0.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.cohesion()).isEqualTo(DEFAULT);
        }

        @Test
        @DisplayName("icfCurrent > 100 → clamped a 100")
        void icfCurrentClamped() {
            FamilyLongitudinalState s = stateWith(120.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.cohesion()).isCloseTo(100.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominio confianza")
    class Confianza {

        @Test
        @DisplayName("con respuestas de cuestionario → normaliza avg 1-5 a 0-100")
        void fromQuestionnaire() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(icafAnswerRepo.avgScoreByDomain(FAM_ID, IcafQuestionnaireService.DOMAIN_CONFIANZA))
                    .thenReturn(3.0); // avg=3 → (3-1)/4*100 = 50
            when(icafAnswerRepo.avgScoreByDomain(FAM_ID, IcafQuestionnaireService.DOMAIN_BIENESTAR))
                    .thenReturn(NO_ANSWERS);
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.confianza()).isCloseTo(50.0, within(0.01));
        }

        @Test
        @DisplayName("avg=5 → confianza = 100.0")
        void maxQuestionnaire() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(icafAnswerRepo.avgScoreByDomain(FAM_ID, IcafQuestionnaireService.DOMAIN_CONFIANZA))
                    .thenReturn(5.0);
            when(icafAnswerRepo.avgScoreByDomain(FAM_ID, IcafQuestionnaireService.DOMAIN_BIENESTAR))
                    .thenReturn(NO_ANSWERS);
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.confianza()).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("sin respuestas + con dimComunicacion → fallback 85%")
        void fallbackToComunicacion() {
            FamilyLongitudinalState s = stateWith(60.0);
            s.setDimComunicacion(80.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.confianza()).isCloseTo(80.0 * 0.85, within(0.01));
        }

        @Test
        @DisplayName("sin respuestas + sin dimComunicacion → default 50.0")
        void fallbackDefault() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.confianza()).isEqualTo(DEFAULT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominio bienestar")
    class Bienestar {

        @Test
        @DisplayName("con respuestas de cuestionario → normaliza avg 1-5 a 0-100")
        void fromQuestionnaire() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            when(icafAnswerRepo.avgScoreByDomain(FAM_ID, IcafQuestionnaireService.DOMAIN_CONFIANZA))
                    .thenReturn(NO_ANSWERS);
            when(icafAnswerRepo.avgScoreByDomain(FAM_ID, IcafQuestionnaireService.DOMAIN_BIENESTAR))
                    .thenReturn(4.0); // (4-1)/4*100 = 75
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.bienestar()).isCloseTo(75.0, within(0.01));
        }

        @Test
        @DisplayName("sin respuestas + con dimEmociones → usa dimEmociones directamente")
        void fallbackToEmociones() {
            FamilyLongitudinalState s = stateWith(60.0);
            s.setDimEmociones(70.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.bienestar()).isCloseTo(70.0, within(0.01));
        }

        @Test
        @DisplayName("sin respuestas + sin dimEmociones → default 50.0")
        void fallbackDefault() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.bienestar()).isEqualTo(DEFAULT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominio comunicacion")
    class Comunicacion {

        @Test
        @DisplayName("usa dimComunicacion cuando disponible")
        void usesDimComunicacion() {
            FamilyLongitudinalState s = stateWith(60.0);
            s.setDimComunicacion(65.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.comunicacion()).isCloseTo(65.0, within(0.01));
        }

        @Test
        @DisplayName("dimComunicacion null → default 50.0")
        void nullFallback() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.comunicacion()).isEqualTo(DEFAULT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominio integracion")
    class Integracion {

        @ParameterizedTest(name = "consciousnessLevel={0} → base={1}")
        @CsvSource({
            "1, 90.0",
            "2, 75.0",
            "3, 60.0",
            "4, 40.0",
            "5, 25.0"
        })
        @DisplayName("base según consciousnessLevel (sin inactividad)")
        void baseByLevel(int level, double expected) {
            FamilyLongitudinalState s = stateWith(60.0);
            s.setConsciousnessLevel(level);
            s.setInactivityDays(0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.integracion()).isCloseTo(expected, within(0.01));
        }

        @Test
        @DisplayName("descuento por inactividad: 10 días con level=1 → 90 - 20 = 70")
        void inactivityDiscount() {
            FamilyLongitudinalState s = stateWith(60.0);
            s.setConsciousnessLevel(1);
            s.setInactivityDays(10);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.integracion()).isCloseTo(70.0, within(0.01));
        }

        @Test
        @DisplayName("descuento máximo de inactividad: 30 días level=2 → 75 - 30 = 45")
        void inactivityMaxCap() {
            FamilyLongitudinalState s = stateWith(60.0);
            s.setConsciousnessLevel(2);
            s.setInactivityDays(20); // 20*2=40 → cap a 30 → 75-30=45
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.integracion()).isCloseTo(45.0, within(0.01));
        }

        @Test
        @DisplayName("nivel null (no configurado) → nivel por defecto 3 → base 60.0")
        void nullLevelDefault() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.integracion()).isCloseTo(60.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominio madurezScore")
    class MadurezScore {

        @ParameterizedTest(name = "icfCurrent={0} → nivel={1} → score={2}")
        @CsvSource({
            "10.0, 1, 20.0",
            "35.0, 2, 40.0",
            "55.0, 3, 60.0",
            "70.0, 4, 80.0",
            "85.0, 5, 100.0"
        })
        @DisplayName("nivel ICaF → madurezScore = nivel × 20")
        void madurezScoreFromIcf(double icf, int nivel, double expectedScore) {
            FamilyLongitudinalState s = stateWith(icf);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.madurezScore()).isCloseTo(expectedScore, within(0.01));
        }

        @Test
        @DisplayName("icfCurrent null → default 50.0")
        void nullIcf() {
            FamilyLongitudinalState s = stateWith(null);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.madurezScore()).isEqualTo(DEFAULT);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("dominios en DEFAULT fijo")
    class DefaultDomains {

        @Test
        @DisplayName("autonomia, proposito, emprendimiento, legado → siempre 50.0")
        void pendingDomainsDefaultTo50() {
            FamilyLongitudinalState s = stateWith(60.0);
            when(longitudinalRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(s));
            stubNoAnswers();
            stubResiliencia(50.0);

            IcafDomains d = resolver.resolve(FAM_ID);

            assertThat(d.autonomia()).isEqualTo(DEFAULT);
            assertThat(d.proposito()).isEqualTo(DEFAULT);
            assertThat(d.emprendimiento()).isEqualTo(DEFAULT);
            assertThat(d.legado()).isEqualTo(DEFAULT);
        }
    }
}
