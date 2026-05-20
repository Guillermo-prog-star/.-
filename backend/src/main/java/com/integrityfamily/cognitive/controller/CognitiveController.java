package com.integrityfamily.cognitive.controller;

import com.integrityfamily.cognitive.dto.CognitiveDtos.*;
import com.integrityfamily.cognitive.service.*;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.FamilyMemory.MemoryType;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.cognitive.service.FamilySkillEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SDD Fases 1–5 — Controlador del Sistema Cognitivo Familiar.
 *
 * Expone los datos del motor cognitivo para el dashboard y el copiloto IA.
 * Todos los endpoints son de sólo lectura excepto POST /reflect (trigger manual).
 */
@Slf4j
@RestController
@RequestMapping("/api/cognitive")
@RequiredArgsConstructor
@Tag(name = "Sistema Cognitivo Familiar", description = "Memoria, narrativa, grafo de identidad y reflexión autónoma")
public class CognitiveController {

    private final FamilyMemoryService familyMemoryService;
    private final FamilyReflectionService familyReflectionService;
    private final NarrativeEvolutionEngine narrativeEvolutionEngine;
    private final FamilyIdentityGraphService familyIdentityGraphService;
    private final FamilySkillEngine familySkillEngine;
    private final FamilyMemoryRepository memoryRepository;
    private final FamilyIdentityProfileRepository identityRepository;
    private final LearnedSkillRepository skillRepository;
    private final NarrativeChapterRepository chapterRepository;
    private final MemberRelationEdgeRepository edgeRepository;
    private final MemberRepository memberRepository;
    private final EvaluationRepository evaluationRepository;

    // ─── 1. Snapshot completo ────────────────────────────────────────────────

