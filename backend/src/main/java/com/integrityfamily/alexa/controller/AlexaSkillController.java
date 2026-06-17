package com.integrityfamily.alexa.controller;

import com.integrityfamily.ai.dto.CopilotDtos.CopilotInferRequest;
import com.integrityfamily.ai.dto.CopilotDtos.StructuredAiInferenceResponse;
import com.integrityfamily.ai.service.CopilotService;
import com.integrityfamily.alexa.config.AlexaProperties;
import com.integrityfamily.alexa.domain.AlexaOAuthToken;
import com.integrityfamily.alexa.service.AlexaOAuthService;
import com.integrityfamily.bitacora.service.SprintService;
import com.integrityfamily.dna.service.FamilyDnaService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.guardian.dto.GuardianBriefingResponse;
import com.integrityfamily.guardian.service.GuardianBriefingService;
import com.integrityfamily.legado.service.LegacyService;
import com.integrityfamily.risk.service.CrisisService;
import com.integrityfamily.risk.service.RiskService;
import com.integrityfamily.timeline.service.FamilyTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Voz del Sistema Vivo de Integrity Family vía Alexa Skill Kit.
 *
 * Identidad: access_token → alexa_oauth_tokens → familia real.
 * Cada intent delega al servicio de dominio correspondiente; Alexa
 * nunca recibe lógica fabricada en este controlador.
 */
@RestController
@RequestMapping("/alexa")
@Slf4j
@RequiredArgsConstructor
public class AlexaSkillController {

    private final AlexaProperties        alexaProperties;
    private final AlexaOAuthService      alexaOAuthService;
    private final FamilyRepository       familyRepository;
    // ── Servicios de dominio ───────────────────────────────────────────────
    private final GuardianBriefingService guardianBriefingService;
    private final CrisisService           crisisService;
    private final RiskService             riskService;
    private final CopilotService          copilotService;
    private final SprintService           sprintService;
    private final FamilyDnaService        familyDnaService;
    private final LegacyService           legacyService;
    private final FamilyTimelineService   familyTimelineService;

