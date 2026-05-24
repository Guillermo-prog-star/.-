package com.integrityfamily.cognitive.controller;

import com.integrityfamily.cognitive.service.*;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.FamilyMemory.MemoryType;
import com.integrityfamily.domain.NarrativeChapter.NarrativePhase;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.security.FamilySecurityEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración para CognitiveController.
 *
 * Estrategia: @SpringBootTest + @AutoConfigureMockMvc con todos los servicios
 * mockeados para evitar acceso a DB. Cubre:
 *   1. Seguridad (unauthenticated → 401, unauthorized → 403, authorized → 200)
 *   2. Forma del payload para cada endpoint
 *   3. Orden de ejecución del pipeline en /bootstrap
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CognitiveController — Seguridad y Contratos de Respuesta")
public class CognitiveControllerTest {

    @Autowired MockMvc mockMvc;

    // ─── Security ───────────────────────────────────────────────────────────
    @MockBean(name = "familySecurity") FamilySecurityEvaluator familySecurity;

    // ─── Servicios del sistema cognitivo ────────────────────────────────────
    @MockBean FamilyMemoryService       familyMemoryService;
    @MockBean FamilyReflectionService   familyReflectionService;
    @MockBean NarrativeEvolutionEngine  narrativeEvolutionEngine;
    @MockBean FamilyIdentityGraphService familyIdentityGraphService;
    @MockBean FamilySkillEngine         familySkillEngine;

    // ─── Repositorios ────────────────────────────────────────────────────────
    @MockBean FamilyMemoryRepository           memoryRepository;
    @MockBean FamilyIdentityProfileRepository  identityRepository;
    @MockBean LearnedSkillRepository           skillRepository;
    @MockBean NarrativeChapterRepository       chapterRepository;
    @MockBean MemberRelationEdgeRepository     edgeRepository;
    @MockBean MemberRepository                 memberRepository;
    @MockBean EvaluationRepository             evaluationRepository;

    // ─── Fixtures compartidos ────────────────────────────────────────────────
    private Family family;
    private FamilyIdentityProfile profile;
    private NarrativeChapter chapter;
    private NarrativeEvolutionEngine.NarrativeSnapshot narrativeSnap;
    private FamilyIdentityGraphService.GraphSnapshot graphSnap;
    private FamilyMemoryService.CognitiveContext cogCtx;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Test Family").build();

        profile = FamilyIdentityProfile.builder()
                .id(1L).family(family)
                .evolutionStage("INITIAL").communicationStyle("DIRECT")
                .conflictStyle("AVOIDANT").emotionalExpression("MEDIUM")
                .adaptabilityIndex(0.6).completedCycles(1)
                .identityNarrative("Una familia en construcción.")
                .build();

        chapter = NarrativeChapter.builder()
                .id(1L).family(family).chapterNumber(1)
                .title("Despertar Inicial").phase(NarrativePhase.AWAKENING)
                .icfAtOpen(55.0).turningPoint(false).build();

        narrativeSnap = new NarrativeEvolutionEngine.NarrativeSnapshot(
                1L, List.of(chapter), chapter, false, "Historia en 1 capítulo."
        );

        graphSnap = new FamilyIdentityGraphService.GraphSnapshot(
                1L, List.of(), List.of(), Map.of(),
                0.0, 0.0, 0L, "Grafo vacío — familia con un solo miembro."
        );

        cogCtx = new FamilyMemoryService.CognitiveContext(
                1L, List.of(), List.of(), profile
        );

