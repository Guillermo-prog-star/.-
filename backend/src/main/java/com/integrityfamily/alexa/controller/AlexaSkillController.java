package com.integrityfamily.alexa.controller;

import com.integrityfamily.alexa.config.AlexaProperties;
import com.integrityfamily.alexa.domain.AlexaOAuthToken;
import com.integrityfamily.alexa.service.AlexaOAuthService;
import com.integrityfamily.bitacora.service.SprintService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Recibe requests del Alexa Skill Kit.
 * Alexa envía el access_token en el cuerpo JSON bajo context.System.user.accessToken.
 * Se valida contra alexa_oauth_tokens para identificar la familia.
 */
@RestController
@RequestMapping("/alexa")
@Slf4j
@RequiredArgsConstructor
public class AlexaSkillController {

    private final AlexaProperties alexaProperties;
    private final AlexaOAuthService alexaOAuthService;
    private final FamilyRepository familyRepository;
    private final SprintService sprintService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> handleSkillRequest(
            @RequestHeader(value = "SignatureCertChainUrl", required = false) String certUrl,
            @RequestBody Map<String, Object> body) {

        // Validar skillId
        String skillId = extractSkillId(body);
        if (alexaProperties.getSkillId() != null
                && !alexaProperties.getSkillId().isBlank()
                && !alexaProperties.getSkillId().equals(skillId)) {
            log.warn("[ALEXA-SKILL] skillId inválido: {}", skillId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Resolver token → familia
        String accessToken = extractAccessToken(body);
        if (accessToken == null) {
            return ResponseEntity.ok(linkAccountResponse());
        }

        AlexaOAuthToken token = alexaOAuthService.resolveToken(accessToken).orElse(null);
        if (token == null) {
            return ResponseEntity.ok(linkAccountResponse());
        }

        Family family = familyRepository.findById(token.getFamilyId()).orElse(null);
        if (family == null) {
            return ResponseEntity.ok(speakResponse("No encontré tu familia vinculada. Intenta vincular tu cuenta nuevamente."));
        }

        // Dispatch por tipo de request / intent
        String requestType = extractRequestType(body);
        if ("LaunchRequest".equals(requestType)) {
            return ResponseEntity.ok(speakResponse(
                    "Hola familia " + family.getName() + ". ¿Qué quieres saber hoy? Puedes preguntarme por la misión actual o el estado familiar."));
        }

        if ("IntentRequest".equals(requestType)) {
            String intentName = extractIntentName(body);
            return switch (intentName != null ? intentName : "") {
                case "CurrentMissionIntent" -> ResponseEntity.ok(handleCurrentMission(family));
                case "FamilyStatusIntent"   -> ResponseEntity.ok(handleFamilyStatus(family));
                case "AMAZON.HelpIntent"    -> ResponseEntity.ok(speakResponse(
                        "Puedes preguntarme: ¿cuál es la misión actual? o ¿cómo está mi familia?"));
                case "AMAZON.StopIntent", "AMAZON.CancelIntent" -> ResponseEntity.ok(speakResponse(
                        "¡Hasta pronto, familia " + family.getName() + "!"));
                default -> ResponseEntity.ok(speakResponse(
                        "No entendí eso. Intenta preguntarme por la misión o el estado familiar."));
            };
        }

        return ResponseEntity.ok(speakResponse("Hasta luego."));
    }

    private Map<String, Object> handleCurrentMission(Family family) {
        try {
            var sprint = sprintService.getActiveSprint(family.getId());
            if (sprint == null) {
                return speakResponse("Tu familia no tiene un sprint activo en este momento.");
            }
            String objective = sprint.objective() != null ? sprint.objective() : "sin objetivo definido";
            return speakResponse("El sprint actual de tu familia es: " + objective + ". ¡Sigan adelante!");
        } catch (Exception e) {
            log.warn("[ALEXA-SKILL] Error obteniendo sprint para familia {}: {}", family.getId(), e.getMessage());
            return speakResponse("No pude obtener la misión actual. Intenta de nuevo más tarde.");
        }
    }

    private Map<String, Object> handleFamilyStatus(Family family) {
        boolean sentinel = Boolean.TRUE.equals(family.getSentinelActive());
        String status = sentinel
                ? "está en modo Sentinel, que indica una situación que requiere atención especial."
                : "está en modo normal. ¡Todo va bien!";
        return speakResponse("La familia " + family.getName() + " " + status);
    }

    // ── helpers de extracción de campos del JSON de Alexa ──────────────────

    @SuppressWarnings("unchecked")
    private static String extractAccessToken(Map<String, Object> body) {
        try {
            var context = (Map<String, Object>) body.get("context");
            var system = (Map<String, Object>) context.get("System");
            var user = (Map<String, Object>) system.get("user");
            return (String) user.get("accessToken");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractSkillId(Map<String, Object> body) {
        try {
            var context = (Map<String, Object>) body.get("context");
            var system = (Map<String, Object>) context.get("System");
            var application = (Map<String, Object>) system.get("application");
            return (String) application.get("applicationId");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractRequestType(Map<String, Object> body) {
        try {
            return (String) ((Map<String, Object>) body.get("request")).get("type");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static String extractIntentName(Map<String, Object> body) {
        try {
            var request = (Map<String, Object>) body.get("request");
            var intent = (Map<String, Object>) request.get("intent");
            return (String) intent.get("name");
        } catch (Exception e) {
            return null;
        }
    }

    // ── builders de respuesta Alexa ─────────────────────────────────────────

    private static Map<String, Object> speakResponse(String text) {
        return Map.of(
                "version", "1.0",
                "response", Map.of(
                        "outputSpeech", Map.of("type", "PlainText", "text", text),
                        "shouldEndSession", true));
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
