package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.EmotionalContentDtos.*;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionalContentEngineService {

    private final EmotionalStimulusRepository emotionalStimulusRepository;
    private final ReflectiveSessionRepository reflectiveSessionRepository;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtiene el estímulo reflexivo activo (por defecto, el video más reciente).
     */
    public Optional<EmotionalStimulus> getActiveStimulus() {
        return emotionalStimulusRepository.findFirstByTypeOrderByCreatedAtDesc("VIDEO");
    }

    /**
     * Procesa la reflexión introspectiva del miembro de la familia usando Claude AI.
     */
    @Transactional
    public EmotionalInferenceDto processReflection(Long stimulusId, ReflectionRequest request) {
        log.info("🧠 [EMOTIONAL-ENGINE] Iniciando análisis reflexivo para Estímulo ID: {} | Miembro ID: {}", stimulusId, request.memberId());

        Family family = familyRepository.findById(request.familyId())
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada con ID: " + request.familyId()));

        FamilyMember member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new IllegalArgumentException("Miembro no encontrado con ID: " + request.memberId()));

        EmotionalStimulus stimulus = emotionalStimulusRepository.findById(stimulusId)
                .orElseThrow(() -> new IllegalArgumentException("Estímulo no encontrado con ID: " + stimulusId));

        // 1. Verificar si ya existe una reflexión previa para evitar duplicidades
        Optional<ReflectiveSession> existing = reflectiveSessionRepository
                .findFirstByFamilyIdAndMemberIdAndStimulusId(family.getId(), member.getId(), stimulus.getId());

        if (existing.isPresent()) {
            try {
                JsonNode node = objectMapper.readTree(existing.get().getInferenceResult());
                return mapJsonNodeToDto(node);
            } catch (Exception e) {
                log.warn("⚠️ Error parseando inferencia previa guardada en BD, recalculando...", e);
            }
        }

        // 2. Construir prompt estructurado para Claude
        String prompt = String.format("""
            Eres Sentinel AI, el motor de inteligencia artificial conductual y sistémica de la plataforma Integrity Family.
            Tu misión es realizar una Inferencia Cualitativa de Consciencia Relacional a partir de la autoevaluación y reflexión escrita de un miembro de la familia tras ver el estímulo reflexivo.
            
            ESTÍMULO EVALUADO:
            - Título: %s
            - Categoría: %s
            - Tipo: %s
            
            DATOS DEL REFLEXIONANTE:
            - Rol en el Hogar: %s
            - Puntuación de Bienestar Familiar Autodeclarada: %d de 5
            
            REFLEXIÓN ESCRITA DEL USUARIO:
            "%s"
            
            Tu objetivo es inferir los siguientes 5 indicadores psicométricos (valores enteros entre 1 y 5, donde 1 es Nulo/Inconsciente y 5 es Pleno):
            1. empathy (Empatía / Capacidad de ver al otro)
            2. avoidance (Evitación / Evasión de tensiones relacionales)
            3. disconnection (Desconexión Emocional / Distanciamiento pasivo)
            4. activePresence (Presencia Activa / Atención plena en el hogar)
            5. reactivity (Reactividad / Impulsividad defensiva)
            
            Además, proporciona un "feedback" empático, cálido y clínicamente asertivo (máximo 300 caracteres) y una "recommendedAction" (micro-misión familiar práctica e intuitiva para sintonizar el hogar hoy).
            
            Debes responder ÚNICAMENTE con un objeto JSON válido que contenga la estructura exacta (sin preámbulos ni explicaciones adicionales, solo el JSON):
            {
              "empathy": (entero 1-5),
              "avoidance": (entero 1-5),
              "disconnection": (entero 1-5),
              "activePresence": (entero 1-5),
              "reactivity": (entero 1-5),
              "feedback": "Párrafo de acompañamiento en español",
              "recommendedAction": "Título y descripción breve de la micro-misión sugerida"
            }
            """,
            stimulus.getTitle(), stimulus.getCategory(), stimulus.getType(),
            member.getRole() != null ? member.getRole() : "FAMILIA",
            request.emotionalScore(),
            request.reflection()
        );

        String jsonResponse = "";
        EmotionalInferenceDto inference = null;

        // 3. Ejecutar inferencia AI o fallback de Simulación SDD
        try {
            jsonResponse = aiProvider.generateRawResponse(prompt).trim();

            // Limpieza de envoltura Markdown en JSON
            if (jsonResponse.startsWith("```")) {
                jsonResponse = jsonResponse.replaceAll("^```json\\s*", "");
                jsonResponse = jsonResponse.replaceAll("^```\\s*", "");
                jsonResponse = jsonResponse.replaceAll("\\s*```$", "");
                jsonResponse = jsonResponse.trim();
            }

            JsonNode node = objectMapper.readTree(jsonResponse);
            inference = mapJsonNodeToDto(node);

        } catch (Exception e) {
            log.warn("❌ Falla en la inferencia real de Claude, aplicando Fallback Adaptativo Híbrido (SDD).");
            
            // Construir fallback inteligente según palabras clave de la reflexión
            String text = request.reflection().toLowerCase();
            int empathy = 3, avoidance = 2, disconnection = 3, presence = 3, reactivity = 2;
            String feedback = "Gracias por compartir tu sentir. Sentinel AI observa una hermosa búsqueda de consciencia. A veces la rutina diaria nos nubla la historia de los demás, pero dar este primer paso abre el canal de conexión.";
            String action = "Cámara de Descompresión: Dedica 10 minutos a escuchar a tu familia en la cena de hoy sin opinar o juzgar.";

            if (text.contains("pantalla") || text.contains("celular") || text.contains("teléfono")) {
                disconnection = 4;
                presence = 2;
                feedback = "Sentinel AI detecta una desconexión silenciosa propiciada por la inercia digital en el hogar. Es un hermoso momento para re-aprender a mirarse a los ojos.";
                action = "Cesta de Dispositivos: Coloquen todos los celulares en una cesta común durante la cena de hoy para sintonizar en presencia plena.";
            } else if (text.contains("estrés") || text.contains("cansado") || text.contains("trabajo")) {
                reactivity = 4;
                presence = 2;
                feedback = "Sentinel AI percibe una reactividad defensiva secundaria al agotamiento. Recuerda que no necesitas hacerlo perfecto, solo estar presente con amor voluntario.";
                action = "Abrazo Silencioso de 20 Segundos: Regala un abrazo de contención física de 20 segundos a quien notes más tenso en casa al llegar hoy.";
            } else if (text.contains("conversar") || text.contains("charlar") || text.contains("escuchar")) {
                empathy = 5;
                presence = 4;
                feedback = "¡Qué maravillosa sintonía cualitativa! Sentinel AI celebra tu empatía y voluntad para priorizar momentos reales de conexión afectiva en el hogar.";
                action = "El Círculo de la Palabra: Pregúntale a cada miembro en la mesa qué fue lo que más le asombró de su día hoy.";
            }

            inference = EmotionalInferenceDto.builder()
                    .empathy(empathy)
                    .avoidance(avoidance)
                    .disconnection(disconnection)
                    .activePresence(presence)
                    .reactivity(reactivity)
                    .feedback(feedback)
                    .recommendedAction(action)
                    .build();

            try {
                jsonResponse = objectMapper.writeValueAsString(inference);
            } catch (Exception ex) {
                jsonResponse = "{}";
            }
        }

        // 4. Persistir la Sesión Reflexiva en Base de Datos
        ReflectiveSession session = ReflectiveSession.builder()
                .family(family)
                .member(member)
                .stimulus(stimulus)
                .reflection(request.reflection())
                .emotionalScore(request.emotionalScore())
                .inferenceResult(jsonResponse)
                .build();

        reflectiveSessionRepository.save(session);
        log.info("✅ [EMOTIONAL-ENGINE] Sesión reflexiva persistida correctamente.");

        return inference;
    }

    /**
     * Calcula las estadísticas emocionales de la familia y el Índice de Observación Consciente (IOC).
     */
    public FamilyEmotionalStats getFamilyStats(Long familyId) {
        List<ReflectiveSession> sessions = reflectiveSessionRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);

        if (sessions.isEmpty()) {
            return FamilyEmotionalStats.builder()
                    .ioc(0.0)
                    .totalReflections(0)
                    .averageEmpathy(0.0)
                    .averagePresence(0.0)
                    .averageReactivity(0.0)
                    .build();
        }

        double totalEmpathy = 0.0;
        double totalPresence = 0.0;
        double totalReactivity = 0.0;
        int parsedCount = 0;

        for (ReflectiveSession session : sessions) {
            try {
                JsonNode node = objectMapper.readTree(session.getInferenceResult());
                totalEmpathy += node.path("empathy").asInt(3);
                totalPresence += node.path("activePresence").asInt(3);
                totalReactivity += node.path("reactivity").asInt(2);
                parsedCount++;
            } catch (Exception e) {
                log.warn("Error leyendo JSON de sesión ID: {}", session.getId());
            }
        }

        if (parsedCount == 0) {
            return FamilyEmotionalStats.builder()
                    .ioc(50.0)
                    .totalReflections(sessions.size())
                    .averageEmpathy(3.0)
                    .averagePresence(3.0)
                    .averageReactivity(2.0)
                    .build();
        }

        double avgEmpathy = totalEmpathy / parsedCount;
        double avgPresence = totalPresence / parsedCount;
        double avgReactivity = totalReactivity / parsedCount;

        // Fórmula clínica de IOC: (Empatía + Presencia + (6 - Reactividad)) / 15 * 100
        // Da una métrica sobre 100.0, donde penaliza la reactividad alta.
        double ioc = ((avgEmpathy + avgPresence + (6.0 - avgReactivity)) / 15.0) * 100.0;
        ioc = Math.min(100.0, Math.max(0.0, Math.round(ioc * 10.0) / 10.0));

        return FamilyEmotionalStats.builder()
                .ioc(ioc)
                .totalReflections(sessions.size())
                .averageEmpathy(Math.round(avgEmpathy * 10.0) / 10.0)
                .averagePresence(Math.round(avgPresence * 10.0) / 10.0)
                .averageReactivity(Math.round(avgReactivity * 10.0) / 10.0)
                .build();
    }

    private EmotionalInferenceDto mapJsonNodeToDto(JsonNode node) {
        return EmotionalInferenceDto.builder()
                .empathy(node.path("empathy").asInt(3))
                .avoidance(node.path("avoidance").asInt(2))
                .disconnection(node.path("disconnection").asInt(3))
                .activePresence(node.path("activePresence").asInt(3))
                .reactivity(node.path("reactivity").asInt(2))
                .feedback(node.path("feedback").asText("Gracias por compartir tu sentir."))
                .recommendedAction(node.path("recommendedAction").asText("Misión del día recomendada para la convivencia."))
                .build();
    }
}
