package com.integrityfamily.cognitive.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.NarrativeChapter.NarrativePhase;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NarrativeEvolutionEngine — Fases y Turning Points")
class NarrativeEvolutionEngineTest {

    @Mock NarrativeChapterRepository chapterRepository;
    @Mock EvaluationRepository       evaluationRepository;
    @Mock FamilyRepository           familyRepository;
    @Mock FamilyIdentityProfileRepository identityRepository;

    @InjectMocks NarrativeEvolutionEngine engine;

    private Family  family;
    private Evaluation evalBase;

    @BeforeEach
    void setUp() {
        family   = Family.builder().id(1L).name("Familia Test").build();
        evalBase = buildEval(100L, 55.0, "MODERADO", false);

        Mockito.lenient().when(familyRepository.getReferenceById(1L)).thenReturn(family);
        Mockito.lenient().when(chapterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Mockito.lenient().when(chapterRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ─── Fase: AWAKENING ────────────────────────────────────────────────────

    @Test
    @DisplayName("Primera evaluación → abre capítulo AWAKENING")
    void firstEval_opensAwakeningChapter() {
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(evalBase));
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.empty());
        // countChapters only called on phase change with open chapter — not here
        Mockito.lenient().when(chapterRepository.countChaptersByFamilyId(1L)).thenReturn(0);
        when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L)).thenReturn(List.of());

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, evalBase);

        assertThat(result.currentChapter()).isNotNull();
        assertThat(result.currentChapter().getPhase()).isEqualTo(NarrativePhase.AWAKENING);
        assertThat(result.currentChapter().getChapterNumber()).isEqualTo(1);
        assertThat(result.currentChapter().getTitle()).contains("Despertar");
    }

    // ─── Fase: CRISIS ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ICF < 40 → abre capítulo CRISIS")
    void lowIcf_opensCrisisChapter() {
        Evaluation crisisEval = buildEval(101L, 35.0, "CRÍTICO", false);

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(crisisEval));
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.empty());
        // countChapters and existsByPhase not called — ICF<40 triggers CRISIS immediately
        Mockito.lenient().when(chapterRepository.countChaptersByFamilyId(1L)).thenReturn(0);
        Mockito.lenient().when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L)).thenReturn(List.of());

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, crisisEval);

        assertThat(result.currentChapter().getPhase()).isEqualTo(NarrativePhase.CRISIS);
    }

    @Test
    @DisplayName("hasCrisis = true → abre capítulo CRISIS independientemente del ICF")
    void activeCrisisFlag_opensCrisisChapter() {
        Evaluation crisisEval = buildEval(102L, 60.0, "MODERADO", true);

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(crisisEval));
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.empty());
        // countChapters and existsByPhase not called — hasCrisis=true triggers CRISIS immediately
        Mockito.lenient().when(chapterRepository.countChaptersByFamilyId(1L)).thenReturn(0);
        Mockito.lenient().when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L)).thenReturn(List.of());

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, crisisEval);

        assertThat(result.currentChapter().getPhase()).isEqualTo(NarrativePhase.CRISIS);
    }

    // ─── Fase: CONSOLIDATION ─────────────────────────────────────────────────

    @Test
    @DisplayName("ICF ≥ 70 sostenido en 3 evaluaciones → CONSOLIDATION")
    void sustainedHighIcf_opensConsolidationChapter() {
        List<Evaluation> history = List.of(
                buildEval(1L, 71.0, "BAJO", false),
                buildEval(2L, 73.0, "BAJO", false),
                buildEval(3L, 75.0, "BAJO", false)
        );

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L)).thenReturn(history);
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.empty());
        // countChapters not called — no open chapter means no phase-change branch
        Mockito.lenient().when(chapterRepository.countChaptersByFamilyId(1L)).thenReturn(0);
        when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L)).thenReturn(List.of());

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, history.get(2));

        assertThat(result.currentChapter().getPhase()).isEqualTo(NarrativePhase.CONSOLIDATION);
    }

    // ─── Turning points ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Delta ICF ≥ 15 → turning point detectado")
    void bigIcfJump_detectsTurningPoint() {
        Evaluation first  = buildEval(1L, 45.0, "ALTO", false);
        Evaluation second = buildEval(2L, 62.0, "MODERADO", false); // +17 pts

        NarrativeChapter openChapter = NarrativeChapter.builder()
                .id(10L).family(family).chapterNumber(1)
                .title("Cap 1").phase(NarrativePhase.DISCOVERY)
                .icfAtOpen(45.0).turningPoint(false).build();

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(first, second));
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.of(openChapter));
        when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        // No phase change (still DISCOVERY) → countChapters not called
        Mockito.lenient().when(chapterRepository.countChaptersByFamilyId(1L)).thenReturn(1);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L)).thenReturn(List.of(openChapter));

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, second);

        assertThat(result.turningPointDetected()).isTrue();
    }

    @Test
    @DisplayName("Delta ICF < 15 → NO turning point")
    void smallIcfChange_noTurningPoint() {
        Evaluation first  = buildEval(1L, 55.0, "MODERADO", false);
        Evaluation second = buildEval(2L, 60.0, "MODERADO", false); // +5 pts

        NarrativeChapter openChapter = NarrativeChapter.builder()
                .id(10L).family(family).chapterNumber(1)
                .title("Cap 1").phase(NarrativePhase.DISCOVERY)
                .icfAtOpen(55.0).turningPoint(false).build();

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(first, second));
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.of(openChapter));
        when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L)).thenReturn(List.of(openChapter));

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, second);

        assertThat(result.turningPointDetected()).isFalse();
    }

    // ─── Transición de fase abre nuevo capítulo ──────────────────────────────

    @Test
    @DisplayName("Cambio de fase → cierra capítulo anterior y abre uno nuevo")
    void phaseChange_closesOldChapterAndOpensNew() {
        // Estaba en DISCOVERY; nueva eval pone en CRISIS
        Evaluation crisisEval = buildEval(99L, 32.0, "CRÍTICO", false);

        NarrativeChapter openChapter = NarrativeChapter.builder()
                .id(5L).family(family).chapterNumber(1)
                .title("Cap 1: Discovery").phase(NarrativePhase.DISCOVERY)
                .icfAtOpen(60.0).turningPoint(false).build();

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(crisisEval));
        when(chapterRepository.findOpenChapterByFamilyId(1L)).thenReturn(Optional.of(openChapter));
        // ICF=32 < 40 → CRISIS resolves before existsByPhase is checked
        Mockito.lenient().when(chapterRepository.existsByFamilyIdAndPhase(1L, NarrativePhase.CRISIS)).thenReturn(false);
        when(chapterRepository.countChaptersByFamilyId(1L)).thenReturn(2);
        when(chapterRepository.findByFamilyIdOrderByChapterNumberAsc(1L))
                .thenReturn(List.of(openChapter));

        NarrativeEvolutionEngine.NarrativeSnapshot result = engine.evolve(1L, crisisEval);

        // El capítulo anterior debe haberse guardado con closedAt
        ArgumentCaptor<NarrativeChapter> captor = ArgumentCaptor.forClass(NarrativeChapter.class);
        verify(chapterRepository, atLeastOnce()).save(captor.capture());

        List<NarrativeChapter> saved = captor.getAllValues();
        boolean hasClosed = saved.stream().anyMatch(c -> c.getClosedAt() != null);
        assertThat(hasClosed).isTrue();

        assertThat(result.currentChapter().getPhase()).isEqualTo(NarrativePhase.CRISIS);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Evaluation buildEval(Long id, double icf, String risk, boolean hasCrisis) {
        return Evaluation.builder()
                .id(id).family(family).icf(icf)
                .riskLevel(risk).hasCrisis(hasCrisis)
                .status(EvaluationStatus.FINALIZED)
                .finalizedAt(LocalDateTime.now())
                .build();
    }
}
