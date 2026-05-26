package com.integrityfamily.plan.service;

import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor Determinístico de Planes Congruentes con el Hito Temporal (Taxonomía v2).
 *
 * Genera un conjunto de PlanTask a partir de:
 *   - El resultado de RISK_ALGO_V1 (ICF, dimensionScores, missionGenerator, relapseFlags…)
 *   - El hito actual de la familia (W1 → M36)
 *   - El banco de 1000 preguntas (fuente de metadatos clínicos: riskType, category, evidenceType)
 *
 * Reglas de generación:
 *   1. Dimensión crítica → 2 tareas (acción primaria + seguimiento)
 *   2. Resto de dimensiones con score < 70 → 1 tarea cada una
 *   3. relapseDetected → +1 tarea centinela de recuperación
 *   4. simulationSuspected → +1 tarea de honestidad/transparencia familiar
 *   5. Todas las fechas vencen en el rango del hito actual (dueDate = now + días del hito)
 *   6. pillarName, milestoneCode, missionGenerator y riskType provienen del banco de preguntas
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MilestoneAwarePlanEngine {

    private final QuestionRepository questionRepository;
    private final PlanTaskRepository planTaskRepository;

    // ─── Mapas de contenido clínico por riskType ────────────────────────────

    /** Acción concreta recomendada por tipo de riesgo */
    private static final Map<String, String> ACTION_BY_RISK = Map.ofEntries(
        Map.entry("desconexion_emocional",
            "Destina 15 minutos al día para una conversación sin pantallas, mirando a los ojos a cada miembro de la familia."),
        Map.entry("comunicacion_toxica",
            "Practica la escucha activa: antes de responder, repite en tus palabras lo que el otro dijo y pregunta si entendiste bien."),
        Map.entry("falta_acuerdos",
            "Realiza una mesa familiar semanal de 20 minutos para revisar compromisos y acordar ajustes sin juicios."),
        Map.entry("ausencia_temporal",
            "Bloquea en el calendario familiar al menos 3 momentos de presencia plena (sin trabajo ni dispositivos) por semana."),
        Map.entry("conflicto_reactivo",
            "Cuando sientas que la tensión sube, usa la señal familiar acordada ('tiempo fuera') antes de responder."),
        Map.entry("ausencia_rutinas",
            "Define y escribe 3 rutinas familiares no negociables (ej: cena juntos, lectura nocturna, saludo al despertar)."),
        Map.entry("mal_uso_tiempo",
            "Usa el método 'tiempo de calidad': 30 minutos sin interrupciones dedicados exclusivamente a un miembro cada día.")
    );

    /** Indicador de cumplimiento por dimensión */
    private static final Map<String, String> INDICATOR_BY_DIM = Map.of(
        "emociones",    "Cada miembro reporta en bitácora cómo se sintió durante la semana con una escala 1-5.",
        "comunicacion", "Se sostiene al menos 1 conversación profunda por semana sin interrupciones ni defensas.",
        "habitos",      "Las 3 rutinas definidas se ejecutan ≥ 5 días de 7 durante el período.",
        "tiempos",      "Los momentos de presencia plena ocurren según lo comprometido en el calendario familiar."
    );

    /** Evidencia requerida por dimensión */
    private static final Map<String, String> EVIDENCE_BY_DIM = Map.of(
        "emociones",    "Entrada reflexiva en la bitácora familiar o captura de pantalla del registro emocional.",
        "comunicacion", "Nota breve en el diario de convivencia describiendo el tema tratado y el acuerdo alcanzado.",
        "habitos",      "Fotografía o check en el tablero familiar de rutinas completadas.",
        "tiempos",      "Captura del calendario con los momentos marcados o registro de actividad compartida."
    );

    /** Objetivo por pilar */
    private static final Map<String, String> OBJECTIVE_BY_PILLAR = Map.of(
        "reconocimiento", "Reconocer patrones relacionales y cultivar la presencia consciente en el hogar.",
        "amor",           "Construir vínculos profundos a través de la comunicación honesta y el tiempo de calidad.",
        "entrega",        "Consolidar hábitos de integridad familiar que trasciendan a las nuevas generaciones."
    );

    /** Impacto ICF estimado por nivel de riesgo */
    private static final Map<String, Integer> ICF_IMPACT_BY_RISK = Map.of(
        "CRITICO",  20,
        "ALTO",     15,
        "MODERADO", 10,
        "BAJO",      5
    );

    // ─── API Pública ───────────────────────────────────────────────────────────

    /**
     * Genera y persiste todas las PlanTask dentro del plan dado.
     *
     * @param plan       plan ya persistido donde se añadirán las tareas
     * @param evaluation evaluación de origen (tiene family, milestoneKey, answers…)
     * @param algo       resultado del RISK_ALGO_V1 con scores y señales
     * @return lista de tareas generadas
     */
    @Transactional
    public List<PlanTask> generate(ImprovementPlan plan,
                                   Evaluation evaluation,
                                   RiskAlgoV1Engine.AlgoResult algo) {

        String milestone = resolveCurrentMilestone(evaluation);
        String pillar    = resolvePillar(milestone);
        int dueDays      = resolveDueDays(milestone);

        log.info("[PLAN-ENGINE-V2] Generando plan. Familia={} | Hito={} | Pilar={} | ICF={} | Riesgo={}",
                evaluation.getFamily().getName(), milestone, pillar, algo.healthyIndex(), algo.riskLevel());

        List<PlanTask> tasks = new ArrayList<>();

        // ── 1. Tareas por dimensión según score ──────────────────────────────
        Map<String, Double> scores = algo.dimensionScores();
        String criticalDim = algo.criticalDimension();

        for (String dim : List.of("emociones", "comunicacion", "habitos", "tiempos")) {
            double score = scores.getOrDefault(dim, 100.0);
            boolean isCritical = dim.equals(criticalDim);

            // Siempre genera tarea para dim crítica; para las demás solo si score < 70
            if (!isCritical && score >= 70.0) continue;

            // Obtener metadatos del banco de preguntas para esta dimensión + hito
            QuestionMeta meta = resolveQuestionMeta(milestone, dim);

            tasks.add(buildTask(plan, evaluation, algo,
                milestone, pillar, dim, dueDays,
                meta.riskType(), meta.missionGenerator(), meta.evidenceType(),
                isCritical ? "principal" : "secundaria"));

            // Segunda tarea de seguimiento para dimensión crítica
            if (isCritical) {
                tasks.add(buildFollowUpTask(plan, evaluation, algo,
                    milestone, pillar, dim, dueDays, meta.riskType()));
            }
        }

        // ── 2. Tarea centinela de recaída ────────────────────────────────────
        if (algo.relapseDetected()) {
            tasks.add(buildRelapseTask(plan, evaluation, milestone, pillar, dueDays,
                    criticalDim, algo.riskLevel()));
        }

        // ── 3. Tarea de honestidad (simulación detectada) ────────────────────
        if (algo.simulationSuspected()) {
            tasks.add(buildSimulationTask(plan, evaluation, milestone, pillar, dueDays));
        }

        // Persistir en lote
        List<PlanTask> saved = planTaskRepository.saveAll(tasks);
        log.info("[PLAN-ENGINE-V2] {} tareas generadas y persistidas para familia ID={}",
                saved.size(), evaluation.getFamily().getId());
        return saved;
    }

    // ─── Constructores de tareas ───────────────────────────────────────────────

    private PlanTask buildTask(ImprovementPlan plan, Evaluation eval,
                               RiskAlgoV1Engine.AlgoResult algo,
                               String milestone, String pillar, String dim,
                               int dueDays, String riskType, String missionGen,
                               String evidenceType, String priority) {

        String action    = ACTION_BY_RISK.getOrDefault(riskType,
                "Practica la observación consciente en la dimensión de " + dim + " durante esta semana.");
        String indicator = INDICATOR_BY_DIM.getOrDefault(dim, "Registro semanal en bitácora familiar.");
        String evidence  = evidenceType != null ? evidenceTypeLabel(evidenceType)
                         : EVIDENCE_BY_DIM.getOrDefault(dim, "Registro en bitácora.");
        String objective = OBJECTIVE_BY_PILLAR.getOrDefault(pillar,
                "Fortalecer la cohesión familiar en la dimensión de " + dim + ".");
        int impact       = ICF_IMPACT_BY_RISK.getOrDefault(algo.riskLevel(), 10);

        String title = buildTitle(dim, riskType, milestone, pillar, priority);

        return PlanTask.builder()
                .plan(plan)
                .title(title)
                .description("Misión " + priority + " · " + pillar.toUpperCase() + " · " + milestone)
                .dimension(dim.toUpperCase())
                .dueDate(LocalDateTime.now().plusDays(dueDays))
                .fase(pillar.toUpperCase())
                .riesgoAsociado(algo.riskLevel())
                .objetivo(objective)
                .accionConcreta(action)
                .indicadorCumplimiento(indicator)
                .evidenciaRequerida(evidence)
                .impactoIcf(impact)
                .pillarName(pillar)
                .milestoneCode(milestone)
                .riskType(riskType)
                .missionGenerator(missionGen)
                .memberType("familia")
                .completed(false)
                .steps(new ArrayList<>())
                .build();
    }

    private PlanTask buildFollowUpTask(ImprovementPlan plan, Evaluation eval,
                                       RiskAlgoV1Engine.AlgoResult algo,
                                       String milestone, String pillar, String dim,
                                       int dueDays, String riskType) {

        String action = "Revisa con la familia el avance de la tarea principal de " + dim +
                ". Ajusta lo que no funcionó y celebra lo que sí se logró.";

        return PlanTask.builder()
                .plan(plan)
                .title("Seguimiento y ajuste — " + dim.substring(0, 1).toUpperCase() + dim.substring(1) + " · " + milestone)
                .description("Tarea de revisión y calibración del avance en " + dim)
                .dimension(dim.toUpperCase())
                .dueDate(LocalDateTime.now().plusDays(Math.max(dueDays - 3, 1)))
                .fase(pillar.toUpperCase())
                .riesgoAsociado(algo.riskLevel())
                .objetivo("Consolidar el aprendizaje y ajustar la estrategia de intervención.")
                .accionConcreta(action)
                .indicadorCumplimiento("La familia identifica al menos un ajuste concreto para la siguiente semana.")
                .evidenciaRequerida("Nota de reflexión en bitácora con mínimo 3 puntos: qué funcionó, qué ajustar, compromiso nuevo.")
                .impactoIcf(5)
                .pillarName(pillar)
                .milestoneCode(milestone)
                .riskType(riskType)
                .missionGenerator("REVISION_ADAPTATIVA")
                .memberType("familia")
                .completed(false)
                .steps(new ArrayList<>())
                .build();
    }

    private PlanTask buildRelapseTask(ImprovementPlan plan, Evaluation eval,
                                      String milestone, String pillar, int dueDays,
                                      String criticalDim, String riskLevel) {

        return PlanTask.builder()
                .plan(plan)
                .title("Protocolo de Recuperación — Señal de Recaída Detectada · " + milestone)
                .description("El algoritmo detectó respuestas que sugieren una recaída en " + criticalDim)
                .dimension(criticalDim.toUpperCase())
                .dueDate(LocalDateTime.now().plusDays(3)) // Urgente — 3 días
                .fase(pillar.toUpperCase())
                .riesgoAsociado(riskLevel)
                .objetivo("Estabilizar la situación y retomar el proceso de transformación familiar.")
                .accionConcreta(
                    "Agenda una conversación familiar sin juicios en las próximas 48 horas. " +
                    "Cada miembro comparte en 2 minutos cómo se siente sin interrupciones. " +
                    "Escríbelo en la bitácora con fecha y compromisos concretos.")
                .indicadorCumplimiento("La conversación ocurrió y quedó registrada en bitácora.")
                .evidenciaRequerida("Entrada en bitácora con fecha, participantes y acuerdos.")
                .impactoIcf(25)
                .pillarName(pillar)
                .milestoneCode(milestone)
                .riskType("recaida_detectada")
                .missionGenerator("PROTOCOLO_RECUPERACION")
                .memberType("familia")
                .completed(false)
                .steps(new ArrayList<>())
                .build();
    }

    private PlanTask buildSimulationTask(ImprovementPlan plan, Evaluation eval,
                                         String milestone, String pillar, int dueDays) {

        return PlanTask.builder()
                .plan(plan)
                .title("Espacio de Honestidad Familiar · " + milestone)
                .description("El sistema detectó posibles respuestas idealizadas. Este espacio invita a la apertura genuina.")
                .dimension("COMUNICACION")
                .dueDate(LocalDateTime.now().plusDays(7))
                .fase(pillar.toUpperCase())
                .riesgoAsociado("MODERADO")
                .objetivo("Crear un ambiente de seguridad emocional donde cada miembro pueda expresar la verdad sin miedo.")
                .accionConcreta(
                    "Realiza el ejercicio 'La verdad con amor': cada miembro completa estas frases en voz alta — " +
                    "'Lo que realmente me cuesta en nuestra familia es…' y " +
                    "'Lo que más agradezco de nosotros es…'. Sin respuestas ni debates, solo escucha.")
                .indicadorCumplimiento("Todos los miembros participaron y se sintieron escuchados (confirmado en bitácora).")
                .evidenciaRequerida("Nota en bitácora con las frases compartidas por cada miembro.")
                .impactoIcf(10)
                .pillarName(pillar)
                .milestoneCode(milestone)
                .riskType("simulacion_detectada")
                .missionGenerator("APERTURA_EMOCIONAL")
                .memberType("familia")
                .completed(false)
                .steps(new ArrayList<>())
                .build();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Consulta el banco de preguntas para obtener metadatos representativos del hito+dimensión */
    private QuestionMeta resolveQuestionMeta(String milestone, String dim) {
        List<Question> pool = questionRepository.findByMilestoneCodeAndTypeAndActiveTrue(milestone, "CORE");
        Optional<Question> match = pool.stream()
                .filter(q -> dim.equalsIgnoreCase(q.getDimension()))
                .findFirst();

        if (match.isPresent()) {
            Question q = match.get();
            return new QuestionMeta(
                    q.getRiskType() != null ? q.getRiskType() : defaultRiskType(dim),
                    q.getMissionGenerator() != null ? q.getMissionGenerator() : defaultMission(dim),
                    q.getEvidenceType()
            );
        }
        return new QuestionMeta(defaultRiskType(dim), defaultMission(dim), null);
    }

    private String buildTitle(String dim, String riskType, String milestone, String pillar, String priority) {
        String dimLabel = switch (dim) {
            case "emociones"    -> "Regulación Emocional";
            case "comunicacion" -> "Comunicación Consciente";
            case "habitos"      -> "Hábitos Familiares";
            case "tiempos"      -> "Presencia y Tiempos";
            default             -> dim;
        };
        String pillarEmoji = switch (pillar) {
            case "reconocimiento" -> "💛";
            case "amor"           -> "❤️";
            case "entrega"        -> "👑";
            default               -> "✨";
        };
        return pillarEmoji + " " + dimLabel + " · " + milestone
               + (priority.equals("principal") ? " ★" : "");
    }

    private String resolveCurrentMilestone(Evaluation eval) {
        String m = eval.getMilestoneKey();
        if (m == null && eval.getFamily() != null) m = eval.getFamily().getCurrentMilestone();
        if (m == null || m.isBlank() || "MES_00_DIAGNOSTICO_BASE".equals(m) || "MES_00_DIAGNOSTICO".equals(m)) return "W1";
        return m;
    }

    private String resolvePillar(String milestone) {
        return switch (milestone) {
            case "W1", "M1", "M3"                   -> "reconocimiento";
            case "M6", "M9", "M12"                  -> "amor";
            case "M18", "M24", "M30", "M36"         -> "entrega";
            default                                  -> "reconocimiento";
        };
    }

    private int resolveDueDays(String milestone) {
        return switch (milestone) {
            case "W1"  -> 7;
            case "M1"  -> 30;
            case "M3"  -> 90;
            case "M6"  -> 180;
            case "M9"  -> 270;
            case "M12" -> 365;
            case "M18" -> 540;
            case "M24" -> 730;
            case "M30" -> 910;
            case "M36" -> 1095;
            default    -> 30;
        };
    }

    private String defaultRiskType(String dim) {
        return switch (dim) {
            case "emociones"    -> "desconexion_emocional";
            case "comunicacion" -> "comunicacion_toxica";
            case "habitos"      -> "falta_acuerdos";
            case "tiempos"      -> "ausencia_temporal";
            default             -> "desconexion_emocional";
        };
    }

    private String defaultMission(String dim) {
        return switch (dim) {
            case "emociones"    -> "ESTABILIZACION_EMOCIONAL";
            case "comunicacion" -> "COMUNICACION_CONSCIENTE";
            case "habitos"      -> "CONSOLIDACION_HABITOS";
            case "tiempos"      -> "PRESENCIA_CONSCIENTE";
            default             -> "ESTABILIZACION_EMOCIONAL";
        };
    }

    private String evidenceTypeLabel(String raw) {
        return switch (raw.toLowerCase()) {
            case "bitacora"    -> "Entrada en la bitácora familiar con fecha y descripción de la actividad.";
            case "fotografica" -> "Fotografía de la actividad realizada compartida en el perfil familiar.";
            case "conductual"  -> "Nota de comportamiento observado redactada por un adulto del hogar.";
            default            -> "Registro en bitácora familiar.";
        };
    }

    /** Metadatos extraídos del banco de preguntas */
    private record QuestionMeta(String riskType, String missionGenerator, String evidenceType) {}
}
