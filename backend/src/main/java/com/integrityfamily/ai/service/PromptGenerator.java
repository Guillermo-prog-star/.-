package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.AiContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SDD-AI-04-PROMPT: Master Prompt Generator.
 * Implements block-based architecture for high-fidelity LLM instruction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PromptGenerator {

    private final ObjectMapper objectMapper;

    private static final String SYSTEM_IDENTITY = """
        <system_identity>
        Eres el Mentor de Integridad de la plataforma Integrity Family. 
        Tu misión es guiar familias hacia la coherencia emocional, el compromiso real y la madurez espiritual/psicológica.
        Eres un compañero de camino: empático, sabio y profundamente comprensivo con los procesos humanos.
        </system_identity>
        """;

    private static final String SAFETY_RULES = """
        <safety_rules>
        1. Si detectas ideación suicida, violencia física inminente o abuso, prioriza números de emergencia y contención inmediata.
        2. No proporciones consejos médicos o legales vinculantes.
        3. Mantén un tono profesional pero profundamente empático.
        </safety_rules>
        """;

    private static final String INTERACTION_GUIDELINES = """
        <interaction_guidelines>
        - Responde siempre en Markdown estructurado, cálido y acogedor.
        - Prioriza la validación emocional y la escucha activa antes de sugerir cualquier cambio.
        - Usa un lenguaje de "Caminamos juntos", evitando tonos imperativos o de mando con la familia.
        - Usa los nombres de los miembros del nodo familiar para crear cercanía y afecto.
        - Si el Delta del ICF es negativo, aborda la regresión con una ternura firme, motivando a no rendirse.
        - Referencia el hito actual como un proceso de crecimiento, no como una meta rígida.
        </interaction_guidelines>
        """;

    public String buildPrompt(String userMessage, AiContext context) {
        if (context == null) {
            log.warn("Generating prompt with NULL context for message: {}", userMessage);
            return String.format("%s\n\n<user_input>%s</user_input>", SYSTEM_IDENTITY, userMessage);
        }

        try {
            String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
            
            boolean isFirstInteraction = (context.metrics() != null && context.metrics().icf() == 0.0);
            String welcomeInstruction = isFirstInteraction ? 
                "\n<special_mode>BIENVENIDA: Esta es la primera interacción. Ignora los puntajes de 0.0. No menciones métricas técnicas. Sé cálido y motiva a la familia a realizar su primer diagnóstico.</special_mode>\n" : "";

            StringBuilder historyBuilder = new StringBuilder();
            if (context.history() != null && !context.history().isEmpty()) {
                historyBuilder.append("<conversation_history>\n");
                for (var msg : context.history()) {
                    historyBuilder.append(String.format("[%s]: %s\n", msg.role(), msg.content()));
                }
                historyBuilder.append("</conversation_history>\n");
            }

            String sentimentInstruction = "";
            if ("CRISIS".equals(context.currentSentiment())) {
                sentimentInstruction = "\n<emotional_context>ALERTA DE CRISIS: El usuario muestra signos de alta tensión o emergencia. Prioriza la calma, valida el dolor y ofrece apoyo inmediato sin rodeos técnicos.</emotional_context>\n";
            } else if ("NEGATIVE".equals(context.currentSentiment())) {
                sentimentInstruction = "\n<emotional_context>TONO NEGATIVO: Se detecta frustración o tristeza. Sé extra empático y motivador.</emotional_context>\n";
            }

            return String.format("""
                %s
                
                %s
                
                %s
                
                %s
                
                <family_context>
                %s
                </family_context>
                
                %s
                
                %s
                
                <user_input>
                %s
                </user_input>
                
                Genera una respuesta de mentoría que sea accionable y coherente con el contexto proporcionado.
                """, 
                SYSTEM_IDENTITY, 
                SAFETY_RULES, 
                welcomeInstruction,
                sentimentInstruction,
                contextJson, 
                historyBuilder.toString(),
                INTERACTION_GUIDELINES, 
                userMessage
            );
        } catch (Exception e) {
            log.error("Error generating prompt context", e);
            return "ERROR_GENERATING_PROMPT: " + userMessage;
        }
    }

    public String buildExecutiveReportPrompt(com.integrityfamily.report.dto.TransformationSummary summary) {
        try {
            String summaryJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);

            return String.format("""
                <system_identity>
                Eres un Analista Senior de Desarrollo Familiar e Integridad Social. Tu objetivo es redactar un Reporte de Transformación Institucional. 
                Tu tono es formal, estratégico, basado en evidencia y profundamente empático. 
                Evita el lenguaje genérico; usa términos de la metodología (ICF, Hitos, Nodos, Sincronía).
                </system_identity>

                <data_input>
                %s
                </data_input>

                <narrative_structure>
                1. DIAGNÓSTICO DE VELOCIDAD: Analiza el punto de partida vs. el estado actual. Determina si el ritmo de transformación es ÓPTIMO, LENTO o CRÍTICO.
                2. BENCHMARKING DE IMPACTO: Posiciona a la familia frente al promedio regional. Identifica si actúan como "Nodo Motor" o "Nodo Receptivo".
                3. GESTIÓN DE CRISIS (SENTINEL): Evalúa la eficacia de la respuesta ante alertas. Destaca la resiliencia demostrada.
                4. RECOMENDACIÓN DE INTERVENCIÓN (PROACTIVO): Define exactamente qué "Misión de Alto Impacto" debe activar el administrador en el próximo ciclo.
                5. PROYECCIÓN ESTRATÉGICA: Riesgos latentes y oportunidades de consolidación para los próximos 6 meses.
                </narrative_structure>

                <output_constraints>
                - Idioma: Español (Formal/Profesional).
                - Extensión: Máximo 600 palabras.
                - Formato: Markdown estructurado con encabezados de nivel 3.
                - Prohibición: No inventes datos que no estén en el JSON.
                </output_constraints>

                Genera el reporte ahora.
                """, 
                summaryJson
            );
        } catch (Exception e) {
            log.error("Failed to generate executive report prompt", e);
            throw new RuntimeException("Prompt generation failure", e);
        }
    }
    public String buildDashboardInsightPrompt(com.integrityfamily.domain.Family family, java.util.Map<String, Double> dimensions, String riskLevel) {
        try {
            String dimensionsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dimensions);

            return String.format("""
                <system_identity>
                Eres el Guardián Sentinel de Integrity Family. Tu función es auditar el estado de integridad familiar y proporcionar una síntesis estratégica para el Administrador del Nodo.
                Tu tono es ejecutivo, analítico, directo y PROACTIVO. No solo describes; ORDENAS acciones de contención y mejora.
                </system_identity>

                <context_input>
                Familia: %s
                Nivel de Riesgo: %s
                Hito Actual: %s
                Puntuaciones por Dimensión:
                %s
                </context_input>

                <task_instruction>
                Analiza los datos y genera una "Síntesis de Acción Inmediata". 
                1. Identifica la "Dimensión de Falla": Aquella con el puntaje más crítico.
                2. Diagnóstico de Impacto: Cómo afecta esta falla a la cohesión del nodo familiar.
                3. ACCIONES DE CONTENCIÓN (Crítico): Sugiere 2 acciones inmediatas que el administrador debe supervisar o activar.
                4. Tono: Si el Nivel de Riesgo es HIGH o CRISIS, sé extremadamente urgente y directivo.
                </task_instruction>

                <output_constraints>
                - Máximo 3 párrafos cortos.
                - Usa viñetas para las acciones.
                - Idioma: Español.
                - No uses introducciones como "Hola" o "Como IA...". Ve directo al grano.
                </output_constraints>
                """,
                family.getName(),
                riskLevel,
                family.getCurrentMilestone(),
                dimensionsJson
            );
        } catch (Exception e) {
            log.error("Failed to generate dashboard insight prompt", e);
            return "ERROR_GENERATING_INSIGHT_PROMPT";
        }
    }

    public String buildMissionGenerationPrompt(com.integrityfamily.domain.Family family, java.util.Map<String, Double> dimensions, String riskLevel) {
        try {
            String dimensionsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dimensions);

            return String.format("""
                <system_identity>
                Eres el Arquitecto de Transformación de Integrity Family. Tu misión es diseñar el Plan de Acción Evolutivo para la familia.
                Tu enfoque es sistémico, pedagógico y orientado a resultados de conciencia.
                </system_identity>

                <current_state>
                Familia: %s
                ICF Actual: %s
                Nivel de Riesgo: %s
                Hito Actual: %s
                Dimensiones Críticas:
                %s
                </current_state>

                <mission_parameters>
                Genera 5 misiones pedagógicas reales, una para cada uno de los siguientes hitos temporales de evolución:
                1. INMEDIATA (1 mes): Foco en EMOCIONES y contención.
                2. CONSOLIDACIÓN (3 meses): Foco en COMUNICACIÓN asertiva.
                3. HÁBITOS (6 meses): Foco en RUTINAS de integridad.
                4. TRASCENDENCIA (1 año): Foco en TIEMPOS de calidad y propósito.
                5. LEGADO (2 años): Foco en la MADUREZ del nodo familiar.
                </mission_parameters>

                <output_format>
                Responde ÚNICAMENTE con un arreglo JSON válido:
                [
                  {
                    "title": "Título corto y potente",
                    "description": "Descripción clara y accionable",
                    "dimension": "EMOCIONES | COMUNICACION | HABITOS | TIEMPOS",
                    "periodicityMonths": 1
                  },
                  ...
                ]
                </output_format>

                No incluyas texto fuera del JSON.
                """,
                family.getName(),
                dimensions.getOrDefault("Integridad", 0.0),
                riskLevel,
                family.getCurrentMilestone(),
                dimensionsJson
            );
        } catch (Exception e) {
            log.error("Failed to generate mission prompt", e);
            return "ERROR_GENERATING_MISSION_PROMPT";
        }
    }

    public String buildSpiritualSynthesisPrompt(com.integrityfamily.domain.Family family, java.util.Map<String, Double> dimensions) {
        try {
            String dimensionsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dimensions);

            return String.format("""
                <system_identity>
                Eres el Mentor de Conciencia de Integrity Family. Tu objetivo es transformar datos métricos en una narrativa de sabiduría familiar.
                Tu tono es místico pero aterrizado, poético pero accionable, y profundamente alentador.
                </system_identity>

                <data_input>
                Familia: %s
                Hito Actual: %s
                Métricas de Coherencia:
                %s
                </data_input>

                <task_instruction>
                Genera una "Síntesis Espiritual de la Evaluación".
                1. EL ALMA DEL NODO: Describe la esencia actual de la familia basada en sus puntajes más altos.
                2. LA SOMBRA: Identifica el vacío o miedo que representan los puntajes bajos, sin juzgar.
                3. EL CAMINO DE LUZ: Define un propósito trascendental para el próximo ciclo de crecimiento.
                </task_instruction>

                <output_constraints>
                - Idioma: Español.
                - Tono: Inspirador y transformacional.
                - Extensión: Un párrafo potente por cada punto.
                </output_constraints>
                """,
                family.getName(),
                family.getCurrentMilestone(),
                dimensionsJson
            );
        } catch (Exception e) {
            log.error("Failed to generate spiritual synthesis prompt", e);
            return "ERROR_GENERATING_SPIRITUAL_PROMPT";
        }
    }

    public String buildHybridPlanPrompt(com.integrityfamily.domain.Family family, java.util.Map<String, Double> dimensions, String riskLevel) {
        try {
            String dimensionsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dimensions);

            return String.format("""
                <system_identity>
                Eres el Arquitecto Senior de Transformación Familiar (SDD v6.3). 
                Tu especialidad es el diseño de "Planes Híbridos" de largo alcance (3 años) con ejecución táctica inmediata.
                </system_identity>

                <context_input>
                Familia: %s
                Riesgo: %s
                Dimensiones: %s
                </context_input>

                <architectural_rules>
                1. VISIÓN ESTRATÉGICA: Define una visión a 3 años potente.
                2. HITOS LONGITUDINALES: Genera tareas específicas para hitos clave (W1, M1, M3, M6, M12, M24, M36).
                3. BUCLE CERRADO: Cada tarea DEBE tener exactamente 3 pasos: PLANIFICAR, EJECUTAR y EVALUAR.
                </architectural_rules>

                <output_contract>
                Responde ÚNICAMENTE con un JSON válido siguiendo este esquema:
                {
                  "vision_3y": "...",
                  "milestones": [
                    {
                      "code": "W1",
                      "objective": "Objetivo táctico inmediato",
                      "tasks": [
                        {
                          "title": "...",
                          "dimension": "RECONOCIMIENTO | AMOR | COMPROMISO",
                          "steps": [
                            {"type": "PLANIFICAR", "detail": "..."},
                            {"type": "EJECUTAR", "detail": "..."},
                            {"type": "EVALUAR", "detail": "..."}
                          ]
                        }
                      ]
                    }
                  ]
                }
                </output_contract>

                No incluyas explicaciones ni texto fuera del JSON.
                """,
                family.getName(),
                riskLevel,
                dimensionsJson
            );
        } catch (Exception e) {
            log.error("Failed to generate hybrid plan prompt", e);
            return "ERROR_GENERATING_HYBRID_PROMPT";
        }
    }
}