    // ═══════════════════════════════════════════════════════════════════════
    // Entry point
    // ═══════════════════════════════════════════════════════════════════════

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleSkillRequest(
            @RequestHeader(value = "SignatureCertChainUrl", required = false) String certUrl,
            @RequestBody Map<String, Object> body) {

        if (!isValidSkillId(body)) {
            log.warn("[ALEXA-SKILL] skillId inválido");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String accessToken = extractAccessToken(body);
        if (accessToken == null) return ResponseEntity.ok(linkAccountResponse());

        AlexaOAuthToken token = alexaOAuthService.resolveToken(accessToken).orElse(null);
        if (token == null)     return ResponseEntity.ok(linkAccountResponse());

        Family family = familyRepository.findById(token.getFamilyId()).orElse(null);
        if (family == null)    return ResponseEntity.ok(speakResponse(
                "No encontré tu familia vinculada. Intenta vincular tu cuenta nuevamente."));

        String requestType = extractRequestType(body);

        if ("LaunchRequest".equals(requestType)) {
            return ResponseEntity.ok(handleLaunch(family));
        }

        if ("IntentRequest".equals(requestType)) {
            String intent = extractIntentName(body);
            Map<String, Object> response = switch (intent != null ? intent : "") {
                // ── Guardian ──────────────────────────────────────────────
                case "GuardianBriefingIntent"  -> handleGuardianBriefing(family);
                // ── Sentinel / riesgo ─────────────────────────────────────
                case "SentinelStatusIntent"    -> handleSentinelStatus(family);
                case "ActivateSentinelIntent"  -> handleActivateSentinel(family, body);
                // ── Consejo Familiar (Copilot IA) ─────────────────────────
                case "CopilotIntent"           -> handleCopilot(family);
                // ── Sprint y misión ───────────────────────────────────────
                case "CurrentMissionIntent"    -> handleCurrentMission(family);
                case "FamilyContextIntent"     -> handleFamilyContext(family);
                // ── ADN Familiar ──────────────────────────────────────────
                case "FamilyDnaIntent"         -> handleFamilyDna(family);
                // ── Legado y narrativa temporal ───────────────────────────
                case "LegacyMessageIntent"     -> handleLegacyMessage(family);
                case "TimelineIntent"          -> handleTimeline(family);
                // ── Ayuda / sistema ───────────────────────────────────────
                case "AMAZON.HelpIntent"       -> helpResponse(family);
                case "AMAZON.StopIntent",
                     "AMAZON.CancelIntent"     -> speakResponse("¡Hasta pronto, familia " + family.getName() + "!");
                default                        -> speakResponse(
                        "No entendí ese comando. Di «ayuda» para ver qué puedo hacer.");
            };
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(speakResponse("Hasta luego."));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Intents — críticos para el guardian
    // ═══════════════════════════════════════════════════════════════════════

    /** LaunchRequest: bienvenida contextual. */
    private Map<String, Object> handleLaunch(Family family) {
        boolean sentinel = Boolean.TRUE.equals(family.getSentinelActive());
        if (sentinel) {
            return speakResponse(
                    "Hola, familia " + family.getName() + ". Hay una situación que requiere atención. " +
                    "Di «estado Sentinel» para conocer los detalles, o «consejo familiar» para orientación.");
        }
        return speakResponseKeepSession(
                "Hola, familia " + family.getName() + ". Estoy aquí para apoyarlos. " +
                "Puedes decirme: resumen del guardián, misión actual, consejo familiar, ADN familiar o estado Sentinel.",
                "Integrity Family escuchando...");
    }

    /**
     * GuardianBriefingIntent — resumen operativo del guardián.
     * Usa GuardianBriefingService que ya calcula fatiga, participación y alertas.
     */
    private Map<String, Object> handleGuardianBriefing(Family family) {
        try {
            GuardianBriefingResponse b = guardianBriefingService.getBriefing(family.getId());

            String fatigaTexto = b.fatigueSignal() != null && !b.fatigueSignal().isBlank()
                    ? " Señal de fatiga detectada: " + b.fatigueSignal() + "."
                    : "";

            String participacion = b.activeParticipants() + " integrantes activos esta semana";
            String inactivos = b.inactiveParticipants() > 0
                    ? " y " + b.inactiveParticipants() + " inactivos"
                    : "";

            String milestoneTxt = b.currentMilestone() != null && !b.currentMilestone().isBlank()
                    ? " Hito actual: " + b.currentMilestone() + "."
                    : "";

            String completionTxt = String.format(" Avance del plan: %.0f por ciento.", b.planCompletionRate() * 100);

            String aiMsgTxt = b.aiMessage() != null && !b.aiMessage().isBlank()
                    ? " El sistema dice: " + b.aiMessage()
                    : "";

            String texto = "Resumen del guardián de la familia " + family.getName() + ". "
                    + participacion + inactivos + "."
                    + fatigaTexto
                    + milestoneTxt
                    + completionTxt
                    + aiMsgTxt;

            return speakResponse(texto);
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] GuardianBriefing error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener el resumen del guardián en este momento.");
        }
    }

    /**
     * SentinelStatusIntent — estado de riesgo real desde BD.
     * Combina isUnderCrisis + último snapshot de riesgo.
     */
    private Map<String, Object> handleSentinelStatus(Family family) {
        try {
            boolean enCrisis = crisisService.isUnderCrisis(family.getId());
            boolean sentinel  = Boolean.TRUE.equals(family.getSentinelActive());

            var snapshots = riskService.findByFamilyId(family.getId());
            String nivelTexto = "desconocido";
            if (!snapshots.isEmpty()) {
                var ultimo = snapshots.get(snapshots.size() - 1);
                if (ultimo.getRiskLevel() != null) {
                    nivelTexto = ultimo.getRiskLevel();
                }
            }

            String estado;
            if (enCrisis || sentinel) {
                estado = "La familia " + family.getName() + " está en alerta activa. "
                        + "Nivel de riesgo: " + nivelTexto + ". "
                        + "El guardian debe revisar el panel de Integrity Family para tomar acción.";
            } else {
                estado = "La familia " + family.getName() + " está estable. "
                        + "Nivel de riesgo: " + nivelTexto + ". "
                        + "Sentinel no detecta situaciones críticas en este momento.";
            }
            return speakResponse(estado);
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] SentinelStatus error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener el estado Sentinel. Revisa el panel de Integrity Family.");
        }
    }

    /**
     * ActivateSentinelIntent — registra crisis por voz.
     * Extrae el slot "nivel" si el usuario lo indicó.
     */
    private Map<String, Object> handleActivateSentinel(Family family, Map<String, Object> body) {
        try {
            String nivelSlot = extractSlot(body, "nivel");
            int nivel = parseNivelSentinel(nivelSlot);
            String descripcion = "Crisis nivel " + nivel + " reportada por voz desde Alexa";
            String categoria   = "ALEXA_REPORT";
            String emocion     = "INDEFINIDA";

            crisisService.registerCrisis(family.getId(), null, categoria, descripcion, emocion);

            return speakResponse(
                    "He registrado la situación crítica de nivel " + nivel + " para la familia " + family.getName() + ". "
                    + "El equipo de apoyo de Integrity Family ha sido notificado. "
                    + "Recuerda: en peligro inmediato llama a los servicios de emergencia locales.");
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] ActivateSentinel error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude activar Sentinel. Por favor usa la aplicación directamente.");
        }
    }

