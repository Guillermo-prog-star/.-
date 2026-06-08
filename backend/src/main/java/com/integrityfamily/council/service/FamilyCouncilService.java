package com.integrityfamily.council.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.context.service.FamilyContextEngine;
import com.integrityfamily.council.dto.CouncilRequest;
import com.integrityfamily.council.dto.CouncilResponse;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.dna.service.FamilyDnaService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.tree.service.FamilyTreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Consejo Familiar IA — el corazón diferenciador de Integrity Family.
 *
 * En lugar de responder desde conocimiento genérico de internet,
 * el Consejo construye su respuesta EXCLUSIVAMENTE desde:
 *   1. La Constitución Familiar (principio fundador, compromisos, límites)
 *   2. El ADN Familiar (valores, fortalezas, sombras, patrones)
 *   3. La Historia Familiar (lecciones aprendidas, reconocimiento)
 *   4. El Estado Actual (Motor de Contexto)
 *   5. La Herencia Generacional (si existe)
 *
 * Resultado: la familia siente que habla CON SUS PROPIOS VALORES,
 * no con una máquina.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyCouncilService {

    private final FamilyRepository      familyRepository;
    private final FamilyLegacyRepository legacyRepository;
    private final FamilyDnaService      dnaService;
    private final FamilyContextEngine   contextEngine;
    private final FamilyTreeService     treeService;
    private final AiProvider            aiProvider;

    @Transactional(readOnly = true)
    public CouncilResponse consult(Long familyId, CouncilRequest req) {
        log.info("[COUNCIL] Consulta para familia {} — tema: {}", familyId, req.topic());

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        FamilyLegacy legacy  = legacyRepository.findByFamilyId(familyId).orElse(null);
        String dnaBlock      = dnaService.buildDnaContextBlock(familyId);
        String contextBlock  = contextEngine.buildContextBlock(familyId);
        String heritageBlock = treeService.buildTreeContextBlock(familyId);

        // Verifica si tiene suficiente identidad para activar el Consejo pleno
        boolean hasIdentity  = legacy != null && legacy.getFoundingPrinciple() != null;
        boolean hasDna       = dnaBlock != null;

        String prompt = buildCouncilPrompt(family, legacy, dnaBlock, contextBlock, heritageBlock, req);

        log.info("[COUNCIL] Invocando IA — identidad={} adn={}", hasIdentity, hasDna);
        String rawResponse = aiProvider.generateRawResponse(prompt);

        return new CouncilResponse(
                familyId,
                family.getName(),
                req.question(),
                req.topic(),
                rawResponse.strip(),
                hasIdentity,
                hasDna,
                buildSourcesUsed(legacy, dnaBlock, contextBlock, heritageBlock),
                LocalDateTime.now()
        );
    }

    // ─── Construcción del prompt del Consejo ──────────────────────────────────

    private String buildCouncilPrompt(Family family, FamilyLegacy legacy, String dnaBlock,
                                      String contextBlock, String heritageBlock, CouncilRequest req) {

        StringBuilder sb = new StringBuilder();

        // Identidad del Consejo
        sb.append("""
            <council_identity>
            Eres el Consejo Familiar de la familia %s.

            No eres una inteligencia artificial genérica que responde desde el conocimiento de internet.
            Eres la VOZ COLECTIVA de esta familia específica, construida exclusivamente desde:
              — Su constitución y principios fundadores
              — Sus valores acordados
              — Sus fortalezas y sombras reconocidas
              — Su historia y aprendizajes vividos
              — Su estado emocional y relacional actual

            REGLAS ABSOLUTAS DEL CONSEJO:
            1. Solo cita valores, principios y aprendizajes que ESTA familia ya definió. Nunca inventes.
            2. Si la familia no ha definido algo, dilo honestamente: "Aún no han definido este principio juntos."
            3. Responde desde la primera persona plural: "Como familia hemos acordado...", "Nuestros valores nos dicen..."
            4. Tono: solemne pero cálido. Como un abuelo sabio que conoce bien a esta familia.
            5. Máximo 4 párrafos. Concreto y accionable.
            </council_identity>

            """.formatted(family.getName()));

        // Constitución familiar
        if (legacy != null) {
            sb.append("<family_constitution>\n");
            appendField(sb, "Principio fundador", legacy.getFoundingPrinciple());
            appendField(sb, "Misión familiar",    legacy.getFamilyMission());
            appendField(sb, "Visión familiar",    legacy.getFamilyVision());
            appendField(sb, "Compromisos",        legacy.getCommitments());
            appendField(sb, "Lo que jamás haremos", legacy.getNeverDo());
            appendField(sb, "Resolución de conflictos", legacy.getConflictResolution());
            appendField(sb, "Lecciones de la historia",  legacy.getHistoryLessons());
            appendField(sb, "Reconocimiento ancestral",  legacy.getHistoryRecognition());
            sb.append("</family_constitution>\n\n");
        } else {
            sb.append("""
                <family_constitution>
                Esta familia aún no ha definido su constitución formal.
                Responde con base en el ADN y el contexto disponibles.
                Al final, sugiere que definan su principio fundador para enriquecer futuras consultas.
                </family_constitution>

                """);
        }

        // ADN familiar
        if (dnaBlock != null) {
            sb.append("<family_dna>\n").append(dnaBlock).append("\n</family_dna>\n\n");
        }

        // Estado actual
        if (contextBlock != null) {
            sb.append("<family_context_now>\n").append(contextBlock).append("\n</family_context_now>\n\n");
        }

        // Herencia generacional
        if (heritageBlock != null) {
            sb.append("<generational_heritage>\n").append(heritageBlock).append("\n</generational_heritage>\n\n");
        }

        // Pregunta / tema
        sb.append("""
            <council_question>
            Tema: %s
            Consulta: %s
            %s
            </council_question>

            Responde como el Consejo Familiar. Cita explícitamente al menos UN valor o principio
            que esta familia ya definió. Si detectas una crisis activa, prioriza contención antes que consejo.
            """.formatted(
                req.topic() != null ? req.topic() : "General",
                req.question(),
                req.context() != null ? "Contexto adicional: " + req.context() : ""
        ));

        return sb.toString();
    }

    private void appendField(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.strip()).append("\n");
        }
    }

    private List<String> buildSourcesUsed(FamilyLegacy legacy, String dna,
                                           String context, String heritage) {
        List<String> sources = new ArrayList<>();
        if (legacy != null && legacy.getFoundingPrinciple() != null) sources.add("Constitución familiar");
        if (legacy != null && legacy.getFamilyMission()     != null) sources.add("Misión y visión");
        if (legacy != null && legacy.getHistoryLessons()    != null) sources.add("Historia familiar");
        if (dna     != null) sources.add("ADN Familiar");
        if (context != null) sources.add("Estado actual");
        if (heritage != null) sources.add("Herencia generacional");
        return sources;
    }
}
