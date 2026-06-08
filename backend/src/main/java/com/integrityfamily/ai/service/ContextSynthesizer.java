package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.dto.CopilotDtos;
import com.integrityfamily.cognitive.service.FamilyMemoryService;
import com.integrityfamily.cognitive.service.FamilyIdentityGraphService;
import com.integrityfamily.cognitive.service.MemberIdentityProfileService;
import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.MemberIdentityProfile;
import com.integrityfamily.domain.ChatMessage;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.FamilyMemory;
import com.integrityfamily.domain.ImprovementPlan;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.domain.repository.ImprovementPlanRepository;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.FamilySprint;
import com.integrityfamily.domain.SprintMission;
import com.integrityfamily.domain.repository.FamilySprintRepository;
import com.integrityfamily.dna.service.FamilyDnaService;
import com.integrityfamily.ritual.service.RitualEngineService;
import com.integrityfamily.tree.service.FamilyTreeService;
import com.integrityfamily.context.service.FamilyContextEngine;
import com.integrityfamily.twin.service.DigitalTwinService;
import com.integrityfamily.participation.service.ParticipationService;
import com.integrityfamily.scanner.domain.FamilyAlert;
import com.integrityfamily.scanner.repository.FamilyAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD-AI-03.2 v2: Sintetizador de Contexto Relacional Unificado.
 * Incorpora perfil del miembro activo, estado del Guardián,
 * snapshot cognitivo y plan activo en el contexto de cada interacción.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContextSynthesizer {

    private final RiskSnapshotRepository riskRepo;
    private final ImprovementPlanRepository planRepo;
    private final EvaluationRepository evalRepo;
    private final ChatMessageRepository chatRepo;
    private final CopilotService copilotService;
    private final ParticipationService participationService;
    private final FamilyMemoryService familyMemoryService;
    private final FamilyIdentityGraphService identityGraphService;
    private final FamilyAlertRepository alertRepository;
    private final MemberIdentityProfileService memberIdentityProfileService;
    private final ConversationSessionRepository conversationSessionRepository;
    private final FamilyLongitudinalStateRepository longitudinalStateRepository;
    private final FamilySprintRepository sprintRepository;
    private final FamilyDnaService familyDnaService;
    private final RitualEngineService ritualEngineService;
    private final FamilyTreeService familyTreeService;
    private final FamilyContextEngine familyContextEngine;
    private final DigitalTwinService  digitalTwinService;
    private final ObjectMapper objectMapper;

    private static final int MISSION_LIMIT = 5;
    private static final int NEXT_MISSIONS_LIMIT = 3;
    private static final int HISTORY_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Síntesis básica sin identidad de miembro. Mantiene compatibilidad con
     * llamadas analíticas (dashboard, scanner, síntesis ejecutiva).
     */
    @Transactional(readOnly = true)
    public AiContext synthesize(Family family, String sentiment) {
        return synthesize(family, null, sentiment);
    }

    /**
     * Síntesis enriquecida con identidad del miembro. Usar en chat conversacional.
     * Proporciona a Claude el contexto relacional completo:
     * quién habla, si es el Guardián, estado cognitivo familiar y plan activo.
     */
    @Transactional(readOnly = true)
    public AiContext synthesize(Family family, Long memberId, String sentiment) {
        return synthesize(family, memberId, null, sentiment);
    }

    /**
     * Síntesis enriquecida con sesión activa (Fase C).
     * Lee arco emocional y objetivo conversacional desde la ConversationSession.
     */
    @Transactional(readOnly = true)
    public AiContext synthesize(Family family, Long memberId, Long sessionId, String sentiment) {
        log.debug("[CONTEXT] Sintetizando contexto para familia {} / miembro {} / sesión {}", family.getId(), memberId, sessionId);

        List<RiskSnapshot> riskHistory = riskRepo.findByFamilyIdOrderByCreatedAtDesc(family.getId());
        Optional<Evaluation> latestEval = evalRepo.findTopByFamilyIdAndStatusOrderByFinalizedAtDesc(
                family.getId(), EvaluationStatus.FINALIZED);
        RiskSnapshot currentRisk = riskHistory.isEmpty() ? null : riskHistory.get(0);

        return new AiContext(
            buildMetadata(family, currentRisk),
            buildMemberNodes(family),
            buildMetrics(currentRisk),
            buildTrends(currentRisk, riskHistory),
            extractDimensionScores(latestEval),
            fetchTopMissions(family.getId()),
            fetchMessageHistory(family.getId()),
            Boolean.TRUE.equals(family.getSentinelActive()),
            sentiment,
            buildActiveMemberProfile(family, memberId, currentRisk),
            buildGuardianProfile(family),
            buildCognitiveSnapshot(family.getId()),
            buildActivePlanSnapshot(family),
            buildMemoryContext(family.getId()),
            buildRelationalGraph(family.getId()),
            buildInterventionLevel(family.getId()),
            buildMemberIdentitySnapshot(memberId),
            buildEmotionalArcFromSession(sessionId),
            buildConversationGoalFromSession(sessionId),
            buildLongitudinalContext(family.getId()),   // ← arquitectura epistemológica
            buildActiveSprintSnapshot(family.getId()), // ← sprint activo con misiones en curso
            familyDnaService.buildDnaContextBlock(family.getId()),    // ← ADN Familiar
            ritualEngineService.buildRitualContextBlock(family.getId()), // ← Rituales activos
            familyTreeService.buildTreeContextBlock(family.getId()),     // ← Árbol Generacional
            familyContextEngine.buildContextBlock(family.getId()),       // ← Motor de Contexto
            digitalTwinService.buildTwinContextBlock(family.getId())    // ← Gemelo Digital
        );
    }

    // ─── Builders existentes ──────────────────────────────────────────────────

    private AiContext.FamilyMetadata buildMetadata(Family family, RiskSnapshot risk) {
        String lastUpdate = (risk != null) ? risk.getCreatedAt().format(DATE_FORMAT) : "N/A";
        return new AiContext.FamilyMetadata(family.getName(), family.getCurrentMilestone(), lastUpdate);
    }

    private List<AiContext.MemberNode> buildMemberNodes(Family family) {
        return family.getMembers().stream()
                .filter(FamilyMember::isActive)
                .map(m -> new AiContext.MemberNode(m.getFullName(), m.getRole()))
                .toList();
    }

    private AiContext.IntegrityMetrics buildMetrics(RiskSnapshot risk) {
        if (risk == null) return new AiContext.IntegrityMetrics(0.0, "DESCONOCIDO", "INICIAL");
        return new AiContext.IntegrityMetrics(risk.getIcf(), risk.getRiskLevel(), risk.getConsciousnessLabel());
    }

    private AiContext.TrendAnalysis buildTrends(RiskSnapshot current, List<RiskSnapshot> history) {
        if (current == null || history.size() < 2) {
            return new AiContext.TrendAnalysis(0.0, 0.0, 0.0);
        }
        RiskSnapshot significantPrevious = history.stream()
                .skip(1)
                .filter(rs -> java.time.Duration.between(rs.getCreatedAt(), current.getCreatedAt()).toHours() >= 24)
                .findFirst()
                .orElse(history.get(1));
        double delta = current.getIcf() - significantPrevious.getIcf();
        long daysBetween = java.time.Duration.between(significantPrevious.getCreatedAt(), current.getCreatedAt()).toDays();
        double velocity = (daysBetween > 0) ? delta / daysBetween : delta;
        return new AiContext.TrendAnalysis(significantPrevious.getIcf(), delta, velocity);
    }

    private Map<String, Double> extractDimensionScores(Optional<Evaluation> eval) {
        return eval.map(e -> e.getDimensionScores().stream()
                .collect(Collectors.toMap(
                    ds -> ds.getDimensionName(),
                    ds -> ds.getScore(),
                    (v1, v2) -> v1
                )))
                .orElse(Collections.emptyMap());
    }

    private List<AiContext.ActiveMission> fetchTopMissions(Long familyId) {
        return planRepo.findByFamilyId(familyId).stream()
                .flatMap(p -> p.getTasks().stream())
                .filter(t -> !t.isCompleted())
                .limit(MISSION_LIMIT)
                .map(t -> new AiContext.ActiveMission(t.getTitle(), t.getDescription()))
                .toList();
    }

    private List<AiContext.MessageHistory> fetchMessageHistory(Long familyId) {
        List<ChatMessage> messages = chatRepo.findByFamilyIdOrderByCreatedAtDesc(familyId, PageRequest.of(0, HISTORY_LIMIT));
        List<ChatMessage> messageList = new ArrayList<>(messages);
        Collections.reverse(messageList);
        return messageList.stream()
                .map(m -> new AiContext.MessageHistory(m.isAi() ? "ASSISTANT" : "USER", m.getContent()))
                .toList();
    }

    // ─── Nuevos builders relacionales ────────────────────────────────────────

    /**
     * Perfil del miembro que está enviando el mensaje.
     * Incluye si es el Guardián, su rol y nivel de conciencia familiar actual.
     * Retorna null si memberId es null (llamadas analíticas).
     */
    private AiContext.ActiveMemberProfile buildActiveMemberProfile(
            Family family, Long memberId, RiskSnapshot currentRisk) {
        if (memberId == null) return null;

        return family.getMembers().stream()
                .filter(m -> m.getId().equals(memberId) && m.isActive())
                .findFirst()
                .map(member -> {
                    boolean isGuardian = memberId.equals(family.getGuardianMemberId());
                    String consciousnessLevel = (currentRisk != null && currentRisk.getConsciousnessLabel() != null)
                            ? currentRisk.getConsciousnessLabel() : "DESCONOCIDO";
                    String lastParticipation = (member.getJoinedAt() != null)
                            ? member.getJoinedAt().format(DATE_FORMAT) : "N/A";
                    return new AiContext.ActiveMemberProfile(
                            member.getId(), member.getFullName(), member.getRole(),
                            isGuardian, consciousnessLevel, lastParticipation);
                })
                .orElse(null);
    }

    /**
     * Estado del Guardián Familiar.
     * Si la familia no tiene Guardián elegido, retorna null.
     * inactiveMembers y fatigueSignal se refinan en Fase 3 (ParticipationEngine).
     */
    private AiContext.GuardianProfile buildGuardianProfile(Family family) {
        if (family.getGuardianMemberId() == null) return null;

        String guardianName = family.getMembers().stream()
                .filter(m -> m.getId().equals(family.getGuardianMemberId()))
                .findFirst()
                .map(FamilyMember::getFullName)
                .orElse("Guardián");

        int familySize = (int) family.getMembers().stream().filter(FamilyMember::isActive).count();

        ParticipationService.FamilyParticipationSummary participation =
                participationService.getSummary(family.getId(), family.getGuardianMemberId());

        return new AiContext.GuardianProfile(
                family.getGuardianMemberId(),
                guardianName,
                familySize,
                participation.activeParticipants(),
                participation.inactiveMembers(),
                participation.fatigueSignal()
        );
    }

    /**
     * Snapshot compacto del motor cognitivo.
     * Falla silenciosamente: si el módulo cognitivo no tiene datos,
     * retorna null para que el prompt funcione sin ese bloque.
     */
    private AiContext.CognitiveSnapshot buildCognitiveSnapshot(Long familyId) {
        try {
            CopilotDtos.CognitiveEnrichment enrichment = copilotService.buildCognitiveEnrichment(familyId);
            return new AiContext.CognitiveSnapshot(
                    enrichment.evolutionStage(),
                    enrichment.communicationStyle(),
                    enrichment.currentChapterPhase(),
                    enrichment.turningPointInLastEval(),
                    enrichment.lastLessonLearned(),
                    enrichment.activeSkills(),
                    enrichment.abandonmentRisk()
            );
        } catch (Exception e) {
            log.warn("[CONTEXT] CognitiveSnapshot no disponible para familia {}: {}", familyId, e.getMessage());
            return null;
        }
    }

    /**
     * Snapshot del plan activo: hito actual, pilar, misiones pendientes del hito
     * y tasa de cumplimiento global.
     */
    private AiContext.ActivePlanSnapshot buildActivePlanSnapshot(Family family) {
        List<ImprovementPlan> plans = planRepo.findByFamilyId(family.getId());
        if (plans.isEmpty()) return null;

        ImprovementPlan latestPlan = plans.get(plans.size() - 1);
        List<PlanTask> tasks = latestPlan.getTasks();

        long total = tasks.size();
        long completedCount = tasks.stream().filter(PlanTask::isCompleted).count();
        double completionRate = total > 0 ? (double) completedCount / total * 100.0 : 0.0;

        String currentMilestone = family.getCurrentMilestone();
        String pillar = pillarFromMilestone(currentMilestone);

        // Prioriza misiones del hito actual; si no hay, toma las más próximas sin importar hito
        List<AiContext.ActiveMission> nextMissions = tasks.stream()
                .filter(t -> !t.isCompleted())
                .filter(t -> currentMilestone.equals(t.getMilestoneCode()) || t.getMilestoneCode() == null)
                .limit(NEXT_MISSIONS_LIMIT)
                .map(t -> new AiContext.ActiveMission(t.getTitle(), t.getDescription()))
                .toList();

        if (nextMissions.isEmpty()) {
            nextMissions = tasks.stream()
                    .filter(t -> !t.isCompleted())
                    .limit(NEXT_MISSIONS_LIMIT)
                    .map(t -> new AiContext.ActiveMission(t.getTitle(), t.getDescription()))
                    .toList();
        }

        return new AiContext.ActivePlanSnapshot(
                latestPlan.getId(), currentMilestone, pillar, nextMissions, completionRate);
    }

    // ─── Sprint Activo ────────────────────────────────────────────────────────

    /**
     * Retorna el sprint activo de la familia con sus misiones en curso.
     * Null si no hay sprint activo — el prompt lo omite en ese caso.
     */
    private AiContext.ActiveSprintSnapshot buildActiveSprintSnapshot(Long familyId) {
        try {
            return sprintRepository.findActiveSprintForFamily(familyId)
                .map(sprint -> {
                    List<SprintMission> missions = sprint.getMissions();
                    int total = missions.size();
                    long completed = missions.stream()
                            .filter(m -> "COMPLETED".equals(m.getStatus())).count();
                    double progress = total > 0 ? (double) completed / total * 100.0 : 0.0;
                    List<String> pending = missions.stream()
                            .filter(m -> "PENDING".equals(m.getStatus()))
                            .map(SprintMission::getDescription)
                            .toList();
                    return new AiContext.ActiveSprintSnapshot(
                            sprint.getId(),
                            sprint.getObjective(),
                            sprint.getRiskDimension(),
                            sprint.getDurationDays(),
                            sprint.getStartDate().toString(),
                            sprint.getEndDate().toString(),
                            total,
                            (int) completed,
                            progress,
                            pending
                    );
                })
                .orElse(null);
        } catch (Exception e) {
            log.warn("[CONTEXT] No se pudo cargar sprint activo para familia {}: {}", familyId, e.getMessage());
            return null;
        }
    }

    // ─── Fase B: Perfil de Identidad del Miembro ─────────────────────────────

    /**
     * Lee el perfil de identidad conversacional del miembro activo.
     * Retorna null si memberId es null o si el servicio falla.
     */
    private AiContext.MemberIdentitySnapshot buildMemberIdentitySnapshot(Long memberId) {
        if (memberId == null) return null;
        try {
            MemberIdentityProfile profile = memberIdentityProfileService.getOrCreate(memberId);
            return new AiContext.MemberIdentitySnapshot(
                    profile.getCommunicationStyle(),
                    profile.getReflexivityLevel() != null ? profile.getReflexivityLevel() : 3,
                    profile.getEmotionalSensitivity() != null ? profile.getEmotionalSensitivity() : 3,
                    profile.getChangeResistance() != null ? profile.getChangeResistance() : "MED",
                    profile.getEvasionPatterns(),
                    profile.getMotivators()
            );
        } catch (Exception e) {
            log.warn("[CONTEXT] MemberIdentitySnapshot no disponible para miembro {}: {}", memberId, e.getMessage());
            return null;
        }
    }

    // ─── Fase A: Motor Cognitivo Conectado ───────────────────────────────────

    /**
     * Extrae la memoria semántica e identidad familiar del motor cognitivo.
     * Falla silenciosamente para no bloquear el chat si el motor no tiene datos.
     */
    private String buildMemoryContext(Long familyId) {
        try {
            FamilyMemoryService.CognitiveContext ctx = familyMemoryService.buildCognitiveContext(familyId);
            if (!ctx.hasPatterns() && !ctx.hasIdentity()) return null;

            StringBuilder sb = new StringBuilder();

            if (ctx.hasIdentity()) {
                var identity = ctx.identityProfile();
                sb.append(String.format(
                    "Etapa evolutiva: %s | Ciclos completados: %d | Adaptabilidad: %.2f | Expresión emocional: %s",
                    ctx.evolutionStage(),
                    identity.getCompletedCycles(),
                    identity.getAdaptabilityIndex(),
                    identity.getEmotionalExpression() != null ? identity.getEmotionalExpression() : "N/A"
                ));
            }

            if (ctx.hasPatterns()) {
                for (FamilyMemory pattern : ctx.semanticPatterns()) {
                    String summary = summarizeSemanticPattern(pattern);
                    if (summary != null) sb.append("\n").append(summary);
                }
            }

            return sb.isEmpty() ? null : sb.toString();
        } catch (Exception e) {
            log.warn("[CONTEXT] MemoryContext no disponible para familia {}: {}", familyId, e.getMessage());
            return null;
        }
    }

    private String summarizeSemanticPattern(FamilyMemory pattern) {
        try {
            JsonNode node = objectMapper.readTree(pattern.getContent());
            return switch (pattern.getSemanticKey()) {
                case "conversation-trend-pattern" -> String.format(
                    "Tendencia conversacional: objetivo dominante=%s | arco emocional dominante=%s | promedio %s turnos/sesión (%d sesiones)",
                    node.path("dominantGoal").asText("?"),
                    node.path("dominantEmotionalArc").asText("?"),
                    node.path("averageTurnsPerSession").asText("?"),
                    node.path("episodeCount").asInt(0));
                case "evaluation-trend-pattern" -> String.format(
                    "Tendencia evaluativa: ICF promedio=%.1f | tendencia=%s | dimensión crítica=%s",
                    node.path("averageIcf").asDouble(0),
                    node.path("trend").asText("?"),
                    node.path("dominantCriticalDimension").asText("?"));
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Lee el grafo de relaciones entre miembros y devuelve el resumen textual.
     * Retorna null si la familia tiene menos de 2 miembros activos o sin evaluaciones.
     */
    private String buildRelationalGraph(Long familyId) {
        try {
            FamilyIdentityGraphService.GraphSnapshot snapshot = identityGraphService.getSnapshot(familyId);
            return snapshot.totalDyads() > 0 ? snapshot.summary() : null;
        } catch (Exception e) {
            log.warn("[CONTEXT] RelationalGraph no disponible para familia {}: {}", familyId, e.getMessage());
            return null;
        }
    }

    /**
     * Calcula el nivel de intervención clínica basado en alertas IF-ALT activas sin resolver.
     */
    private String buildInterventionLevel(Long familyId) {
        try {
            List<FamilyAlert> alerts = alertRepository
                    .findByFamilyIdAndResolvedFalseOrderByCreatedAtDesc(familyId);
            if (alerts.isEmpty()) return "NONE";
            boolean hasCritical = alerts.stream().anyMatch(a -> "CRITICAL".equals(a.getSeverity()));
            if (hasCritical) return "CRISIS";
            boolean hasHigh = alerts.stream().anyMatch(a -> "HIGH".equals(a.getSeverity()));
            if (hasHigh || alerts.size() >= 2) return "URGENT";
            return "ATTENTION";
        } catch (Exception e) {
            log.warn("[CONTEXT] InterventionLevel no disponible para familia {}: {}", familyId, e.getMessage());
            return "NONE";
        }
    }

    // ─── Fase C: Arco Emocional + Objetivo Conversacional ────────────────────

    private String buildEmotionalArcFromSession(Long sessionId) {
        if (sessionId == null) return null;
        try {
            return conversationSessionRepository.findById(sessionId)
                    .map(ConversationSession::getEmotionalState)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[CONTEXT] EmotionalArc no disponible para sesión {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    private String buildConversationGoalFromSession(Long sessionId) {
        if (sessionId == null) return null;
        try {
            return conversationSessionRepository.findById(sessionId)
                    .map(ConversationSession::getGoal)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[CONTEXT] ConversationGoal no disponible para sesión {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    // ─── Arquitectura Epistemológica: Estado Longitudinal ─────────────────────

    /**
     * Construye el contexto longitudinal de la familia para el Consultor IA.
     *
     * Este es el componente que transforma el Consultor IA de un sistema
     * sin memoria (FALLA 2) a un sistema con conocimiento estructural real.
     *
     * El contexto incluye:
     *   - Trayectoria ICF (hoy vs 30d atrás)
     *   - Tendencia: IMPROVING / STABLE / DETERIORATING
     *   - Fase evolutiva: RECONOCIMIENTO → AMOR → ENTREGA
     *   - Señales de crisis activa y deterioro sostenido
     *
     * Si no existe estado longitudinal aún, retorna un contexto neutro
     * para no bloquear el chat.
     */
    private AiContext.LongitudinalContext buildLongitudinalContext(Long familyId) {
        try {
            return longitudinalStateRepository.findByFamilyId(familyId)
                    .map(state -> new AiContext.LongitudinalContext(
                            state.getIcfCurrent(),
                            state.icfDelta30d(),
                            state.getRiskTrend()       != null ? state.getRiskTrend()       : "STABLE",
                            state.getCurrentRiskLevel() != null ? state.getCurrentRiskLevel() : "MODERADO",
                            state.getEvolutionPhase()  != null ? state.getEvolutionPhase()  : "inconsciente",
                            state.getNarrativeStage()  != null ? state.getNarrativeStage()  : "RECONOCIMIENTO",
                            state.getConsciousnessLabel() != null ? state.getConsciousnessLabel() : "Reactiva",
                            state.getCrisisCount30d()  != null ? state.getCrisisCount30d()  : 0,
                            state.getConsecutiveDeteriorations() != null ? state.getConsecutiveDeteriorations() : 0,
                            state.getConsecutiveImprovements()   != null ? state.getConsecutiveImprovements()   : 0,
                            Boolean.TRUE.equals(state.getCommunicationCollapseActive()),
                            state.isInActiveCrisis(),
                            state.getCriticalDimension() != null ? state.getCriticalDimension() : "emociones"
                    ))
                    .orElse(neutralLongitudinalContext());
        } catch (Exception e) {
            log.warn("[CONTEXT] LongitudinalContext no disponible para familia {}: {}", familyId, e.getMessage());
            return neutralLongitudinalContext();
        }
    }

    /** Contexto longitudinal neutro para familias sin historial aún */
    private AiContext.LongitudinalContext neutralLongitudinalContext() {
        return new AiContext.LongitudinalContext(
                50.0, 0.0, "STABLE", "MODERADO",
                "inconsciente", "RECONOCIMIENTO", "Reactiva",
                0, 0, 0, false, false, "emociones"
        );
    }

    private String pillarFromMilestone(String code) {
        if (code == null) return "RECONOCIMIENTO";
        return switch (code) {
            case "W1", "M1", "M2", "M3" -> "RECONOCIMIENTO";
            case "M4", "M5", "M6", "M9", "M12" -> "AMOR";
            case "M15", "M18", "M21", "M24", "M36" -> "ENTREGA";
            default -> "RECONOCIMIENTO";
        };
    }
}