    @GetMapping("/{familyId}/snapshot")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Snapshot cognitivo completo",
               description = "Devuelve identidad, capítulo actual, resumen de grafo, memorias recientes y skills activas en una sola llamada.")
    public ApiResponse<CognitiveSnapshotResponse> getSnapshot(@PathVariable Long familyId) {
        log.info("🧠 [COGNITIVE] Solicitando snapshot para familia ID: {}", familyId);

        FamilyMemoryService.CognitiveContext ctx = familyMemoryService.buildCognitiveContext(familyId);

        IdentityProfileDto identity = ctx.hasIdentity()
                ? mapIdentity(ctx.identityProfile())
                : emptyIdentity();

        NarrativeEvolutionEngine.NarrativeSnapshot narrative =
                narrativeEvolutionEngine.getSnapshot(familyId);

        CurrentChapterDto currentChapter = narrative.currentChapter() != null
                ? mapCurrentChapter(narrative.currentChapter())
                : null;

        long turningPoints = narrative.chapters().stream()
                .filter(c -> Boolean.TRUE.equals(c.getTurningPoint())).count();

        FamilyIdentityGraphService.GraphSnapshot graph =
                familyIdentityGraphService.getSnapshot(familyId);

        List<MemoryDto> recentMemories = ctx.recentEpisodes().stream()
                .map(this::mapMemory).toList();

        List<SkillDto> appliedSkills = memoryRepository
                .findByFamilyIdAndMemoryTypeOrderByImportanceScoreDesc(familyId, MemoryType.PROCEDURAL)
                .stream().limit(5)
                .map(m -> new SkillDto(m.getId(), m.getSemanticKey(), m.getContent(),
                        "GENERAL", 0.7, 0.0, 0))
                .toList();

        CognitiveSnapshotResponse response = new CognitiveSnapshotResponse(
                familyId,
                identity,
                currentChapter,
                narrative.totalChapters(),
                turningPoints,
                mapGraphSummary(graph),
                recentMemories,
                appliedSkills,
                narrative.storyArcSummary(),
                LocalDateTime.now()
        );

        return ApiResponse.ok(response);
    }

    // ─── 2. Narrativa ────────────────────────────────────────────────────────

    @GetMapping("/{familyId}/narrative")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Historia evolutiva familiar",
               description = "Todos los capítulos narrativos de la familia, ordenados cronológicamente.")
    public ApiResponse<NarrativeResponse> getNarrative(@PathVariable Long familyId) {
        log.info("📖 [COGNITIVE] Solicitando narrativa para familia ID: {}", familyId);

        NarrativeEvolutionEngine.NarrativeSnapshot snapshot =
                narrativeEvolutionEngine.getSnapshot(familyId);

        List<ChapterDto> chapters = snapshot.chapters().stream()
                .map(this::mapChapter).toList();

        long turningPoints = snapshot.chapters().stream()
                .filter(c -> Boolean.TRUE.equals(c.getTurningPoint())).count();

        return ApiResponse.ok(new NarrativeResponse(
                familyId,
                chapters,
                snapshot.currentPhase().name(),
                snapshot.totalChapters(),
                turningPoints,
                snapshot.storyArcSummary()
        ));
    }

    // ─── 3. Grafo de identidad ────────────────────────────────────────────────

    @GetMapping("/{familyId}/graph")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Grafo de dinámicas relacionales",
               description = "Díadas entre miembros con puntuaciones de cohesión, tensión y roles sistémicos.")
    public ApiResponse<GraphResponse> getGraph(@PathVariable Long familyId) {
        log.info("🕸️ [COGNITIVE] Solicitando grafo para familia ID: {}", familyId);

        FamilyIdentityGraphService.GraphSnapshot snapshot =
                familyIdentityGraphService.getSnapshot(familyId);

        List<FamilyMember> members = memberRepository.findByFamilyId(familyId);
        Map<Long, String> memberNames = new HashMap<>();
        members.forEach(m -> memberNames.put(m.getId(),
                m.getFirstName() != null ? m.getFirstName() : m.getFullName()));

        List<DyadDto> dyads = snapshot.edges().stream()
                .map(e -> mapDyad(e, memberNames)).toList();

        List<MemberRoleDto> roles = snapshot.systemRoles().entrySet().stream()
                .map(entry -> new MemberRoleDto(
                        entry.getKey(),
                        memberNames.getOrDefault(entry.getKey(), "—"),
                        entry.getValue()))
                .toList();

        return ApiResponse.ok(new GraphResponse(
                familyId,
                dyads,
                roles,
                snapshot.cohesionDensity(),
                snapshot.tensionDensity(),
                snapshot.conflictiveEdges(),
                snapshot.isHealthy(),
                snapshot.summary()
        ));
    }

    // ─── 4. Memoria cognitiva ─────────────────────────────────────────────────

    @GetMapping("/{familyId}/memory")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Memorias activas",
               description = "Episodios recientes, patrones semánticos consolidados y memorias procedurales (skills aplicadas).")
    public ApiResponse<MemoryResponse> getMemory(@PathVariable Long familyId) {
        log.info("💾 [COGNITIVE] Solicitando memorias para familia ID: {}", familyId);

        List<FamilyMemory> all = memoryRepository.findActiveMemoriesByFamilyId(familyId, LocalDateTime.now());

        List<MemoryDto> episodic = all.stream()
                .filter(m -> m.getMemoryType() == MemoryType.EPISODIC)
                .map(this::mapMemory).toList();
        List<MemoryDto> semantic = all.stream()
                .filter(m -> m.getMemoryType() == MemoryType.SEMANTIC)
                .map(this::mapMemory).toList();
        List<MemoryDto> procedural = all.stream()
                .filter(m -> m.getMemoryType() == MemoryType.PROCEDURAL)
                .map(this::mapMemory).toList();

        return ApiResponse.ok(new MemoryResponse(familyId, episodic, semantic, procedural));
    }

    // ─── 5. Identidad ────────────────────────────────────────────────────────

    @GetMapping("/{familyId}/identity")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Perfil de identidad familiar",
               description = "Estilo comunicativo, ciclos completados, etapa de evolución y narrativa identitaria.")
    public ApiResponse<IdentityProfileDto> getIdentity(@PathVariable Long familyId) {
        log.info("🪪 [COGNITIVE] Solicitando perfil identitario para familia ID: {}", familyId);

        FamilyIdentityProfile profile = identityRepository.findByFamilyId(familyId)
                .orElseGet(() -> familyMemoryService.getOrCreateIdentityProfile(familyId));

        return ApiResponse.ok(mapIdentity(profile));
    }

    // ─── 6. Reflexión ────────────────────────────────────────────────────────

    /**
     * GET: Devuelve la última reflexión calculada (caché o análisis read-only).
     * Usar para carga del dashboard — no tiene efectos secundarios de escritura.
     */
    @GetMapping("/{familyId}/reflection/latest")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Última reflexión autónoma (read-only)",
               description = "Devuelve la reflexión más reciente del sistema. Si no hay caché, realiza análisis sin persistir cambios.")
    public ApiResponse<ReflectionResponse> getLatestReflection(@PathVariable Long familyId) {
        log.info("📋 [COGNITIVE] Solicitando última reflexión para familia ID: {}", familyId);

        FamilyReflectionService.ReflectionReport report = familyReflectionService.getLatest(familyId);
        return ApiResponse.ok(mapReflectionReport(familyId, report));
    }

    /**
     * POST: Dispara un ciclo completo de reflexión autónoma (escribe lecciones y narrativa).
     * Usar sólo desde acciones explícitas del usuario o triggers automatizados.
     */
    @PostMapping("/{familyId}/reflect")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Ejecutar ciclo de reflexión autónoma",
               description = "Dispara la autoevaluación del sistema: efectividad, riesgo de abandono y lección aprendida.")
    public ApiResponse<ReflectionResponse> triggerReflection(@PathVariable Long familyId) {
        log.info("🪞 [COGNITIVE] Reflexión manual disparada para familia ID: {}", familyId);

        FamilyReflectionService.ReflectionReport report = familyReflectionService.reflect(familyId);
        return ApiResponse.ok(mapReflectionReport(familyId, report));
    }

    private ReflectionResponse mapReflectionReport(Long familyId,
                                                    FamilyReflectionService.ReflectionReport report) {
        return new ReflectionResponse(
                familyId,
                report.effectiveness().level().name(),
                report.effectiveness().evaluationCount(),
                report.effectiveness().icfTrend(),
                report.effectiveness().avgAdherence(),
                report.effectiveness().reflectionRate(),
                report.effectiveness().summary(),
                report.abandonmentRisk().level().name(),
                report.abandonmentRisk().signals(),
                report.abandonmentRisk().riskScore(),
                report.lessonLearned(),
                report.updatedNarrative(),
                report.requiresUrgentAttention(),
                report.generatedAt()
        );
    }

    // ─── 7. Bootstrap cognitivo (hidratación de familias pre-existentes) ────

    @PostMapping("/{familyId}/bootstrap")
    @PreAuthorize("@familySecurity.check(#familyId)")
    @Operation(summary = "Hidratar sistema cognitivo con evaluaciones pre-existentes",
               description = "Corre el pipeline completo (grafo + memoria + narrativa + skills) sobre la evaluación más reciente. " +
                             "Útil para familias que existían antes de que el motor cognitivo estuviera activo.")
    public ApiResponse<CognitiveSnapshotResponse> bootstrapCognitive(@PathVariable Long familyId) {
        log.info("🚀 [COGNITIVE] Bootstrap cognitivo iniciado para familia ID: {}", familyId);

        // Obtener la evaluación más reciente finalizada
        Evaluation latestEval = evaluationRepository
                .findByFamilyIdOrderByFinalizedAtAsc(familyId)
                .stream()
                .filter(e -> e.getStatus() == EvaluationStatus.FINALIZED && e.getIcf() != null)
                .reduce((first, second) -> second)  // la más reciente
                .orElseThrow(() -> new RuntimeException(
                        "No hay evaluaciones finalizadas para la familia " + familyId));

        log.info("📊 [COGNITIVE] Evaluación de referencia: ID={} | ICF={} | Risk={}",
                latestEval.getId(), latestEval.getIcf(), latestEval.getRiskLevel());

        // Correr el pipeline cognitivo completo
        familyIdentityGraphService.updateGraph(familyId, latestEval);
        familyMemoryService.captureEvaluationMemory(latestEval);
        familyMemoryService.consolidateSemanticPattern(familyId, "evaluation-result");
        familySkillEngine.analyze(familyId, latestEval);
        narrativeEvolutionEngine.evolve(familyId, latestEval);
        familyReflectionService.reflect(familyId);

        log.info("✅ [COGNITIVE] Bootstrap completado para familia ID: {}", familyId);

        // Devolver snapshot actualizado
        return getSnapshot(familyId);
    }

    // ─── Mappers privados ────────────────────────────────────────────────────

    private IdentityProfileDto mapIdentity(FamilyIdentityProfile p) {
        return new IdentityProfileDto(
                p.getEvolutionStage(),
                p.getCommunicationStyle(),
                p.getConflictStyle(),
                p.getEmotionalExpression(),
                p.getAdaptabilityIndex() != null ? p.getAdaptabilityIndex() : 0.0,
                p.getCompletedCycles() != null ? p.getCompletedCycles() : 0,
                p.getIdentityNarrative()
        );
    }

    private IdentityProfileDto emptyIdentity() {
        return new IdentityProfileDto("INITIAL", "UNKNOWN", "UNKNOWN", "UNKNOWN", 0.0, 0, null);
    }

    private CurrentChapterDto mapCurrentChapter(NarrativeChapter c) {
        return new CurrentChapterDto(
                c.getChapterNumber(),
                c.getTitle(),
                c.getBody(),
                c.getPhase(),
                c.getIcfAtOpen(),
                Boolean.TRUE.equals(c.getTurningPoint())
        );
    }

    private ChapterDto mapChapter(NarrativeChapter c) {
        return new ChapterDto(
                c.getChapterNumber(),
                c.getTitle(),
                c.getBody(),
                c.getPhase(),
                c.getIcfAtOpen(),
                c.getIcfAtClose(),
                Boolean.TRUE.equals(c.getTurningPoint()),
                c.isOpen(),
                c.getOpenedAt(),
                c.getClosedAt()
        );
    }

    private MemoryDto mapMemory(FamilyMemory m) {
        return new MemoryDto(
                m.getId(),
                m.getMemoryType().name(),
                m.getSemanticKey(),
                m.getContent(),
                m.getImportanceScore(),
                m.getSourceType(),
                m.getCreatedAt()
        );
    }

    private DyadDto mapDyad(MemberRelationEdge e, Map<Long, String> names) {
        return new DyadDto(
                e.getMemberA().getId(),
                names.getOrDefault(e.getMemberA().getId(), "—"),
                e.getMemberB().getId(),
                names.getOrDefault(e.getMemberB().getId(), "—"),
                e.getRelationshipType(),
                e.getDynamicType(),
                e.getCohesionScore(),
                e.getTensionScore(),
                e.getCommunicationScore(),
                e.healthScore(),
                e.getEvolutionTrend(),
                e.getRoleA(),
                e.getRoleB()
        );
    }

    private GraphSummaryDto mapGraphSummary(FamilyIdentityGraphService.GraphSnapshot g) {
        List<FamilyMember> members = memberRepository.findByFamilyId(g.familyId());
        Map<Long, String> names = new HashMap<>();
        members.forEach(m -> names.put(m.getId(),
                m.getFirstName() != null ? m.getFirstName() : m.getFullName()));

        List<MemberRoleDto> roles = g.systemRoles().entrySet().stream()
                .map(e -> new MemberRoleDto(e.getKey(), names.getOrDefault(e.getKey(), "—"), e.getValue()))
                .toList();

        return new GraphSummaryDto(
                g.totalDyads(),
                g.cohesionDensity(),
                g.tensionDensity(),
                g.conflictiveEdges(),
                g.isHealthy(),
                roles
        );
    }
}
