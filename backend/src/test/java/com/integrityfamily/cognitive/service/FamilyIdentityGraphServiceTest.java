package com.integrityfamily.cognitive.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.MemberRelationEdge.DynamicType;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyIdentityGraphService — Grafo Relacional y Roles Sistémicos")
class FamilyIdentityGraphServiceTest {

    @Mock MemberRelationEdgeRepository edgeRepository;
    @Mock MemberRepository             memberRepository;
    @Mock ReflectionRepository         reflectionRepository;
    @Mock EvaluationRepository         evaluationRepository;
    @Mock FamilyRepository             familyRepository;

    @InjectMocks FamilyIdentityGraphService graphService;

    private Family       family;
    private FamilyMember memberA;
    private FamilyMember memberB;
    private Evaluation   evaluation;

    @BeforeEach
    void setUp() {
        family  = Family.builder().id(1L).name("Familia Test").build();
        memberA = FamilyMember.builder().id(10L).fullName("María López").firstName("María")
                .role("MADRE").active(true).family(family).build();
        memberB = FamilyMember.builder().id(20L).fullName("Pedro López").firstName("Pedro")
                .role("PADRE").active(true).family(family).build();
        evaluation = Evaluation.builder()
                .id(100L).family(family).icf(65.0).riskLevel("MODERADO")
                .hasCrisis(false).build();

        Mockito.lenient().when(familyRepository.getReferenceById(1L)).thenReturn(family);
        Mockito.lenient().when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of());
        Mockito.lenient().when(edgeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        Mockito.lenient().when(edgeRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));
        // findByFamilyId is only used in getSnapshot(), not in updateGraph() — make lenient
        Mockito.lenient().when(edgeRepository.findByFamilyId(anyLong())).thenReturn(List.of());
    }

    // ─── Familia con un solo miembro ─────────────────────────────────────────

    @Test
    @DisplayName("Familia con 1 miembro activo → snapshot vacío, sin aristas")
    void singleMember_emptyGraph() {
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberA));

        FamilyIdentityGraphService.GraphSnapshot snapshot = graphService.updateGraph(1L, evaluation);

        assertThat(snapshot.totalDyads()).isZero();
        assertThat(snapshot.edges()).isEmpty();
        assertThat(snapshot.summary()).contains("un solo miembro");
    }

    // ─── Creación de arista para dos miembros ────────────────────────────────

    @Test
    @DisplayName("Familia con 2 miembros → se crea 1 díada")
    void twoMembers_createOneDyad() {
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberA, memberB));
        when(edgeRepository.findPair(eq(1L), eq(10L), eq(20L))).thenReturn(Optional.empty());

        FamilyIdentityGraphService.GraphSnapshot snapshot = graphService.updateGraph(1L, evaluation);

        // Debe haberse guardado exactamente 1 arista
        verify(edgeRepository, times(1)).saveAll(any());
        // snapshot se construye a partir de updatedEdges — debe tener la díada creada
        assertThat(snapshot.totalDyads()).isEqualTo(1);
    }

    // ─── Clasificación de relación ───────────────────────────────────────────

    @Test
    @DisplayName("PADRE + MADRE → tipo de relación SPOUSE")
    void padreYMadre_spouseRelationship() {
        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberA, memberB));
        when(edgeRepository.findPair(any(), any(), any())).thenReturn(Optional.empty());

        graphService.updateGraph(1L, evaluation);

        ArgumentCaptor<List<MemberRelationEdge>> captor = ArgumentCaptor.forClass(List.class);
        verify(edgeRepository).saveAll(captor.capture());

        List<MemberRelationEdge> edges = captor.getValue();
        assertThat(edges).hasSize(1);
        assertThat(edges.get(0).getRelationshipType()).isEqualTo("SPOUSE");
    }

    // ─── Clasificación dinámica ──────────────────────────────────────────────

    @Test
    @DisplayName("Crisis activa → tensión alta → dinámica CONFLICTIVE")
    void crisisEval_conflictiveDynamic() {
        Evaluation crisisEval = Evaluation.builder()
                .id(200L).family(family).icf(38.0).riskLevel("CRÍTICO")
                .hasCrisis(true).build();

        // Arista existente con tensión ya alta — exponential smoothing (α=0.35) sobre 80 → ≥80
        MemberRelationEdge existingEdge = MemberRelationEdge.builder()
                .id(1L).family(family).memberA(memberA).memberB(memberB)
                .cohesionScore(40.0).tensionScore(80.0).communicationScore(40.0)
                .relationshipType("SPOUSE").build();

        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberA, memberB));
        when(edgeRepository.findPair(any(), any(), any())).thenReturn(Optional.of(existingEdge));

        graphService.updateGraph(1L, crisisEval);

        ArgumentCaptor<List<MemberRelationEdge>> captor = ArgumentCaptor.forClass(List.class);
        verify(edgeRepository).saveAll(captor.capture());

        MemberRelationEdge edge = captor.getValue().get(0);
        assertThat(edge.getDynamicType()).isEqualTo(DynamicType.CONFLICTIVE);
        assertThat(edge.getTensionScore()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("ICF alto sin crisis → cohesión alta → dinámica SUPPORTIVE")
    void highIcfNoCrisis_supportiveDynamic() {
        Evaluation goodEval = Evaluation.builder()
                .id(300L).family(family).icf(82.0).riskLevel("BAJO")
                .hasCrisis(false).build();

        // Arista existente con cohesión ya alta — blend(80, 49.2, 0.35)=69.2 ≥ 65 → SUPPORTIVE
        MemberRelationEdge existingEdge = MemberRelationEdge.builder()
                .id(2L).family(family).memberA(memberA).memberB(memberB)
                .cohesionScore(80.0).tensionScore(20.0).communicationScore(70.0)
                .relationshipType("SPOUSE").build();

        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberA, memberB));
        when(edgeRepository.findPair(any(), any(), any())).thenReturn(Optional.of(existingEdge));

        graphService.updateGraph(1L, goodEval);

        ArgumentCaptor<List<MemberRelationEdge>> captor = ArgumentCaptor.forClass(List.class);
        verify(edgeRepository).saveAll(captor.capture());

        MemberRelationEdge edge = captor.getValue().get(0);
        assertThat(edge.getDynamicType()).isEqualTo(DynamicType.SUPPORTIVE);
        assertThat(edge.getCohesionScore()).isGreaterThan(50.0);
    }

    // ─── Health score ────────────────────────────────────────────────────────

    @Test
    @DisplayName("healthScore() refleja correctamente cohesión, comunicación y tensión")
    void healthScore_calculatesCorrectly() {
        MemberRelationEdge edge = MemberRelationEdge.builder()
                .cohesionScore(80.0).communicationScore(70.0).tensionScore(20.0).build();

        // healthScore = (80*0.4) + (70*0.4) + ((100-20)*0.2) = 32 + 28 + 16 = 76
        assertThat(edge.healthScore()).isEqualTo(76.0);
    }

    @Test
    @DisplayName("healthScore() está acotado entre 0 y 100")
    void healthScore_isBounded() {
        MemberRelationEdge perfect = MemberRelationEdge.builder()
                .cohesionScore(100.0).communicationScore(100.0).tensionScore(0.0).build();
        MemberRelationEdge worst = MemberRelationEdge.builder()
                .cohesionScore(0.0).communicationScore(0.0).tensionScore(100.0).build();

        assertThat(perfect.healthScore()).isEqualTo(100.0);
        assertThat(worst.healthScore()).isEqualTo(0.0);
    }

    // ─── Tendencia en arista existente ───────────────────────────────────────

    @Test
    @DisplayName("Arista existente con ICF mejorando → evolutionTrend IMPROVING")
    void existingEdge_icfImproving_trendImproving() {
        // Arista ya existente con cohesión baja (30) — blend(30, 49.2, 0.35)=36.72 → delta=6.72 > 3 → IMPROVING
        MemberRelationEdge existing = MemberRelationEdge.builder()
                .id(1L).family(family).memberA(memberA).memberB(memberB)
                .relationshipType("SPOUSE").dynamicType(DynamicType.BALANCED)
                .cohesionScore(30.0).tensionScore(35.0).communicationScore(50.0)
                .evolutionTrend("STABLE").fromEvaluationId(99L).build();

        // Nueva eval con ICF 82 → cohesión nueva alta
        Evaluation goodEval = Evaluation.builder()
                .id(101L).family(family).icf(82.0).riskLevel("BAJO").hasCrisis(false).build();

        when(memberRepository.findByFamilyId(1L)).thenReturn(List.of(memberA, memberB));
        when(edgeRepository.findPair(eq(1L), eq(10L), eq(20L))).thenReturn(Optional.of(existing));

        graphService.updateGraph(1L, goodEval);

        ArgumentCaptor<List<MemberRelationEdge>> captor = ArgumentCaptor.forClass(List.class);
        verify(edgeRepository).saveAll(captor.capture());

        MemberRelationEdge updated = captor.getValue().get(0);
        assertThat(updated.getEvolutionTrend()).isEqualTo("IMPROVING");
    }
}
