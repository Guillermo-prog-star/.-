package com.integrityfamily.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Servicio de carga del banco maestro de 1000 reactivos psicométricos.
 * Lee questions-bank-v2.json desde classpath y persiste en BD de forma idempotente.
 *
 * Taxonomía soportada:
 *   - Pilares: reconocimiento (W1-M3) | amor (M6-M12) | entrega (M18-M36)
 *   - Hitos: W1 M1 M3 M6 M9 M12 M18 M24 M30 M36
 *   - Tipos: CORE | ADAPTIVE | FASE_PILLAR | MIRROR | EXPLORATORY
 *   - Dimensiones: emociones | comunicacion | habitos | tiempos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionBankLoaderService {

    private static final String BANK_FILE = "questions-bank-v2.json";

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Carga todas las preguntas del banco JSON a la BD.
     * Idempotente: omite preguntas cuyo questionKey ya existe.
     *
     * @return número de preguntas nuevas persistidas
     */
    @Transactional
    public int loadAll() {
        log.info(">>>> [LOADER] Iniciando carga del banco maestro de reactivos desde '{}'...", BANK_FILE);

        ClassPathResource resource = new ClassPathResource(BANK_FILE);
        if (!resource.exists()) {
            log.error(">>>> [LOADER] No se encontró '{}' en classpath. Abortando carga.", BANK_FILE);
            return 0;
        }

        try (InputStream is = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode questionsNode = root.path("questions");

            if (!questionsNode.isArray()) {
                log.error(">>>> [LOADER] El JSON no contiene un array 'questions'. Estructura inválida.");
                return 0;
            }

            int total = questionsNode.size();

            // Pre-cargar todas las claves existentes en un Set — 1 sola query
            Set<String> existingKeys = new HashSet<>(
                questionRepository.findAll().stream()
                    .map(Question::getQuestionKey)
                    .filter(Objects::nonNull)
                    .toList()
            );
            log.info(">>>> [LOADER] Claves existentes en BD: {}", existingKeys.size());

            int inserted = 0;
            int skipped  = 0;
            List<Question> batch = new ArrayList<>(100);

            for (JsonNode node : questionsNode) {
                String qKey = node.path("id").asText(null);
                if (qKey == null || qKey.isBlank()) continue;

                if (existingKeys.contains(qKey)) {
                    skipped++;
                    continue;
                }

                Question q = mapNodeToQuestion(node, qKey);
                batch.add(q);
                existingKeys.add(qKey); // Evitar duplicados dentro del mismo JSON
                inserted++;

                // Flush por lotes de 100 para evitar presión en el contexto JPA
                if (batch.size() == 100) {
                    questionRepository.saveAll(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                questionRepository.saveAll(batch);
            }

            log.info(">>>> [LOADER] Carga completada. Total JSON={} | Nuevas={} | Omitidas(dup)={}",
                    total, inserted, skipped);
            return inserted;

        } catch (Exception e) {
            log.error(">>>> [LOADER] Error crítico durante la carga del banco de preguntas", e);
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Mapeo JSON → Entidad
    // -------------------------------------------------------------------------

    private Question mapNodeToQuestion(JsonNode node, String qKey) {
        return Question.builder()
                .questionKey(qKey)
                .text(node.path("text").asText(""))
                .dimension(node.path("dimension").asText(null))
                .pillarName(node.path("pillarName").asText(null))
                .milestoneCode(node.path("milestoneCode").asText(null))
                .phase(node.path("phase").asText(null))
                .type(node.path("type").asText("CORE"))
                .memberType(node.path("memberType").asText("familia"))
                .severityWeight(parseDouble(node.path("severityWeight")))
                .detectsRelapse(node.path("detectsRelapse").asBoolean(false))
                .requiresEvidence(node.path("requiresEvidence").asBoolean(false))
                .reverseQuestion(node.path("reverseQuestion").asBoolean(false))
                .category(node.path("category").asText(null))
                .adaptiveTriggers(node.path("adaptiveTriggers").asText(null))
                .evidenceType(node.path("evidenceType").asText(null))
                .riskType(node.path("riskType").asText(null))
                .missionGenerator(node.path("missionGenerator").asText(null))
                // Campos legados con valores coherentes
                .pillar(node.path("milestoneCode").asText(null))   // campo legado 'pillar' = milestone
                .direction("POSITIVE")
                .version("2.0")
                .active(true)
                .weight(1)
                .build();
    }

    private Double parseDouble(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return node.asDouble();
        } catch (Exception e) {
            return null;
        }
    }
}