    /**
     * CopilotIntent — Consejo Familiar Autónomo.
     * Usa CopilotService que internamente sintetiza ADN + contexto + historial + IA.
     */
    private Map<String, Object> handleCopilot(Family family) {
        try {
            StructuredAiInferenceResponse resp = copilotService.generateInference(
                    new CopilotInferRequest(family.getId(), "ALEXA_CONSULTATION"));

            if (resp == null) {
                return speakResponse("El Consejo Familiar no tiene orientación disponible en este momento. "
                        + "Vuelve a intentarlo en unos minutos.");
            }

            StringBuilder texto = new StringBuilder("El Consejo Familiar dice: ");
            if (resp.summary() != null) texto.append(resp.summary()).append(". ");

            if (resp.recommendedActions() != null && !resp.recommendedActions().isEmpty()) {
                texto.append("Acciones recomendadas: ");
                int max = Math.min(resp.recommendedActions().size(), 3);
                for (int i = 0; i < max; i++) {
                    texto.append(i + 1).append(": ").append(resp.recommendedActions().get(i));
                    if (i < max - 1) texto.append(". ");
                }
                texto.append(". ");
            }

            if (resp.containmentSuggestion() != null && !resp.containmentSuggestion().isBlank()) {
                texto.append("Sugerencia inmediata: ").append(resp.containmentSuggestion());
            }

            return speakResponse(texto.toString().trim());
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] Copilot error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("El Consejo Familiar no está disponible ahora. Intenta de nuevo pronto.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Intents — sprint, DNA, legado, contexto
    // ═══════════════════════════════════════════════════════════════════════

    /** CurrentMissionIntent — misión real desde el sprint activo. */
    private Map<String, Object> handleCurrentMission(Family family) {
        try {
            var sprint = sprintService.getActiveSprint(family.getId());
            if (sprint == null) {
                return speakResponse("La familia " + family.getName() + " no tiene un sprint activo. "
                        + "Entra a Integrity Family para iniciar uno.");
            }
            String objective = sprint.objective() != null ? sprint.objective() : "objetivo no definido";
            String misionTexto = "La misión del sprint actual es: " + objective + ".";

            if (sprint.missions() != null && !sprint.missions().isEmpty()) {
                long pendientes = sprint.missions().stream()
                        .filter(m -> !"COMPLETED".equalsIgnoreCase(m.status()))
                        .count();
                misionTexto += " Tienen " + pendientes + " misiones pendientes esta semana.";
            }
            return speakResponse(misionTexto + " ¡Adelante, familia " + family.getName() + "!");
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] CurrentMission error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener la misión. Intenta de nuevo más tarde.");
        }
    }

    /** FamilyContextIntent — resumen compacto del estado real de la familia. */
    private Map<String, Object> handleFamilyContext(Family family) {
        try {
            var ctx = copilotService.buildContext(family.getId());
            String nivel    = ctx.riskLevel()        != null ? ctx.riskLevel()        : "desconocido";
            String dimension= ctx.criticalDimension() != null ? ctx.criticalDimension() : "sin dato";
            String tendencia= ctx.trend()            != null ? ctx.trend()            : "estable";
            double adherencia = ctx.adherence() != null ? ctx.adherence() : 0.0;
            int    inactividad= ctx.inactiveDays() != null ? ctx.inactiveDays() : 0;

            String alertas = "";
            if (ctx.alerts() != null && !ctx.alerts().isEmpty()) {
                alertas = " Alertas activas: " + String.join(", ", ctx.alerts()) + ".";
            }

            String texto = "Contexto de la familia " + family.getName() + ". "
                    + "Nivel de riesgo: " + nivel + ". "
                    + "Dimensión crítica: " + dimension + ". "
                    + "Tendencia: " + tendencia + ". "
                    + String.format("Adherencia al plan: %.0f por ciento. ", adherencia * 100)
                    + (inactividad > 0 ? "Días sin actividad registrada: " + inactividad + ". " : "")
                    + alertas;

            return speakResponse(texto.trim());
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] FamilyContext error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener el contexto familiar en este momento.");
        }
    }

