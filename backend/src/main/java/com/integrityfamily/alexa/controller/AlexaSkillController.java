package com.integrityfamily.alexa.controller;

import com.integrityfamily.ai.dto.CopilotDtos.CopilotInferRequest;
import com.integrityfamily.ai.dto.CopilotDtos.StructuredAiInferenceResponse;
import com.integrityfamily.ai.service.CopilotService;
import com.integrityfamily.alexa.config.AlexaProperties;
import com.integrityfamily.alexa.domain.AlexaOAuthToken;
import com.integrityfamily.alexa.service.AlexaOAuthService;
import com.integrityfamily.alexa.service.ChapterService;
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
    private final ChapterService          chapterService;

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
                // ── Consejo Familiar (Copilot IA — tensión narrativa) ─────
                case "CopilotIntent"           -> handleCopilot(family);
                // ── Sprint y misión ───────────────────────────────────────
                case "CurrentMissionIntent"    -> handleCurrentMission(family);
                case "FamilyContextIntent"     -> handleFamilyContext(family);
                // ── ADN Familiar ──────────────────────────────────────────
                case "FamilyDnaIntent"         -> handleFamilyDna(family);
                // ── Legado y narrativa temporal ───────────────────────────
                case "LegacyMessageIntent"     -> handleLegacyMessage(family);
                case "TimelineIntent"          -> handleTimeline(family);
                // ── Historia y Escena (narrativa de transformación) ───────
                case "FamilyHistoryIntent"     -> handleFamilyHistory(family);
                case "FamilySceneIntent"       -> handleFamilyScene(family);
                // ── Consejo Diario — tarjeta insignia ─────────────────────
                case "ConsejoDiarioIntent"     -> handleConsejoDiario(family);
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

    /** LaunchRequest: bienvenida contextual con Temporada y pantalla APL en Echo Show. */
    private Map<String, Object> handleLaunch(Family family) {
        boolean sentinel = Boolean.TRUE.equals(family.getSentinelActive());

        // Determinar temporada desde línea del tiempo
        String temporada = "Temporada 1 · Aprender a Ver";
        try {
            var eventos = familyTimelineService.getTimeline(family.getId());
            if (eventos != null) temporada = getTemporada(eventos.size());
        } catch (Exception ignored) {}

        String texto = sentinel
                ? "Hola, familia " + family.getName() + ". Hay una situación que requiere atención. " +
                  "Di «estado Sentinel» para conocer los detalles, o «tensión familiar» para orientación."
                : "Hola, familia " + family.getName() + ". Están en su " + temporada + ". " +
                  "Puedes decirme: escena del día, historia familiar, misión actual, tensión familiar o estado Sentinel.";

        String icfTexto = family.getMilestoneIcfAvg() != null
                ? String.format("%.0f%%", family.getMilestoneIcfAvg()) : "—";
        String riesgo   = sentinel ? "🔴 ALERTA" : "🟢 Estable";
        int participacion = family.getParticipationScore() != null ? family.getParticipationScore() : 0;

        return aplResponseKeepSession(texto, "Integrity Family escuchando...",
                aplDashboard(family.getName(), icfTexto, riesgo, participacion, sentinel, temporada));
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

            return aplResponse(texto, aplInfoCard("🛡️ Resumen del Guardián", family.getName(),
                    List.of(
                        participacion + inactivos,
                        "Avance: " + String.format("%.0f%%", b.planCompletionRate() * 100),
                        b.currentMilestone() != null ? "Hito: " + b.currentMilestone() : "",
                        b.fatigueSignal() != null && !b.fatigueSignal().isBlank() ? "⚠️ " + b.fatigueSignal() : ""
                    ), "#2563eb"));
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
            boolean alerta = enCrisis || sentinel;
            String accentColor = alerta ? "#ef4444" : "#22c55e";
            return aplResponse(estado, aplInfoCard("🔭 Estado Sentinel", family.getName(),
                    List.of(
                        alerta ? "🔴 ALERTA ACTIVA" : "🟢 Sin situaciones críticas",
                        "Nivel de riesgo: " + nivelTexto,
                        alerta ? "Revisa el panel de Integrity Family" : "Sentinel monitorea continuamente"
                    ), accentColor));
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

            String textoAlerta = "He registrado la situación crítica de nivel " + nivel
                    + " para la familia " + family.getName() + ". "
                    + "El equipo de apoyo de Integrity Family ha sido notificado. "
                    + "Recuerda: en peligro inmediato llama a los servicios de emergencia locales.";
            return aplResponse(textoAlerta, aplInfoCard("🚨 Sentinel Activado", family.getName(),
                    List.of(
                        "Nivel registrado: " + nivel + " / 5",
                        "⚠️ Equipo de apoyo notificado",
                        "🆘 Emergencia: llama al 123"
                    ), "#ef4444"));
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] ActivateSentinel error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude activar Sentinel. Por favor usa la aplicación directamente.");
        }
    }

    /**
     * CopilotIntent — Consejo Familiar Autónomo.
     * Usa CopilotService que internamente sintetiza ADN + contexto + historial + IA.
     */
    /**
     * CopilotIntent — interpreta tensión narrativa, no prescribe acciones.
     * La IA identifica QUÉ está en tensión; la familia decide qué hacer.
     */
    private Map<String, Object> handleCopilot(Family family) {
        try {
            StructuredAiInferenceResponse resp = copilotService.generateInference(
                    new CopilotInferRequest(family.getId(), "ALEXA_CONSULTATION"));

            if (resp == null) {
                return speakResponse("El Consejo Familiar no tiene una lectura disponible en este momento. "
                        + "Vuelve a intentarlo en unos minutos.");
            }

            // Texto de voz — enmarcado como interpretación, no prescripción
            StringBuilder texto = new StringBuilder("La familia " + family.getName() + " atraviesa un momento que merece atención. ");
            if (resp.summary() != null) texto.append(resp.summary()).append(". ");
            if (resp.containmentSuggestion() != null && !resp.containmentSuggestion().isBlank()) {
                texto.append("Hay un capítulo disponible que puede ayudar: ").append(resp.containmentSuggestion()).append(". ");
            }
            texto.append("La familia decide cuándo y cómo avanzar.");

            // APL — tensión narrativa, no lista de acciones
            List<String> lineas = new java.util.ArrayList<>();
            if (resp.summary() != null)
                lineas.add("⚠️ " + resp.summary());
            if (resp.recommendedActions() != null && !resp.recommendedActions().isEmpty()) {
                lineas.add("Manifestaciones:");
                resp.recommendedActions().stream().limit(3)
                        .forEach(a -> lineas.add("  · " + a));
            }
            if (resp.containmentSuggestion() != null && !resp.containmentSuggestion().isBlank())
                lineas.add("📖 Capítulo recomendado: " + resp.containmentSuggestion());

            return aplResponse(texto.toString().trim(),
                    aplInfoCard("🧠 Tensión Narrativa Familiar", family.getName(), lineas, "#7c3aed"));
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] Copilot error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("El Consejo Familiar no está disponible ahora. Intenta de nuevo pronto.");
        }
    }

    /** FamilyHistoryIntent — Historia Familiar Viva: evolución narrativa, no métricas. */
    private Map<String, Object> handleFamilyHistory(Family family) {
        try {
            var eventos    = familyTimelineService.getTimeline(family.getId());
            var dna        = familyDnaService.findByFamilyId(family.getId()).orElse(null);
            var legValues  = legacyService.getValues(family.getId());

            int totalEventos = eventos != null ? eventos.size() : 0;

            // Narrativa de apertura
            String apertura = totalEventos > 0
                    ? "Esta familia lleva " + totalEventos + " momentos registrados en su historia."
                    : "Esta familia está escribiendo sus primeros capítulos.";

            // Lo que aprendieron — valores del ADN
            String aprendizajes = "";
            if (dna != null && dna.fortalezas() != null && !dna.fortalezas().isEmpty()) {
                aprendizajes = "Sus fortalezas: " + formatLista(dna.fortalezas(), 3, "") + ". ";
            }

            // Lo que viene — sombras a trabajar
            String proximoPaso = "";
            if (dna != null && dna.sombras() != null && !dna.sombras().isEmpty()) {
                proximoPaso = "Su próximo reto: " + dna.sombras().get(0) + ".";
            }

            String legadoTxt = (legValues != null && !legValues.isEmpty())
                    ? " Han registrado " + legValues.size() + " valor" + (legValues.size() != 1 ? "es" : "") + " de legado."
                    : "";

            String texto = "Historia Familiar Viva de la familia " + family.getName() + ". "
                    + apertura + " " + aprendizajes + legadoTxt + " " + proximoPaso;

            // Determinar temporada actual
            var sprint = sprintService.getActiveSprint(family.getId());
            String temporadaTxt = sprint != null ? getTemporada(totalEventos) : "Temporada en construcción";

            List<String> lineas = new java.util.ArrayList<>();
            lineas.add("🗓 " + temporadaTxt);
            lineas.add("📅 " + totalEventos + " momentos en su historia");
            if (dna != null && dna.valores() != null && !dna.valores().isEmpty())
                lineas.add("✨ Valores: " + formatLista(dna.valores(), 2, ""));
            if (legValues != null && !legValues.isEmpty())
                lineas.add("🏛️ " + legValues.size() + " valor" + (legValues.size() != 1 ? "es" : "") + " de legado");
            if (dna != null && dna.sombras() != null && !dna.sombras().isEmpty())
                lineas.add("❤️ Próximo reto: " + dna.sombras().get(0));

            return aplResponse(texto.trim(),
                    aplInfoCard("📖 Historia Familiar Viva", family.getName(), lineas, "#10b981"));
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] FamilyHistory error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener la historia familiar en este momento.");
        }
    }

    /** FamilySceneIntent — Escena Familiar del Día: el momento más reciente que merece ser recordado. */
    private Map<String, Object> handleFamilyScene(Family family) {
        try {
            var eventos = familyTimelineService.getTimeline(family.getId());

            if (eventos == null || eventos.isEmpty()) {
                return speakResponse("La familia " + family.getName() + " aún no tiene escenas registradas. "
                        + "Cada misión completada, cada evidencia y cada conversación profunda se convertirá en una escena.");
            }

            // Última escena registrada
            var escena = eventos.get(eventos.size() - 1);
            String titulo  = escena.title() != null ? escena.title() : "Un momento familiar";
            String actor   = escena.actor() != null && !escena.actor().isBlank() ? escena.actor() : null;

            String textoVoz = "Hoy ocurrió algo que merece ser recordado en la familia " + family.getName() + ". "
                    + titulo + (actor != null ? ", protagonizado por " + actor : "") + ". "
                    + "Cada escena es un capítulo de la historia que están construyendo.";

            List<String> lineas = new java.util.ArrayList<>();
            lineas.add("🎬 " + titulo);
            if (actor != null) lineas.add("👤 " + actor);
            lineas.add("❤️ Confianza +1");
            lineas.add("📼 Guardado en su historia familiar");

            return aplResponse(textoVoz,
                    aplInfoCard("🎬 Escena Familiar del Día", family.getName(), lineas, "#f59e0b"));
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] FamilyScene error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener la escena del día en este momento.");
        }
    }

    /**
     * ConsejoDiarioIntent — la tarjeta insignia de Integrity Family.
     * Responde: ¿Qué debe hacer esta familia HOY para escribir un mejor capítulo de su historia?
     * Backed by the 75-chapter narrative system from chapters.json.
     */
    private Map<String, Object> handleConsejoDiario(Family family) {
        try {
            var chapter = chapterService.getCurrentChapter(family.getId());
            int completados = chapterService.getCompletedCount(family.getId());
            int totalChapters = chapterService.getChapters().size();

            if (chapter == null) {
                return speakResponse("La familia " + family.getName() + " está comenzando su viaje. "
                        + "Ingresa a Integrity Family para iniciar tu primer capítulo.");
            }

            int num         = ((Number) chapter.get("number")).intValue();
            String titulo   = (String) chapter.get("title");
            String escena   = (String) chapter.get("scene");
            String tension  = (String) chapter.get("tension");
            String accion   = (String) chapter.get("action");
            String temporada = chapterService.getSeasonName(num);
            int pillar      = ((Number) chapter.get("pillar")).intValue();
            String pilarNombre = switch (pillar) {
                case 1 -> "Pilar I · Reconocimiento";
                case 2 -> "Pilar II · Amor";
                case 3 -> "Pilar III · Entrega y Legado";
                default -> "Pilar " + pillar;
            };

            // Voz narrativa — no prescriptiva
            String texto = "Hoy, la familia " + family.getName() + " está en el capítulo " + num + ": "
                    + titulo + ". "
                    + escena + ". "
                    + "La pregunta que guía el día de hoy es: " + tension + " "
                    + "Una posible acción: " + accion + ". "
                    + "La familia decide cómo y cuándo actuar.";

            // APL — tarjeta insignia
            List<Map<String, Object>> items = new java.util.ArrayList<>();

            // Cabecera: pilar y temporada
            items.add(Map.of("type", "Text",
                    "text", pilarNombre + "  ·  " + temporada,
                    "fontSize", "16dp", "color", "#64748b",
                    "textAlign", "left", "spacing", "0dp"));

            // Título del capítulo
            items.add(Map.of("type", "Text",
                    "text", "Cap. " + num + " · " + titulo,
                    "fontSize", "30dp", "color", "#f8fafc",
                    "fontWeight", "700", "textAlign", "left", "spacing", "10dp"));

            // Línea separadora
            items.add(Map.of("type", "Frame", "width", "100%", "height", "2dp",
                    "backgroundColor", "#7c3aed", "spacing", "14dp"));

            // Escena (contextualiza el momento)
            items.add(Map.of("type", "Text",
                    "text", "📖 " + escena,
                    "fontSize", "20dp", "color", "#cbd5e1",
                    "textAlign", "left", "spacing", "10dp"));

            // Tensión (la pregunta del día)
            items.add(Map.of("type", "Text",
                    "text", "❓ " + tension,
                    "fontSize", "20dp", "color", "#a78bfa",
                    "fontStyle", "italic", "textAlign", "left", "spacing", "12dp"));

            // Acción sugerida
            items.add(Map.of("type", "Text",
                    "text", "✦ Acción sugerida: " + accion,
                    "fontSize", "19dp", "color", "#e2e8f0",
                    "textAlign", "left", "spacing", "12dp"));

            // Progreso
            items.add(Map.of("type", "Text",
                    "text", completados + " / " + totalChapters + " capítulos completados",
                    "fontSize", "15dp", "color", "#475569",
                    "textAlign", "left", "spacing", "16dp"));

            Map<String, Object> document = Map.of(
                "type", "APL", "version", "2024.1",
                "mainTemplate", Map.of(
                    "parameters", List.of("p"),
                    "items", List.of(Map.of(
                        "type", "Container",
                        "width", "100vw", "height", "100vh",
                        "backgroundColor", "#0a0f1e",
                        "direction", "column",
                        "justifyContent", "center",
                        "paddingLeft", "72dp", "paddingRight", "72dp",
                        "items", items
                    ))
                )
            );

            Map<String, Object> aplDirective = Map.of(
                "type", "Alexa.Presentation.APL.RenderDocument",
                "token", "consejoDiario",
                "document", document,
                "datasources", Map.of("p", Map.of("family", family.getName()))
            );

            return Map.of(
                "version", "1.0",
                "response", Map.of(
                    "outputSpeech", Map.of("type", "PlainText", "text", texto),
                    "directives", List.of(aplDirective),
                    "shouldEndSession", true));

        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] ConsejoDiario error familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener el consejo del día en este momento. Intenta de nuevo pronto.");
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
            String textoMision = misionTexto + " ¡Adelante, familia " + family.getName() + "!";
            long total    = sprint.missions() != null ? sprint.missions().size() : 0;
            long completadas = sprint.missions() != null ? sprint.missions().stream()
                    .filter(m -> "COMPLETED".equalsIgnoreCase(m.status())).count() : 0;
            long pendientes = total - completadas;

            return aplResponse(textoMision, aplInfoCard("🎯 Misión Actual", family.getName(),
                    List.of(
                        sprint.objective() != null ? sprint.objective() : "Objetivo no definido",
                        "✅ Completadas: " + completadas + " / " + total,
                        "⏳ Pendientes: " + pendientes
                    ), "#f59e0b"));
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

            List<String> lineasCtx = new java.util.ArrayList<>(List.of(
                "Riesgo: " + nivel,
                "Dimensión crítica: " + dimension,
                "Tendencia: " + tendencia,
                String.format("Adherencia: %.0f%%", adherencia * 100)
            ));
            if (inactividad > 0) lineasCtx.add("⚠️ " + inactividad + " días sin actividad");
            if (ctx.alerts() != null) ctx.alerts().stream().limit(2)
                    .forEach(a -> lineasCtx.add("🔔 " + a));

            return aplResponse(texto.trim(),
                    aplInfoCard("📊 Contexto Familiar", family.getName(), lineasCtx, "#0891b2"));
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

            return aplResponse(texto, aplInfoCard("🧬 ADN Familiar", family.getName(),
                    List.of(
                        "💚 Valores: " + valores,
                        "💪 Fortalezas: " + fortalezas,
                        "🌑 Sombras: " + sombras,
                        "🗣️ Comunicación: " + estilo
                    ), "#059669"));
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
            List<String> lineasLegado = new java.util.ArrayList<>();
            lineasLegado.add("📖 " + primero.getName());
            if (primero.getDescription() != null && !primero.getDescription().isBlank())
                lineasLegado.add(primero.getDescription().length() > 80
                        ? primero.getDescription().substring(0, 80) + "…"
                        : primero.getDescription());
            lineasLegado.add("Total de valores: " + values.size());

            return aplResponse(texto, aplInfoCard("🏛️ Legado Familiar", family.getName(),
                    lineasLegado, "#d97706"));
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

            List<String> lineasTl = new java.util.ArrayList<>();
            lineasTl.add("📅 " + total + " evento" + (total != 1 ? "s" : "") + " en su historia");
            recientes.forEach(ev -> {
                String linea = "▸ " + ev.title();
                if (ev.actor() != null && !ev.actor().isBlank()) linea += " (" + ev.actor() + ")";
                lineasTl.add(linea);
            });

            return aplResponse(texto.toString(),
                    aplInfoCard("📜 Línea del Tiempo", family.getName(), lineasTl, "#6366f1"));
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
                + "«consejo del día», «escena del día», «historia familiar», "
                + "«tensión familiar», «misión actual», «ADN familiar», «legado», «línea del tiempo», "
                + "«resumen del guardián» o «estado Sentinel».",
                "¿Qué deseas consultar?");
    }

    private boolean isValidSkillId(Map<String, Object> body) {
        String configured = alexaProperties.getSkillId();
        if (configured == null || configured.isBlank()) return true;
        String incoming = extractSkillId(body);
        return configured.equals(incoming);
    }

    /** Mapea número de eventos/capítulos a nombre de Temporada. */
    private static String getTemporada(int capitulos) {
        if (capitulos <= 5)  return "Temporada 1 · Aprender a Ver";
        if (capitulos <= 10) return "Temporada 2 · Aprender a Comprender";
        if (capitulos <= 15) return "Temporada 3 · Aprender a Valorar";
        if (capitulos <= 20) return "Temporada 4 · Comprender Nuestra Historia";
        if (capitulos <= 25) return "Temporada 5 · Descubrir Quiénes Somos";
        if (capitulos <= 30) return "Temporada 6 · Escribir Nuestra Historia";
        if (capitulos <= 35) return "Temporada 7 · Sostener lo Construido";
        if (capitulos <= 40) return "Temporada 8 · Transmitir el Legado";
        return "Temporada 9 · Familia Transformada";
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

    // ── APL — pantalla visual para Echo Show ──────────────────────────────

    private static Map<String, Object> aplResponseKeepSession(
            String text, String reprompt, Map<String, Object> aplDirective) {
        return Map.of(
                "version", "1.0",
                "response", Map.of(
                        "outputSpeech",    Map.of("type", "PlainText", "text", text),
                        "reprompt",        Map.of("outputSpeech", Map.of("type", "PlainText", "text", reprompt)),
                        "directives",      List.of(aplDirective),
                        "shouldEndSession", false));
    }

    private static Map<String, Object> aplResponse(String text, Map<String, Object> aplDirective) {
        return Map.of(
                "version", "1.0",
                "response", Map.of(
                        "outputSpeech", Map.of("type", "PlainText", "text", text),
                        "directives",   List.of(aplDirective),
                        "shouldEndSession", true));
    }

    /** APL RenderDocument para el dashboard de bienvenida con Temporada. */
    private static Map<String, Object> aplDashboard(
            String familyName, String icf, String riesgo, int participacion, boolean alerta, String temporada) {

        String bg     = alerta ? "#4a0000" : "#0d1b2a";
        String accent = alerta ? "#ff4444" : "#00d4aa";

        Map<String, Object> document = Map.of(
            "type", "APL",
            "version", "2024.1",
            "mainTemplate", Map.of(
                "parameters", List.of("p"),
                "items", List.of(Map.of(
                    "type", "Container",
                    "width", "100vw", "height", "100vh",
                    "backgroundColor", "${p.bg}",
                    "direction", "column",
                    "alignItems", "center",
                    "justifyContent", "center",
                    "paddingLeft", "60dp", "paddingRight", "60dp",
                    "items", List.of(
                        Map.of("type", "Text",
                               "text", "✨ INTEGRITY FAMILY",
                               "fontSize", "20dp", "color", "${p.accent}",
                               "textAlign", "center", "spacing", "0dp"),
                        Map.of("type", "Text",
                               "text", "Familia ${p.familyName}",
                               "fontSize", "40dp", "color", "#ffffff",
                               "fontWeight", "700", "textAlign", "center",
                               "spacing", "10dp"),
                        Map.of("type", "Text",
                               "text", "${p.temporada}",
                               "fontSize", "18dp", "color", "${p.accent}",
                               "textAlign", "center", "spacing", "6dp"),
                        Map.of("type", "Container",
                               "direction", "row",
                               "justifyContent", "center",
                               "spacing", "32dp",
                               "items", List.of(
                                   aplStat("ICF", "${p.icf}", "${p.accent}"),
                                   aplStat("Estado", "${p.riesgo}", "#ffffff"),
                                   aplStat("Participación", "${p.part}", "#f0c040")
                               )),
                        Map.of("type", "Text",
                               "text", "Di: «escena del día» · «historia familiar» · «misión actual» · «tensión familiar»",
                               "fontSize", "16dp", "color", "#aaaaaa",
                               "textAlign", "center", "spacing", "20dp")
                    )
                ))
            )
        );

        Map<String, Object> datasources = Map.of(
            "p", Map.of(
                "familyName",  familyName,
                "icf",         icf,
                "riesgo",      riesgo,
                "part",        participacion + " pts",
                "bg",          bg,
                "accent",      accent,
                "temporada",   temporada
            )
        );

        return Map.of(
            "type",        "Alexa.Presentation.APL.RenderDocument",
            "token",       "ifDashboard",
            "document",    document,
            "datasources", datasources
        );
    }

    /**
     * Tarjeta informativa genérica APL para intents secundarios.
     * Muestra título del intent, nombre de la familia y hasta 5 líneas de datos.
     */
    private static Map<String, Object> aplInfoCard(
            String titulo, String familyName, List<String> lineas, String accentColor) {

        List<Map<String, Object>> items = new java.util.ArrayList<>();
        items.add(Map.of("type", "Text", "text", titulo,
                "fontSize", "26dp", "color", accentColor, "fontWeight", "700",
                "textAlign", "left", "spacing", "0dp"));
        items.add(Map.of("type", "Text", "text", "Familia " + familyName,
                "fontSize", "20dp", "color", "#94a3b8", "textAlign", "left", "spacing", "6dp"));

        // Separador
        items.add(Map.of("type", "Frame", "width", "100%", "height", "2dp",
                "backgroundColor", accentColor, "spacing", "14dp"));

        // Líneas de datos (máx 5, ignorar vacías)
        lineas.stream()
              .filter(l -> l != null && !l.isBlank())
              .limit(5)
              .forEach(l -> items.add(Map.of(
                  "type", "Text", "text", l,
                  "fontSize", "22dp", "color", "#e2e8f0",
                  "textAlign", "left", "spacing", "10dp")));

        Map<String, Object> document = Map.of(
            "type", "APL", "version", "2024.1",
            "mainTemplate", Map.of(
                "parameters", List.of("p"),
                "items", List.of(Map.of(
                    "type", "Container",
                    "width", "100vw", "height", "100vh",
                    "backgroundColor", "#0f172a",
                    "direction", "column",
                    "justifyContent", "center",
                    "paddingLeft", "72dp", "paddingRight", "72dp",
                    "items", items
                ))
            )
        );

        return Map.of(
            "type", "Alexa.Presentation.APL.RenderDocument",
            "token", "ifCard",
            "document", document,
            "datasources", Map.of("p", Map.of("family", familyName))
        );
    }

    private static Map<String, Object> aplStat(String label, String value, String color) {
        return Map.of(
            "type", "Container",
            "alignItems", "center",
            "paddingLeft", "24dp", "paddingRight", "24dp",
            "items", List.of(
                Map.of("type", "Text", "text", value,
                       "fontSize", "36dp", "color", color, "fontWeight", "700"),
                Map.of("type", "Text", "text", label,
                       "fontSize", "16dp", "color", "#aaaaaa")
            )
        );
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
