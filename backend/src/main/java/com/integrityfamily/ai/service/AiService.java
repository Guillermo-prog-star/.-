package com.integrityfamily.ai.service;

import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.plan.domain.Plan;
import com.integrityfamily.plan.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final EvaluationRepository evaluationRepository;

    public AiService(EvaluationRepository evaluationRepository) {
        this.evaluationRepository = evaluationRepository;
    }

    public String chat(String message, Family family, String context) {
        if (family == null) return "Error: No hay contexto familiar detectado.";

        String input = message.toLowerCase();
        
        // --- PROCESADOR DE COMANDOS PEDAGÓGICOS ---
        
        // 1. EVALUACIONES
        if (input.contains("evaluación") || input.contains("meses") || input.contains("inicio")) {
            return "### 🏁 MISIÓN DE EVALUACIÓN\n" +
                   "Has activado el comando de diagnóstico. Para la familia **" + family.getName() + 
                   "**, nos encontramos en: **" + family.getCurrentMilestone() + "**. \n\n" +
                   "> *Acción:* Dirígete a 'Nuevo Diagnóstico' para sincronizar tu estado actual.";
        }

        // 2. CONTINUIDAD
        if (input.contains("práctica semanal")) {
            return "### 🔄 CONTINUIDAD: PRÁCTICA SEMANAL\n" +
                   "La virtud no es un acto, sino un hábito. Esta semana el foco de **" + family.getName() + 
                   "** es la templanza.\n\n" +
                   "¿Cómo han vivido el orden en las comidas hoy?";
        }

        // 3. ANÁLISIS
        if (input.contains("ruta") || input.contains("plan longitudinal") || input.contains("perfil")) {
            return "### 🗺️ ANÁLISIS LONGITUDINAL\n" +
                   "Visualizando la ruta de 36 meses...\n" +
                   "- Hito Actual: **" + family.getCurrentMilestone() + "**\n" +
                   "- Próxima Meta: " + calculateNextMilestone(family.getCurrentMilestone()) + "\n\n" +
                   "> *Perspectiva:* Estás transformando el futuro de tus próximas 3 generaciones.";
        }

        // 4. MOMENTOS (CRISIS)
        if (input.contains("día crítico") || input.contains("crisis")) {
            return "### 🚨 MOMENTOS: PROTOCOLO DE PAZ\n" +
                   "**ALERTA SENTINEL:** Se ha activado el protocolo de emergencia.\n\n" +
                   "1. Silencio proactivo (no responder a la ofensa).\n" +
                   "2. Espacio de seguridad (15 min de separación).\n" +
                   "3. Re-conexión desde la vulnerabilidad.\n\n" +
                   "¿Deseas activar una sesión de mentoría urgente?";
        }

        // 5. SEGUIMIENTO
        if (input.contains("checklist")) {
            return "### 📋 SEGUIMIENTO DE COMPROMISOS\n" +
                   "Revisando el tablero de misiones...\n" +
                   "- Tareas de Plan: 2 activas\n" +
                   "- Rutina de Mañana: 80% completada.\n\n" +
                   "¿Deseas marcar alguna actividad como realizada?";
        }

        // 6. SESIÓN
        if (input.contains("cambiar familia") || input.contains("main menu")) {
            return "[COMMAND:CHANGE_FAMILY] ### 🔄 CAMBIO DE NÚCLEO\n" +
                   "Cerrando sesión de consultoría para **" + family.getName() + "**. \n" +
                   "Redirigiendo al menú de acceso...";
        }

        // --- LÓGICA DE RESPUESTA POR DEFECTO (TENSIÓN) ---
        boolean isUserChallenging = input.contains("no") || input.contains("aburrido") || input.contains("mismo");
        
        StringBuilder reply = new StringBuilder();
        reply.append("### CONSULTOR DE INTEGRIDAD\n\n");
        
        if (isUserChallenging) {
            reply.append("> **PEDAGOGÍA DE LA TENSIÓN:** Tu resistencia es el primer paso hacia el cambio real. ¿Qué parte de tu rutina familiar estás defendiendo con tanto ahínco?\n\n");
        } else {
            reply.append("> **JUICIO AUTORREGULADO:** Buscas una respuesta, pero la verdad reside en los silencios de tu hogar. ¿Qué no estás diciendo hoy?\n\n");
        }

        reply.append("📊 **ESTADO:** ").append(family.getName()).append(" | Hito: ").append(family.getCurrentMilestone()).append("\n");
        return reply.toString();
    }

    private String calculateNextMilestone(String current) {
        if (current == null) return "Diagnóstico Base";
        if (current.contains("36")) return "Ciclo Completado";
        if (current.contains("30")) return "Transformación Completa (36 Meses)";
        if (current.contains("24")) return "Cierre y Sostenimiento (30 Meses)";
        if (current.contains("18")) return "Madurez del Sistema (24 Meses)";
        if (current.contains("12")) return "Profundización (18 Meses)";
        if (current.contains("06")) return "Primera Transformación (12 Meses)";
        if (current.contains("03")) return "Consolidación Inicial (6 Meses)";
        return "Primeros Cambios (3 Meses)";
    }

    private int calculateRemainingMonths(String currentMilestone) {
        if (currentMilestone == null) return 36;
        try {
            if (currentMilestone.contains("36")) return 0;
            if (currentMilestone.contains("30")) return 6;
            if (currentMilestone.contains("24")) return 12;
            if (currentMilestone.contains("18")) return 18;
            if (currentMilestone.contains("12")) return 24;
            if (currentMilestone.contains("06")) return 30;
            if (currentMilestone.contains("03")) return 33;
            return 36;
        } catch (Exception e) {
            return 36;
        }
    }

    public String generateSynthesis(Map<String, Object> context) {
        return "### SÍNTESIS GENERADA\nEl flujo de transformación está activo para " + context.get("familyName");
    }

    public String getPersonalizedAdvice(Map<String, Double> scores) {
        return "### EL ESPEJO\n¿Este puntaje es un cambio real o solo una coartada?";
    }
}