    /** FamilyDnaIntent — ADN de la familia: valores, fortalezas, sombras. */
    private Map<String, Object> handleFamilyDna(Family family) {
        try {
            var dna = familyDnaService.findByFamilyId(family.getId()).orElse(null);
            if (dna == null) {
                return speakResponse("Tu familia aún no tiene un ADN generado. "
                        + "Completa una evaluación en Integrity Family para construirlo.");
            }

            String valores     = formatLista(dna.valores(), 3, "sin valores registrados");
            String fortalezas  = formatLista(dna.fortalezas(), 2, "sin fortalezas registradas");
            String sombras     = formatLista(dna.sombras(), 2, "sin riesgos identificados");
            String estilo      = dna.estiloComunicacion() != null ? dna.estiloComunicacion() : "no definido";

            String texto = "ADN de la familia " + family.getName() + ". "
                    + "Valores: " + valores + ". "
                    + "Fortalezas: " + fortalezas + ". "
                    + "Sombras a trabajar: " + sombras + ". "
                    + "Estilo de comunicación: " + estilo + ".";

            return speakResponse(texto);
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] FamilyDna error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener el ADN familiar en este momento.");
        }
    }

    /** LegacyMessageIntent — valores heredables de la familia. */
    private Map<String, Object> handleLegacyMessage(Family family) {
        try {
            var values = legacyService.getValues(family.getId());
            if (values == null || values.isEmpty()) {
                return speakResponse("La familia " + family.getName() + " aún no ha registrado valores de legado. "
                        + "Entra a Integrity Family para comenzar a construir tu legado familiar.");
            }
            var primero = values.get(0);
            String texto = "Legado de la familia " + family.getName() + ". "
                    + "Valor registrado: " + primero.getName() + ". "
                    + (primero.getDescription() != null ? primero.getDescription() : "")
                    + " Tu familia tiene " + values.size() + " valor" + (values.size() != 1 ? "es" : "")
                    + " de legado registrado" + (values.size() != 1 ? "s" : "") + ".";
            return speakResponse(texto);
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] LegacyMessage error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude acceder al legado familiar en este momento.");
        }
    }

    /** TimelineIntent — narración de hitos recientes de la línea del tiempo. */
    private Map<String, Object> handleTimeline(Family family) {
        try {
            var eventos = familyTimelineService.getTimeline(family.getId());
            if (eventos == null || eventos.isEmpty()) {
                return speakResponse("La familia " + family.getName() + " aún no tiene eventos en su línea del tiempo.");
            }

            int total = eventos.size();
            var recientes = eventos.stream()
                    .skip(Math.max(0, total - 3))
                    .collect(Collectors.toList());

            StringBuilder texto = new StringBuilder(
                    "Línea del tiempo de la familia " + family.getName() + ". "
                    + "Han registrado " + total + " evento" + (total != 1 ? "s" : "") + " en su historia. "
                    + "Últimos momentos: ");

            for (int i = 0; i < recientes.size(); i++) {
                var ev = recientes.get(i);
                texto.append(ev.title());
                if (ev.actor() != null && !ev.actor().isBlank()) texto.append(", por ").append(ev.actor());
                if (i < recientes.size() - 1) texto.append(". ");
            }
            texto.append(". Cada evento es una huella de su evolución.");

            return speakResponse(texto.toString());
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] Timeline error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener la línea del tiempo familiar en este momento.");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utilidades internas
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, Object> helpResponse(Family family) {
        return speakResponseKeepSession(
                "Hola, soy Integrity Family. Puedes pedirme: "
                + "«resumen del guardián», «estado Sentinel», «consejo familiar», "
                + "«misión actual», «contexto familiar», «ADN familiar», «legado», o «línea del tiempo».",
                "¿Qué deseas consultar?");
    }

    private boolean isValidSkillId(Map<String, Object> body) {
        String configured = alexaProperties.getSkillId();
        if (configured == null || configured.isBlank()) return true;
        String incoming = extractSkillId(body);
        return configured.equals(incoming);
    }

    private static int parseNivelSentinel(String slot) {
        if (slot == null) return 3;
        return switch (slot.toLowerCase().trim()) {
            case "1", "uno"        -> 1;
            case "2", "dos"        -> 2;
            case "3", "tres"       -> 3;
            case "4", "cuatro"     -> 4;
            case "5", "cinco"      -> 5;
            default                -> 3;
        };
    }

    private static String formatLista(List<String> lista, int max, String fallback) {
        if (lista == null || lista.isEmpty()) return fallback;
        return lista.stream().limit(max).collect(Collectors.joining(", "));
    }

    // ── Extracciones del JSON de Alexa ─────────────────────────────────────

    @SuppressWarnings("unchecked")
    private static String extractAccessToken(Map<String, Object> body) {
        try {
            var ctx  = (Map<String, Object>) body.get("context");
            var sys  = (Map<String, Object>) ctx.get("System");
            var user = (Map<String, Object>) sys.get("user");
            return (String) user.get("accessToken");
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static String extractSkillId(Map<String, Object> body) {
        try {
            var ctx = (Map<String, Object>) body.get("context");
            var sys = (Map<String, Object>) ctx.get("System");
            var app = (Map<String, Object>) sys.get("application");
            return (String) app.get("applicationId");
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static String extractRequestType(Map<String, Object> body) {
        try { return (String) ((Map<String, Object>) body.get("request")).get("type"); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static String extractIntentName(Map<String, Object> body) {
        try {
            var req    = (Map<String, Object>) body.get("request");
            var intent = (Map<String, Object>) req.get("intent");
            return (String) intent.get("name");
        } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private static String extractSlot(Map<String, Object> body, String slotName) {
        try {
            var req    = (Map<String, Object>) body.get("request");
            var intent = (Map<String, Object>) req.get("intent");
            var slots  = (Map<String, Object>) intent.get("slots");
            var slot   = (Map<String, Object>) slots.get(slotName);
            return (String) slot.get("value");
        } catch (Exception e) { return null; }
    }

    // ── Builders de respuesta Alexa ────────────────────────────────────────

    private static Map<String, Object> speakResponse(String text) {
        return Map.of(
                "version", "1.0",
                "response", Map.of(
                        "outputSpeech", Map.of("type", "PlainText", "text", text),
                        "shouldEndSession", true));
    }

    private static Map<String, Object> speakResponseKeepSession(String text, String reprompt) {
        return Map.of(
                "version", "1.0",
                "response", Map.of(
                        "outputSpeech", Map.of("type", "PlainText", "text", text),
                        "reprompt", Map.of("outputSpeech", Map.of("type", "PlainText", "text", reprompt)),
                        "shouldEndSession", false));
    }

    private static Map<String, Object> linkAccountResponse() {
        return Map.of(
                "version", "1.0",
                "response", Map.of(
                        "outputSpeech", Map.of(
                                "type", "PlainText",
                                "text", "Para usar esta skill, vincula tu cuenta de Integrity Family desde la app de Alexa."),
                        "card", Map.of("type", "LinkAccount"),
                        "shouldEndSession", true));
    }
}