        // Default stubs — sobrescribibles en tests individuales
        lenient().when(familySecurity.check(1L)).thenReturn(true);
        lenient().when(familySecurity.check(2L)).thenReturn(false);
        lenient().when(familyMemoryService.buildCognitiveContext(1L)).thenReturn(cogCtx);
        lenient().when(narrativeEvolutionEngine.getSnapshot(1L)).thenReturn(narrativeSnap);
        lenient().when(familyIdentityGraphService.getSnapshot(1L)).thenReturn(graphSnap);
        lenient().when(memoryRepository
                .findByFamilyIdAndMemoryTypeOrderByImportanceScoreDesc(1L, MemoryType.PROCEDURAL))
                .thenReturn(List.of());
        lenient().when(memberRepository.findByFamilyId(1L)).thenReturn(List.of());
        lenient().when(memoryRepository.findActiveMemoriesByFamilyId(eq(1L), any()))
                .thenReturn(List.of());
        lenient().when(identityRepository.findByFamilyId(1L)).thenReturn(Optional.of(profile));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. Seguridad — unauthenticated (Spring devuelve 403 sin entry point custom)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /snapshot sin autenticación → 403")
    void snapshot_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/snapshot"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /narrative sin autenticación → 403")
    void narrative_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/narrative"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /reflect sin autenticación → 403")
    void reflect_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/cognitive/1/reflect"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /bootstrap sin autenticación → 403")
    void bootstrap_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/cognitive/1/bootstrap"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. Seguridad — familia ajena → 403
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /snapshot sin permiso de familia → 403")
    void snapshot_noFamilyPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/cognitive/2/snapshot"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /bootstrap sin permiso de familia → 403")
    void bootstrap_noFamilyPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/cognitive/2/bootstrap"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. GET /snapshot — contrato de respuesta
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /snapshot con permiso → 200 con identityProfile, graphSummary y generatedAt")
    void snapshot_authorized_returns200WithExpectedFields() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.familyId").value(1))
                .andExpect(jsonPath("$.data.identityProfile.evolutionStage").value("INITIAL"))
                .andExpect(jsonPath("$.data.currentChapter.title").value("Despertar Inicial"))
                .andExpect(jsonPath("$.data.graphSummary").exists())
                .andExpect(jsonPath("$.data.generatedAt").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /snapshot sin capítulo activo → currentChapter es null")
    void snapshot_noCurrentChapter_currentChapterIsNull() throws Exception {
        NarrativeEvolutionEngine.NarrativeSnapshot emptyNarrative =
                new NarrativeEvolutionEngine.NarrativeSnapshot(
                        1L, List.of(), null, false, "Sin historia aún."
                );
        when(narrativeEvolutionEngine.getSnapshot(1L)).thenReturn(emptyNarrative);

        mockMvc.perform(get("/api/cognitive/1/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentChapter").doesNotExist());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. GET /narrative — lista de capítulos
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /narrative → 200 con lista de capítulos y currentPhase")
    void narrative_authorized_returnsChapterList() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/narrative"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chapters").isArray())
                .andExpect(jsonPath("$.data.chapters[0].title").value("Despertar Inicial"))
                .andExpect(jsonPath("$.data.currentPhase").value("AWAKENING"))
                .andExpect(jsonPath("$.data.totalChapters").value(1));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  5. GET /graph — snapshot del grafo
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /graph → 200 con dyads, cohesionDensity y healthy=false cuando cohesión < 65")
    void graph_authorized_returnsGraphFields() throws Exception {
        // cohesionDensity=0.0 → isHealthy()=false (threshold is ≥65)
        mockMvc.perform(get("/api/cognitive/1/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dyads").isArray())
                .andExpect(jsonPath("$.data.cohesionDensity").value(0.0))
                .andExpect(jsonPath("$.data.healthy").value(false));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /graph → healthy=true cuando cohesión ≥ 65 y tensión ≤ 35")
    void graph_highCohesionLowTension_healthyIsTrue() throws Exception {
        FamilyIdentityGraphService.GraphSnapshot healthySnap =
                new FamilyIdentityGraphService.GraphSnapshot(
                        1L, List.of(), List.of(), Map.of(),
                        70.0, 20.0, 0L, "Grafo saludable."
                );
        when(familyIdentityGraphService.getSnapshot(1L)).thenReturn(healthySnap);

        mockMvc.perform(get("/api/cognitive/1/graph"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.healthy").value(true));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  6. GET /memory — memorias por tipo
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /memory → 200 con listas episodic, semantic, procedural vacías")
    void memory_authorized_returnsThreeEmptyLists() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.episodic").isArray())
                .andExpect(jsonPath("$.data.semantic").isArray())
                .andExpect(jsonPath("$.data.procedural").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /memory con una memoria episódica → aparece en la lista episodic")
    void memory_withOneEpisodicMemory_appearsInEpisodicList() throws Exception {
        FamilyMemory m = FamilyMemory.builder()
                .id(42L).family(family).memoryType(MemoryType.EPISODIC)
                .semanticKey("test-key").content("Una sesión de evaluación.")
                .importanceScore(0.9).sourceType("EVALUATION")
                .createdAt(LocalDateTime.now()).build();

        when(memoryRepository.findActiveMemoriesByFamilyId(eq(1L), any()))
                .thenReturn(List.of(m));

        mockMvc.perform(get("/api/cognitive/1/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.episodic").isArray())
                .andExpect(jsonPath("$.data.episodic[0].content").value("Una sesión de evaluación."))
                .andExpect(jsonPath("$.data.episodic[0].importanceScore").value(0.9));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  7. GET /identity — perfil de identidad
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /identity → 200 con todos los campos del perfil")
    void identity_authorized_returnsFullProfile() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evolutionStage").value("INITIAL"))
                .andExpect(jsonPath("$.data.communicationStyle").value("DIRECT"))
                .andExpect(jsonPath("$.data.adaptabilityIndex").value(0.6))
                .andExpect(jsonPath("$.data.completedCycles").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /identity sin perfil existente → crea uno y lo retorna")
    void identity_noExistingProfile_createsAndReturns() throws Exception {
        when(identityRepository.findByFamilyId(1L)).thenReturn(Optional.empty());
        when(familyMemoryService.getOrCreateIdentityProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/cognitive/1/identity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evolutionStage").value("INITIAL"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  8a. GET /reflection/latest — última reflexión (read-only)
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /reflection/latest sin autenticación → 403")
    void reflectionLatest_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/cognitive/1/reflection/latest"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /reflection/latest → 200 invoca getLatest() y retorna reporte sin efectos secundarios")
    void reflectionLatest_authorized_invokesGetLatestAndReturns200() throws Exception {
        FamilyReflectionService.ReflectionReport report = buildMockReport();
        when(familyReflectionService.getLatest(1L)).thenReturn(report);

        mockMvc.perform(get("/api/cognitive/1/reflection/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.familyId").value(1))
                .andExpect(jsonPath("$.data.effectivenessLevel").value("MODERATE"))
                .andExpect(jsonPath("$.data.abandonmentLevel").value("LOW"))
                .andExpect(jsonPath("$.data.requiresUrgentAttention").value(false));

        // Debe llamar getLatest, NO reflect
        verify(familyReflectionService, times(1)).getLatest(1L);
        verify(familyReflectionService, never()).reflect(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /reflection/latest sin permiso de familia → 403")
    void reflectionLatest_noFamilyPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/cognitive/2/reflection/latest"))
                .andExpect(status().isForbidden());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  8b. POST /reflect — ciclo de reflexión con efectos secundarios
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /reflect → 200 invoca familyReflectionService.reflect() y retorna reporte")
    void reflect_authorized_invokesReflectionAndReturns200() throws Exception {
        FamilyReflectionService.ReflectionReport report = buildMockReport();
        when(familyReflectionService.reflect(1L)).thenReturn(report);

        mockMvc.perform(post("/api/cognitive/1/reflect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.familyId").value(1))
                .andExpect(jsonPath("$.data.effectivenessLevel").value("MODERATE"))
                .andExpect(jsonPath("$.data.abandonmentLevel").value("LOW"))
                .andExpect(jsonPath("$.data.requiresUrgentAttention").value(false));

        verify(familyReflectionService, times(1)).reflect(1L);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  9. POST /bootstrap — pipeline completo
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /bootstrap → ejecuta el pipeline en orden: graph → memory → consolidate → skills → narrative → reflect")
    void bootstrap_authorized_runsPipelineInOrder() throws Exception {
        Evaluation latestEval = Evaluation.builder()
                .id(10L).family(family).icf(65.0).riskLevel("MODERADO")
                .hasCrisis(false).status(EvaluationStatus.FINALIZED)
                .finalizedAt(LocalDateTime.now()).build();

        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of(latestEval));
        when(familyIdentityGraphService.updateGraph(1L, latestEval)).thenReturn(graphSnap);
        when(narrativeEvolutionEngine.evolve(1L, latestEval)).thenReturn(narrativeSnap);
        when(familyReflectionService.reflect(1L)).thenReturn(buildMockReport());

        mockMvc.perform(post("/api/cognitive/1/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.familyId").value(1));

        // Verificar que el pipeline se invocó completo y en orden
        var order = inOrder(
                familyIdentityGraphService, familyMemoryService,
                familySkillEngine, narrativeEvolutionEngine, familyReflectionService
        );
        order.verify(familyIdentityGraphService).updateGraph(1L, latestEval);
        order.verify(familyMemoryService).captureEvaluationMemory(latestEval);
        order.verify(familyMemoryService).consolidateSemanticPattern(1L, "evaluation-result");
        order.verify(familySkillEngine).analyze(1L, latestEval);
        order.verify(narrativeEvolutionEngine).evolve(1L, latestEval);
        order.verify(familyReflectionService).reflect(1L);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /bootstrap sin evaluaciones finalizadas → 500 con mensaje claro")
    void bootstrap_noFinalizedEvaluations_returns500() throws Exception {
        when(evaluationRepository.findByFamilyIdOrderByFinalizedAtAsc(1L))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/cognitive/1/bootstrap"))
                .andExpect(status().is5xxServerError());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private FamilyReflectionService.ReflectionReport buildMockReport() {
        FamilyReflectionService.InterventionEffectiveness effectiveness =
                new FamilyReflectionService.InterventionEffectiveness(
                        FamilyReflectionService.EffectivenessLevel.MODERATE,
                        3, -5.0, 0.75, 0.8,
                        "Efectividad moderada con tendencia a mejorar."
                );
        FamilyReflectionService.AbandonmentRisk risk =
                new FamilyReflectionService.AbandonmentRisk(
                        FamilyReflectionService.AbandonmentLevel.LOW,
                        List.of(), 20
                );
        return new FamilyReflectionService.ReflectionReport(
                1L, LocalDateTime.now(), effectiveness, risk,
                "Mantener consistencia en sesiones.", null
        );
    }
}
