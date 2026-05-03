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
        Tu misiÃƒÂ³n es guiar familias hacia la coherencia emocional, el compromiso real y la madurez espiritual/psicolÃƒÂ³gica.
        Eres un compaÃƒÂ±ero de camino: empÃƒÂ¡tico, sabio y profundamente comprensivo con los procesos humanos.
        </system_identity>
        """;

    private static final String SAFETY_RULES = """
        <safety_rules>
        1. Si detectas ideaciÃƒÂ³n suicida, violencia fÃƒÂ­sica inminente o abuso, prioriza nÃƒÂºmeros de emergencia y contenciÃƒÂ³n inmediata.
        2. No proporciones consejos mÃƒÂ©dicos o legales vinculantes.
        3. MantÃƒÂ©n un tono profesional pero profundamente empÃƒÂ¡tico.
        </safety_rules>
        """;

    private static final String INTERACTION_GUIDELINES = """
        <interaction_guidelines>
        - Responde siempre en Markdown estructurado, cÃƒÂ¡lido y acogedor.
        - Prioriza la validaciÃƒÂ³n emocional y la escucha activa antes de sugerir cualquier cambio.
        - Usa un lenguaje de "Caminamos juntos", evitando tonos imperativos o de mando con la familia.
        - Usa los nombres de los miembros del nodo familiar para crear cercanÃƒÂ­a y afecto.
        - Si el Delta del ICF es negativo, aborda la regresiÃƒÂ³n con una ternura firme, motivando a no rendirse.
        - Referencia el hito actual como un proceso de crecimiento, no como una meta rÃƒÂ­gida.
        </interaction_guidelines>
        """;

    public String buildPrompt(String userMessage, AiContext context) {
        try {
            String contextJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(context);
            
            boolean isFirstInteraction = context.metrics().icf() == 0.0;
            String welcomeInstruction = isFirstInteraction ? 
                "\n<special_mode>BIENVENIDA: Esta es la primera interacciÃƒÂ³n. Ignora los puntajes de 0.0. No menciones mÃƒÂ©tricas tÃƒÂ©cnicas. SÃƒÂ© cÃƒÂ¡lido y motiva a la familia a realizar su primer diagnÃƒÂ³stico.</special_mode>\n" : "";

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
                sentimentInstruction = "\n<emotional_context>ALERTA DE CRISIS: El usuario muestra signos de alta tensiÃƒÂ³n o emergencia. Prioriza la calma, valida el dolor y ofrece apoyo inmediato sin rodeos tÃƒÂ©cnicos.</emotional_context>\n";
            } else if ("NEGATIVE".equals(context.currentSentiment())) {
                sentimentInstruction = "\n<emotional_context>TONO NEGATIVO: Se detecta frustraciÃƒÂ³n o tristeza. SÃƒÂ© extra empÃƒÂ¡tico y motivador.</emotional_context>\n";
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
                
                Genera una respuesta de mentorÃƒÂ­a que sea accionable y coherente con el contexto proporcionado.
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
                Eres un Analista Senior de Desarrollo Familiar e Integridad Social. Tu objetivo es redactar un Reporte de TransformaciÃƒÂ³n Institucional. 
                Tu tono es formal, estratÃƒÂ©gico, basado en evidencia y profundamente empÃƒÂ¡tico. 
                Evita el lenguaje genÃƒÂ©rico; usa tÃƒÂ©rminos de la metodologÃƒÂ­a (ICF, Hitos, Nodos, SincronÃƒÂ­a).
                </system_identity>

                <data_input>
                %s
                </data_input>

                <narrative_structure>
                1. DIAGNÃƒâ€œSTICO DE VELOCIDAD: Analiza el punto de partida vs. el estado actual. Determina si el ritmo de transformaciÃƒÂ³n es Ãƒâ€œPTIMO, LENTO o CRÃƒÂTICO.
                2. BENCHMARKING DE IMPACTO: Posiciona a la familia frente al promedio regional. Identifica si actÃƒÂºan como "Nodo Motor" o "Nodo Receptivo".
                3. GESTIÃƒâ€œN DE CRISIS (SENTINEL): EvalÃƒÂºa la eficacia de la respuesta ante alertas. Destaca la resiliencia demostrada.
                4. RECOMENDACIÃƒâ€œN DE INTERVENCIÃƒâ€œN (PROACTIVO): Define exactamente quÃƒÂ© "MisiÃƒÂ³n de Alto Impacto" debe activar el administrador en el prÃƒÂ³ximo ciclo.
                5. PROYECCIÃƒâ€œN ESTRATÃƒâ€°GICA: Riesgos latentes y oportunidades de consolidaciÃƒÂ³n para los prÃƒÂ³ximos 6 meses.
                </narrative_structure>

                <output_constraints>
                - Idioma: EspaÃƒÂ±ol (Formal/Profesional).
                - ExtensiÃƒÂ³n: MÃƒÂ¡ximo 600 palabras.
                - Formato: Markdown estructurado con encabezados de nivel 3.
                - ProhibiciÃƒÂ³n: No inventes datos que no estÃƒÂ©n en el JSON.
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
                Eres el GuardiÃƒÂ¡n Sentinel de Integrity Family. Tu funciÃƒÂ³n es auditar el estado de integridad familiar y proporcionar una sÃƒÂ­ntesis estratÃƒÂ©gica para el Administrador del Nodo.
                Tu tono es ejecutivo, analÃƒÂ­tico, directo y PROACTIVO. No solo describes; ORDENAS acciones de contenciÃƒÂ³n y mejora.
                </system_identity>

                <context_input>
                Familia: %s
                Nivel de Riesgo: %s
                Hito Actual: %s
                Puntuaciones por DimensiÃƒÂ³n:
                %s
                </context_input>

                <task_instruction>
                Analiza los datos y genera una "SÃƒÂ­ntesis de AcciÃƒÂ³n Inmediata". 
                1. Identifica la "DimensiÃƒÂ³n de Falla": Aquella con el puntaje mÃƒÂ¡s crÃƒÂ­tico.
                2. DiagnÃƒÂ³stico de Impacto: CÃƒÂ³mo afecta esta falla a la cohesiÃƒÂ³n del nodo familiar.
                3. ACCIONES DE CONTENCIÃƒâ€œN (CrÃƒÂ­tico): Sugiere 2 acciones inmediatas que el administrador debe supervisar o activar.
                4. Tono: Si el Nivel de Riesgo es HIGH o CRISIS, sÃƒÂ© extremadamente urgente y directivo.
                </task_instruction>

                <output_constraints>
                - MÃƒÂ¡ximo 3 pÃƒÂ¡rrafos cortos.
                - Usa viÃƒÂ±etas para las acciones.
                - Idioma: EspaÃƒÂ±ol.
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
}


