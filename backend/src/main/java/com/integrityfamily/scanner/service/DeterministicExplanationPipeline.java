package com.integrityfamily.scanner.service;

import com.integrityfamily.risk.service.RiskAlgoV1Engine;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * IF-DEP: Pipeline de Explicación Determinístico.
 *
 * Garantía central: misma entrada (AlgoResult + rol) → misma explicación.
 * La síntesis NO depende de creatividad generativa de IA.
 * Depende exclusivamente de: riskLevel, criticalDimension, rol del miembro,
 * consciousnessLabel, UncertaintyVector y flags de señales especiales.
 *
 * Explicaciones prohibidas: diagnósticos clínicos, etiquetas de trastornos,
 * atribución de culpa, interpretaciones absolutas.
 *
 * Explicaciones permitidas: observaciones conductuales, tendencias, misiones,
 * advertencias de incertidumbre, alertas de recaída.
 *
 * Niveles de explicación: familiar (UI), clínico (consultor), técnico (log).
 */
@Service
public class DeterministicExplanationPipeline {

    // ── Plantillas de observación: riskLevel + "_" + rolNormalizado ──────────

    private static final Map<String, String> OBSERVATION = Map.ofEntries(
        // BAJO
        Map.entry("BAJO_PADRE",       "Se observa estabilidad en el liderazgo emocional y presencia consciente."),
        Map.entry("BAJO_MADRE",       "Se observa equilibrio en la carga mental y vínculos emocionales activos."),
        Map.entry("BAJO_ADOLESCENTE", "Se observa expresión emocional segura y sentido de pertenencia familiar."),
        Map.entry("BAJO_NINO",        "Se observan hábitos positivos consolidados y juego consciente activo."),
        Map.entry("BAJO_OTRO",        "Se observa participación familiar activa y cohesión emocional estable."),
        // MODERADO
        Map.entry("MODERADO_PADRE",       "Se detectan tensiones intermitentes en la conexión con los hijos."),
        Map.entry("MODERADO_MADRE",       "Se detecta sobrecarga emocional con señales de agotamiento intermitente."),
        Map.entry("MODERADO_ADOLESCENTE", "Se detectan dificultades en la comunicación y búsqueda de autonomía."),
        Map.entry("MODERADO_NINO",        "Se detectan irregularidades en rutinas y necesidad de atención emocional."),
        Map.entry("MODERADO_OTRO",        "Se detectan desajustes relacionales en la dinámica familiar."),
        // ALTO
        Map.entry("ALTO_PADRE",       "Se observa desconexión emocional sostenida y reducción de presencia vincular."),
        Map.entry("ALTO_MADRE",       "Se observa sobrecarga crónica con señales de agotamiento y aislamiento."),
        Map.entry("ALTO_ADOLESCENTE", "Se observa retraimiento emocional y distancia relacional creciente."),
        Map.entry("ALTO_NINO",        "Se observan señales de inseguridad emocional y desorientación en rutinas."),
        Map.entry("ALTO_OTRO",        "Se observa deterioro relacional significativo en el núcleo familiar."),
        // CRITICO
        Map.entry("CRITICO_PADRE",       "Se detecta fragmentación del rol parental con riesgo de ruptura vincular."),
        Map.entry("CRITICO_MADRE",       "Se detecta colapso emocional crítico con necesidad urgente de apoyo."),
        Map.entry("CRITICO_ADOLESCENTE", "Se detecta crisis de identidad y desconexión severa del núcleo familiar."),
        Map.entry("CRITICO_NINO",        "Se detectan señales de crisis emocional que requieren intervención inmediata."),
        Map.entry("CRITICO_OTRO",        "Se detecta crisis familiar crítica. Protocolo Sentinel activado.")
    );

    // ── Recomendación por dimensión crítica ──────────────────────────────────

