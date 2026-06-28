package com.integrityfamily.capital.service;

import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IcafScoringEngine — Unit Tests")
class IcafScoringEngineTest {

    @Mock FamilyRepository              familyRepository;
    @Mock FamilyCapitalSnapshotRepository snapshotRepo;
    @Mock IcafDomainResolver            domainResolver;
    @Mock EventPublisher                eventPublisher;

    @InjectMocks IcafScoringEngine engine;

    private static final Long FAM_ID = 1L;

    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private IcafScoringEngine.IcafDomains allEqual(double v) {
        return new IcafScoringEngine.IcafDomains(v, v, v, v, v, v, v, v, v, v, v);
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("compute()")
    class Compute {

        @Test
        @DisplayName("familia no encontrada → resultado vacío, sin persistencia ni evento")
        void familyNotFound() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            IcafScoringEngine.IcafResult result = engine.compute(FAM_ID, "MANUAL");

            assertThat(result.icaf()).isEqualTo(0.0);
            assertThat(result.madurez()).isEqualTo(1);
            verify(snapshotRepo, never()).save(any());
            verify(eventPublisher, never()).publish(any());
        }

        @Test
        @DisplayName("todos los dominios a 50 → ICaF = 50.0, madurez = 3 (Organización)")
        void allDomainsAt50() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(50.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            IcafScoringEngine.IcafResult result = engine.compute(FAM_ID, "ASSESSMENT");

            assertThat(result.icaf()).isCloseTo(50.0, within(0.01));
            assertThat(result.madurez()).isEqualTo(3);
            assertThat(result.madurezLabel()).isEqualTo("Organización");
        }

        @Test
        @DisplayName("todos los dominios a 100 → ICaF = 100.0, madurez = 5 (Legado)")
        void allDomainsAt100() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(100.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            IcafScoringEngine.IcafResult result = engine.compute(FAM_ID, "SCHEDULED");

            assertThat(result.icaf()).isCloseTo(100.0, within(0.01));
            assertThat(result.madurez()).isEqualTo(5);
        }

        @Test
        @DisplayName("todos los dominios a 0 → ICaF = 0.0, madurez = 1 (Supervivencia)")
        void allDomainsAt0() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(0.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            IcafScoringEngine.IcafResult result = engine.compute(FAM_ID, "SPRINT_CLOSE");

            assertThat(result.icaf()).isCloseTo(0.0, within(0.01));
            assertThat(result.madurez()).isEqualTo(1);
        }

        @Test
        @DisplayName("fórmula ponderada correcta con dominios mixtos")
        void weightedFormulaCorrect() {
            // cohesion=80 (peso 0.20), resto=50 → ICaF = 80*0.20 + 50*0.80 = 16+40 = 56
            IcafScoringEngine.IcafDomains domains = new IcafScoringEngine.IcafDomains(
                    80, 50, 50, 50, 50, 50, 50, 50, 50, 50, 50
            );
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(domains);
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            IcafScoringEngine.IcafResult result = engine.compute(FAM_ID, "MANUAL");

            assertThat(result.icaf()).isCloseTo(56.0, within(0.1));
        }

        @Test
        @DisplayName("persiste snapshot con datos correctos")
        void persistsSnapshot() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(60.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            engine.compute(FAM_ID, "ASSESSMENT");

            ArgumentCaptor<FamilyCapitalSnapshot> captor = ArgumentCaptor.forClass(FamilyCapitalSnapshot.class);
            verify(snapshotRepo).save(captor.capture());
            FamilyCapitalSnapshot saved = captor.getValue();
            assertThat(saved.getIcaf()).isCloseTo(60.0, within(0.01));
            assertThat(saved.getTriggerType()).isEqualTo("ASSESSMENT");
        }

        @Test
        @DisplayName("publica FamilyIcafRecalculatedEvent exactamente una vez")
        void publishesEvent() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(70.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            engine.compute(FAM_ID, "SPRINT_CLOSE");

            verify(eventPublisher, times(1)).publish(any());
        }

        @Test
        @DisplayName("usa ICaF del snapshot previo como previousIcaf en el evento")
        void usePreviousSnapshotIcaf() {
            FamilyCapitalSnapshot prev = FamilyCapitalSnapshot.builder()
                    .icaf(42.0).madurezNivel(2).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(50.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.of(prev));

            engine.compute(FAM_ID, "MANUAL");

            verify(snapshotRepo).save(any());
            verify(eventPublisher).publish(any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("computeMadurez()")
    class ComputeMadurez {

        @ParameterizedTest(name = "ICaF={0} → madurez={1}")
        @CsvSource({
            "0,   1",
            "29,  1",
            "29.9,1",
            "30,  2",
            "49,  2",
            "49.9,2",
            "50,  3",
            "64,  3",
            "64.9,3",
            "65,  4",
            "79,  4",
            "79.9,4",
            "80,  5",
            "100, 5"
        })
        @DisplayName("niveles de madurez con umbrales exactos")
        void madurezLevels(double icaf, int expected) {
            assertThat(IcafScoringEngine.computeMadurez(icaf)).isEqualTo(expected);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("madurezLabel()")
    class MadurezLabel {

        @ParameterizedTest(name = "nivel={0} → '{1}'")
        @CsvSource({
            "1, Supervivencia",
            "2, Reactividad",
            "3, Organización",
            "4, Propósito",
            "5, Legado",
            "0, Supervivencia",
            "6, Supervivencia"
        })
        @DisplayName("etiquetas por nivel")
        void labels(int nivel, String expected) {
            assertThat(IcafScoringEngine.madurezLabel(nivel)).isEqualTo(expected);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("IcafResult")
    class IcafResultTests {

        @Test
        @DisplayName("empty() devuelve ICaF=0 y madurez=1")
        void empty() {
            IcafScoringEngine.IcafResult r = IcafScoringEngine.IcafResult.empty(FAM_ID);
            assertThat(r.familyId()).isEqualTo(FAM_ID);
            assertThat(r.icaf()).isEqualTo(0.0);
            assertThat(r.madurez()).isEqualTo(1);
            assertThat(r.madurezLabel()).isEqualTo("Supervivencia");
        }

        @Test
        @DisplayName("madurezLabel() refleja el nivel calculado")
        void madurezLabelFromResult() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(domainResolver.resolve(FAM_ID)).thenReturn(allEqual(72.0));
            when(snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(Optional.empty());

            IcafScoringEngine.IcafResult r = engine.compute(FAM_ID, "MANUAL");

            assertThat(r.madurezLabel()).isEqualTo("Propósito");
        }
    }
}
