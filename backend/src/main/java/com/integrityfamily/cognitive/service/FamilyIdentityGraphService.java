package com.integrityfamily.cognitive.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.MemberRelationEdge.DynamicType;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDD Fase 5 — Grafo de Identidad Familiar.
 *
 * Construye y mantiene un grafo de relaciones entre miembros de la familia.
 * Cada arista modela la dinámica de un par: cohesión, tensión, comunicación y
 * el rol sistémico que cada miembro juega dentro de esa díada.
 *
 * Las puntuaciones se derivan de datos objetivos: dimensiones de evaluación,
 * reflexiones completadas, adherencia e historial de crisis.
 * No requiere input manual — es un grafo vivo que evoluciona con cada ciclo.
 *
 * Responsabilidades:
 *  1. Actualizar todas las aristas del grafo tras cada evaluación.
 *  2. Detectar roles sistémicos: ANCHOR, PEACEMAKER, ESCALATOR, DISCONNECTED.
 *  3. Calcular métricas de grafo: densidad de cohesión, focos de tensión.
 *  4. Devolver un GraphSnapshot para el copiloto IA y el dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyIdentityGraphService {

    private final MemberRelationEdgeRepository edgeRepository;
    private final MemberRepository memberRepository;
    private final ReflectionRepository reflectionRepository;
    private final EvaluationRepository evaluationRepository;
    private final FamilyRepository familyRepository;

    // ─── Punto de entrada ────────────────────────────────────────────────────

    /**
     * Actualiza el grafo de la familia a partir de la evaluación más reciente.
     * Crea aristas nuevas para pares que no existen aún y actualiza las existentes.
     */
    @Transactional
    public GraphSnapshot updateGraph(Long familyId, Evaluation evaluation) {
        List<FamilyMember> members = memberRepository.findByFamilyId(familyId)
                .stream().filter(FamilyMember::isActive).toList();

        if (members.size() < 2) {
            log.debug("👥 [GRAPH] Familia {} tiene {} miembro(s) activo(s) — sin aristas que construir.",
                    familyId, members.size());
            return buildEmptySnapshot(familyId);
        }

        log.info("🕸️ [GRAPH] Actualizando grafo para familia ID: {} ({} miembros activos)",
                familyId, members.size());

        // Datos de contexto para scoring
        DimensionScores dimScores = extractDimensionScores(evaluation);
        List<Reflection> reflections = reflectionRepository.findByFamilyId(familyId);
        double reflectionCommunicationRate = computeReflectionCommunicationRate(reflections);
        boolean hasCrisis = Boolean.TRUE.equals(evaluation.getHasCrisis());
        double icf = evaluation.getIcf() != null ? evaluation.getIcf() : 50.0;

        // Construir/actualizar cada par de miembros (grafo no-dirigido)
        List<MemberRelationEdge> updatedEdges = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                FamilyMember a = members.get(i);
                FamilyMember b = members.get(j);
                // Garantiza a.id < b.id
                if (a.getId() > b.getId()) { FamilyMember tmp = a; a = b; b = tmp; }

                MemberRelationEdge edge = upsertEdge(familyId, a, b, evaluation,
                        dimScores, reflectionCommunicationRate, hasCrisis, icf);
                updatedEdges.add(edge);
            }
        }

        // Detectar roles sistémicos y actualizar aristas
        Map<Long, String> systemRoles = detectSystemRoles(members, updatedEdges);
        applySystemRolesToEdges(updatedEdges, systemRoles);
        edgeRepository.saveAll(updatedEdges);

        log.info("✅ [GRAPH] Grafo actualizado: {} aristas | Roles: {}", updatedEdges.size(), systemRoles);
        return buildSnapshot(familyId, updatedEdges, members, systemRoles);
    }

    // ─── Upsert de arista ────────────────────────────────────────────────────

    private MemberRelationEdge upsertEdge(Long familyId, FamilyMember a, FamilyMember b,
            Evaluation eval, DimensionScores dims, double reflComm, boolean crisis, double icf) {

        MemberRelationEdge edge = edgeRepository.findPair(familyId, a.getId(), b.getId())
                .orElseGet(() -> {
                    Family family = familyRepository.getReferenceById(familyId);
                    return MemberRelationEdge.builder()
                            .family(family)
                            .memberA(a)
                            .memberB(b)
                            .relationshipType(inferRelationshipType(a, b))
                            .build();
                });

        // Scoring
        double prevCohesion = edge.getCohesionScore();
        double newCohesion   = computeCohesionScore(icf, crisis, a, b);
        double newTension    = computeTensionScore(eval, crisis, dims);
        double newComm       = computeCommunicationScore(dims.communication(), reflComm);

        // Suavizado exponencial — evita oscilaciones bruscas (α = 0.35)
        double alpha = 0.35;
        edge.setCohesionScore(blend(edge.getCohesionScore(), newCohesion, alpha));
        edge.setTensionScore(blend(edge.getTensionScore(), newTension, alpha));
        edge.setCommunicationScore(blend(edge.getCommunicationScore(), newComm, alpha));

        // Tendencia
        double deltaCohesion = edge.getCohesionScore() - prevCohesion;
        edge.setEvolutionTrend(deltaCohesion > 3 ? "IMPROVING" : deltaCohesion < -3 ? "DECLINING" : "STABLE");

        // Tipo dinámico
        edge.setDynamicType(classifyDynamic(edge));
        edge.setFromEvaluationId(eval.getId());
        edge.setUpdatedAt(LocalDateTime.now());

        return edge;
    }

    // ─── Cálculo de puntuaciones ─────────────────────────────────────────────

    private double computeCohesionScore(double icf, boolean crisis, FamilyMember a, FamilyMember b) {
        double base = icf * 0.6; // ICF familiar es proxy de cohesión sistémica
        if (crisis) base -= 15;
        // Autonomía y responsabilidad alta → mayor cohesión potencial
        if (a.getAutonomyLevel() != null && a.getAutonomyLevel() >= 7) base += 5;
        if (b.getAutonomyLevel() != null && b.getAutonomyLevel() >= 7) base += 5;
        return Math.max(0, Math.min(100, base));
    }

    private double computeTensionScore(Evaluation eval, boolean crisis, DimensionScores dims) {
        double tension = switch (eval.getRiskLevel() != null ? eval.getRiskLevel() : "MODERADO") {
            case "CRÍTICO", "CRITICO"   -> 80.0;
            case "ALTO"                 -> 60.0;
            case "MODERADO"             -> 35.0;
            case "BAJO"                 -> 15.0;
            default                     -> 30.0;
        };
        if (crisis) tension = Math.max(tension, 85.0);
        // Bajo score de emociones → más tensión
        if (dims.emociones() < 40) tension += 10;
        return Math.max(0, Math.min(100, tension));
    }

    private double computeCommunicationScore(double dimComm, double reflComm) {
        // Promedio ponderado: 60% dimensión evaluación + 40% adherencia reflexión
        return Math.max(0, Math.min(100, dimComm * 0.6 + reflComm * 0.4));
    }

    private DynamicType classifyDynamic(MemberRelationEdge edge) {
        double cohesion = edge.getCohesionScore();
        double tension  = edge.getTensionScore();
        if (cohesion >= 65 && tension <= 35) return DynamicType.SUPPORTIVE;
        if (tension >= 65)                   return DynamicType.CONFLICTIVE;
        if (cohesion <= 35)                  return DynamicType.DISTANT;
        return DynamicType.BALANCED;
    }

    private double blend(double old, double fresh, double alpha) {
        return old * (1 - alpha) + fresh * alpha;
    }

    // ─── Roles sistémicos ────────────────────────────────────────────────────

    /**
     * Detecta el rol que cada miembro juega en el sistema familiar.
     * Basado en el promedio de sus aristas: cohesión, tensión y comunicación.
     */
    private Map<Long, String> detectSystemRoles(List<FamilyMember> members,
                                                 List<MemberRelationEdge> edges) {
        Map<Long, DoubleSummaryStatistics> cohesionStats = new HashMap<>();
        Map<Long, DoubleSummaryStatistics> tensionStats  = new HashMap<>();

        // Acumular stats por miembro
        for (MemberRelationEdge edge : edges) {
            Long aId = edge.getMemberA().getId();
            Long bId = edge.getMemberB().getId();
            cohesionStats.computeIfAbsent(aId, k -> new DoubleSummaryStatistics()).accept(edge.getCohesionScore());
            cohesionStats.computeIfAbsent(bId, k -> new DoubleSummaryStatistics()).accept(edge.getCohesionScore());
            tensionStats.computeIfAbsent(aId, k -> new DoubleSummaryStatistics()).accept(edge.getTensionScore());
            tensionStats.computeIfAbsent(bId, k -> new DoubleSummaryStatistics()).accept(edge.getTensionScore());
        }

        Map<Long, Double> avgCohesion = avgOf(cohesionStats);
        Map<Long, Double> avgTension  = avgOf(tensionStats);

        // Extremos para comparar
        double maxCohesion = avgCohesion.values().stream().mapToDouble(Double::doubleValue).max().orElse(50);
        double minTension  = avgTension.values().stream().mapToDouble(Double::doubleValue).min().orElse(30);
        double maxTension  = avgTension.values().stream().mapToDouble(Double::doubleValue).max().orElse(30);
        double minCohesion = avgCohesion.values().stream().mapToDouble(Double::doubleValue).min().orElse(50);

        Map<Long, String> roles = new HashMap<>();
        for (FamilyMember m : members) {
            Long id = m.getId();
            double coh = avgCohesion.getOrDefault(id, 50.0);
            double ten = avgTension.getOrDefault(id, 30.0);

            String role;
            if (coh == maxCohesion && coh >= 65)   role = "ANCHOR";
            else if (ten == minTension && ten <= 25) role = "PEACEMAKER";
            else if (ten == maxTension && ten >= 55) role = "ESCALATOR";
            else if (coh == minCohesion && coh <= 35) role = "DISCONNECTED";
            else                                     role = "NEUTRAL";

            roles.put(id, role);
        }
        return roles;
    }

    private void applySystemRolesToEdges(List<MemberRelationEdge> edges,
                                          Map<Long, String> systemRoles) {
        for (MemberRelationEdge edge : edges) {
            edge.setRoleA(systemRoles.getOrDefault(edge.getMemberA().getId(), "NEUTRAL"));
            edge.setRoleB(systemRoles.getOrDefault(edge.getMemberB().getId(), "NEUTRAL"));
        }
    }

    // ─── Tipos de relación ───────────────────────────────────────────────────

    private String inferRelationshipType(FamilyMember a, FamilyMember b) {
        String ra = a.getRole() != null ? a.getRole().toUpperCase() : "";
        String rb = b.getRole() != null ? b.getRole().toUpperCase() : "";

        Set<String> parentRoles = Set.of("PADRE", "MADRE", "PAPA", "MAMA");
        Set<String> childRoles  = Set.of("HIJO", "HIJA");
        Set<String> spouseRoles = Set.of("PADRE", "MADRE", "PAPA", "MAMA", "ESPOSO", "ESPOSA", "CONYUGE");

        if (parentRoles.contains(ra) && spouseRoles.contains(rb)
                && !childRoles.contains(rb)) return "SPOUSE";
        if ((parentRoles.contains(ra) && childRoles.contains(rb))
                || (parentRoles.contains(rb) && childRoles.contains(ra))) return "PARENT_CHILD";
        if (childRoles.contains(ra) && childRoles.contains(rb)) return "SIBLING";
        return "OTHER";
    }

    // ─── Snapshot ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GraphSnapshot getSnapshot(Long familyId) {
        List<FamilyMember> members = memberRepository.findByFamilyId(familyId)
                .stream().filter(FamilyMember::isActive).toList();
        List<MemberRelationEdge> edges = edgeRepository.findByFamilyId(familyId);
        Map<Long, String> roles = detectSystemRoles(members, edges);
        return buildSnapshot(familyId, edges, members, roles);
    }

    private GraphSnapshot buildSnapshot(Long familyId, List<MemberRelationEdge> edges,
                                         List<FamilyMember> members, Map<Long, String> roles) {
        double cohesionDensity = edges.stream()
                .mapToDouble(MemberRelationEdge::getCohesionScore).average().orElse(0);
        double tensionDensity  = edges.stream()
                .mapToDouble(MemberRelationEdge::getTensionScore).average().orElse(0);

        long conflictEdges = edges.stream()
                .filter(e -> e.getDynamicType() == DynamicType.CONFLICTIVE).count();

        String anchor = roles.entrySet().stream()
                .filter(e -> "ANCHOR".equals(e.getValue()))
                .map(e -> members.stream().filter(m -> m.getId().equals(e.getKey()))
                        .findFirst().map(FamilyMember::getFirstName).orElse("—"))
                .findFirst().orElse(null);

        String summary = buildGraphSummary(edges.size(), cohesionDensity, tensionDensity,
                conflictEdges, roles, anchor);

        return new GraphSnapshot(familyId, edges, members, roles,
                cohesionDensity, tensionDensity, conflictEdges, summary);
    }

    private GraphSnapshot buildEmptySnapshot(Long familyId) {
        return new GraphSnapshot(familyId, List.of(), List.of(),
                Map.of(), 0, 0, 0, "Familia con un solo miembro activo — grafo no aplica.");
    }

    private String buildGraphSummary(int edges, double cohesion, double tension,
                                      long conflicts, Map<Long, String> roles, String anchor) {
        String health = cohesion >= 65 ? "saludable" : cohesion >= 45 ? "moderado" : "bajo";
        StringBuilder sb = new StringBuilder(String.format(
            "Grafo familiar: %d díadas | Cohesión promedio: %.1f (%s) | Tensión promedio: %.1f.",
            edges, cohesion, health, tension));

        if (conflicts > 0) sb.append(String.format(" ⚠️ %d díada(s) conflictiva(s).", conflicts));
        if (anchor != null) sb.append(String.format(" Ancla principal: %s.", anchor));

        long escalators = roles.values().stream().filter("ESCALATOR"::equals).count();
        if (escalators > 0) sb.append(String.format(" %d miembro(s) con rol ESCALATOR detectado.", escalators));

        return sb.toString();
    }

    // ─── Utilidades de stats ─────────────────────────────────────────────────

    private Map<Long, Double> avgOf(Map<Long, DoubleSummaryStatistics> statsMap) {
        Map<Long, Double> result = new HashMap<>();
        statsMap.forEach((id, stats) -> result.put(id, stats.getAverage()));
        return result;
    }

    private DimensionScores extractDimensionScores(Evaluation eval) {
        if (eval.getDimensionScores() == null || eval.getDimensionScores().isEmpty()) {
            return new DimensionScores(50, 50, 50, 50);
        }
        Map<String, Double> map = new HashMap<>();
        eval.getDimensionScores().forEach(ds ->
                map.put(ds.getDimensionName().toLowerCase(), ds.getScore()));

        return new DimensionScores(
            map.getOrDefault("comunicacion", 50.0),
            map.getOrDefault("emociones", 50.0),
            map.getOrDefault("habitos", 50.0),
            map.getOrDefault("tiempos", 50.0)
        );
    }

    private double computeReflectionCommunicationRate(List<Reflection> reflections) {
        if (reflections.isEmpty()) return 50.0;
        long improved = reflections.stream()
                .filter(r -> Boolean.TRUE.equals(r.getCommunicationImproved())).count();
        return (double) improved / reflections.size() * 100;
    }

    // ─── Tipos de datos ──────────────────────────────────────────────────────

    private record DimensionScores(double communication, double emociones,
                                    double habitos, double tiempos) {}

    public record GraphSnapshot(
            Long familyId,
            List<MemberRelationEdge> edges,
            List<FamilyMember> members,
            Map<Long, String> systemRoles,
            double cohesionDensity,
            double tensionDensity,
            long conflictiveEdges,
            String summary
    ) {
        public boolean isHealthy() { return cohesionDensity >= 65 && tensionDensity <= 35; }
        public boolean hasConflict() { return conflictiveEdges > 0; }
        public int totalDyads() { return edges.size(); }
    }
}