    private static final Map<String, String> RECOMMENDATION_BY_DIM = Map.of(
        "emociones",    "Priorizar espacios de escucha activa y regulación emocional dentro del núcleo.",
        "comunicacion", "Establecer momentos de diálogo sin interrupciones ni dispositivos activos.",
        "habitos",      "Revisar y simplificar rutinas diarias para reducir fricción familiar.",
        "tiempos",      "Incrementar tiempo de calidad presente sin distracciones digitales."
    );

    // ── Etiquetas legibles para misiones ─────────────────────────────────────

    private static final Map<String, String> MISSION_LABEL = Map.of(
        "ESTABILIZACION_EMOCIONAL", "Estabilización Emocional",
        "COMUNICACION_CONSCIENTE",  "Comunicación Consciente",
        "CONSOLIDACION_HABITOS",    "Consolidación de Hábitos",
        "PRESENCIA_CONSCIENTE",     "Presencia Consciente"
    );

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Construye la narrativa familiar determinística.
     *
     * @param algo       resultado completo del RISK_ALGO_V1
     * @param memberRole rol del miembro (nullable → "OTRO")
     * @return texto estructurado para el campo spiritualSynthesis
     */
    public String buildFamiliarNarrative(RiskAlgoV1Engine.AlgoResult algo, String memberRole) {
        String role = normalizeRole(memberRole);
        String observationKey = algo.riskLevel() + "_" + role;

        String observation = OBSERVATION.getOrDefault(observationKey,
                "Se registraron patrones relacionales en el diagnóstico familiar.");

        String recommendation = RECOMMENDATION_BY_DIM.getOrDefault(
                algo.criticalDimension(),
                "Atender la dimensión más vulnerable con acciones concretas y sostenidas.");

        String missionLabel = MISSION_LABEL.getOrDefault(
                algo.suggestedMissionGenerator(), algo.suggestedMissionGenerator());

        StringBuilder sb = new StringBuilder();
        sb.append("[DIAGNÓSTICO CONSCIENTE]\n");
        sb.append("Observación: ").append(observation).append("\n");
        sb.append("Recomendación: ").append(recommendation).append("\n");
        sb.append("Misión activada: ").append(missionLabel).append("\n");
        sb.append("Nivel de consciencia: ").append(algo.consciousnessLabel()).append(".\n");

        if (algo.relapseDetected()) {
            sb.append("⚠ Alerta de recaída: señales críticas detectadas (")
              .append(String.join(", ", algo.relapseFlags()))
              .append("). Se requiere atención prioritaria.\n");
        }

        RiskAlgoV1Engine.UncertaintyVector u = algo.uncertainty();
        if (u != null && u.isHigh()) {
            sb.append("ⓘ Incertidumbre elevada (")
              .append(String.format("%.0f%%", u.total() * 100))
              .append("): diagnóstico requiere validación adicional con más señales.\n");
        }

        return sb.toString().trim();
    }

    /**
     * Explicación técnica para logs y auditoría científica.
     * Determinística: reconstruible exactamente a partir de los campos de AlgoResult.
     */
    public String buildTechnicalSummary(RiskAlgoV1Engine.AlgoResult algo) {
        RiskAlgoV1Engine.UncertaintyVector u = algo.uncertainty();
        return String.format(
            "ICF=%.1f [%s] critDim=%s conf=%.2f uncert=%.2f(%s) " +
            "consciousness=%s(%d) sim=%s relapse=%s algo=RISK_ALGO_V1",
            algo.healthyIndex(), algo.riskLevel(), algo.criticalDimension(),
            1.0 - (u != null ? u.total() : 0.0),
            u != null ? u.total() : 0.0,
            u != null ? u.level() : "N/A",
            algo.consciousnessLabel(), algo.consciousnessLevel(),
            algo.simulationSuspected(), algo.relapseDetected()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String normalizeRole(String role) {
        if (role == null) return "OTRO";
        return switch (role.toUpperCase().trim()) {
            case "PADRE"                     -> "PADRE";
            case "MADRE"                     -> "MADRE";
            case "ADOLESCENTE"               -> "ADOLESCENTE";
            case "NINO", "NIÑO", "HIJO", "HIJA" -> "NINO";
            default                          -> "OTRO";
        };
    }
}
