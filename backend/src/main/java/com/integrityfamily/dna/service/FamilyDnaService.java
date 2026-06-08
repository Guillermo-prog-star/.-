package com.integrityfamily.dna.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.dna.domain.FamilyDna;
import com.integrityfamily.dna.dto.FamilyDnaDto;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyDnaService {

    private final FamilyDnaRepository dnaRepository;
    private final FamilyRepository familyRepository;
    private final EvaluationRepository evaluationRepository;
    private final RiskSnapshotRepository riskRepository;
    private final AiProvider aiProvider;
    private final ObjectMapper objectMapper;

    // ─── Consulta ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<FamilyDnaDto> findByFamilyId(Long familyId) {
        return dnaRepository.findByFamilyId(familyId).map(this::toDto);
    }

    // ─── Síntesis / Actualización ─────────────────────────────────────────────

    /**
     * Sintetiza el ADN Familiar a partir de evaluaciones, snapshots de riesgo
     * y miembros activos. Llama a Claude para generar la narrativa y las listas.
     * Si ya existe un ADN, lo actualiza (incrementa versión).
     */
    @Transactional
    public FamilyDnaDto synthesize(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        String prompt = buildSynthesisPrompt(family);
        log.info("[DNA] Sintetizando ADN para familia {} ({})", familyId, family.getName());

        String rawJson = aiProvider.generateRawResponse(prompt);
        log.debug("[DNA] Respuesta IA (raw): {}", rawJson);

        FamilyDna dna = dnaRepository.findByFamilyId(familyId)
                .orElse(FamilyDna.builder().familyId(familyId).build());

        applyAiResponse(dna, rawJson);
        dnaRepository.save(dna);

        log.info("[DNA] ADN v{} guardado para familia {}", dna.getVersion(), familyId);
        return toDto(dna);
    }

    // ─── Construcción del prompt ──────────────────────────────────────────────

    private String buildSynthesisPrompt(Family family) {
        List<Evaluation> evals = evaluationRepository
                .findByFamilyId(family.getId()).stream()
                .filter(e -> EvaluationStatus.FINALIZED.equals(e.getStatus()))
                .collect(Collectors.toList());
        List<RiskSnapshot> snapshots = riskRepository
                .findByFamilyIdOrderByCreatedAtDesc(family.getId());

        String miembros = family.getMembers().stream()
                .filter(FamilyMember::isActive)
                .map(m -> "- " + m.getFullName() + " (" + m.getRole() + ")")
                .collect(Collectors.joining("\n"));

        String evalResumen = evals.stream()
                .limit(5)
                .map(e -> {
                    String dims = e.getDimensionScores().stream()
                            .map(ds -> ds.getDimensionName() + "=" + String.format("%.1f", ds.getScore()))
                            .collect(Collectors.joining(", "));
                    return "• " + (e.getFinalizedAt() != null ? e.getFinalizedAt().toLocalDate() : "N/A")
                            + " → ICF=" + String.format("%.1f", e.getIcf() != null ? e.getIcf() : 0.0)
                            + " [" + dims + "]";
                })
                .collect(Collectors.joining("\n"));

        String riskResumen = snapshots.stream()
                .limit(3)
                .map(s -> "• " + s.getCreatedAt().toLocalDate()
                        + " ICF=" + String.format("%.1f", s.getIcf())
                        + " riesgo=" + s.getRiskLevel()
                        + " conciencia=" + s.getConsciousnessLabel())
                .collect(Collectors.joining("\n"));

        return """
            Eres el motor de síntesis del ADN Familiar de Integrity Family.
            Tu tarea es analizar la historia de esta familia y generar su ADN en formato JSON puro.

            FAMILIA: %s
            MIEMBROS ACTIVOS:
            %s

            HISTORIAL DE EVALUACIONES (últimas 5):
            %s

            SNAPSHOTS DE RIESGO (últimos 3):
            %s

            Genera un JSON con esta estructura exacta (sin texto adicional, solo el JSON):
            {
              "valores": ["valor1", "valor2", "valor3"],
              "fortalezas": ["fortaleza1", "fortaleza2", "fortaleza3"],
              "sombras": ["sombra1", "sombra2"],
              "patrones": ["patrón observable 1", "patrón observable 2"],
              "estiloComunicacion": "descripción en 1-2 oraciones del estilo comunicacional",
              "ritmoFamiliar": "descripción en 1-2 oraciones del ritmo y ciclos de la familia",
              "potencialOculto": [
                {"miembro": "nombre", "talento": "talento específico", "descripcion": "descripción breve"}
              ],
              "narrativaIa": "párrafo cálido de 3-4 oraciones que describe quiénes son como familia, qué los mueve y qué pueden llegar a ser"
            }

            Reglas:
            - Si hay pocos datos, infiere con sabiduría desde lo que existe.
            - Las listas deben tener entre 2 y 5 ítems.
            - El lenguaje debe ser cálido, humano y no clínico.
            - Responde ÚNICAMENTE con el JSON, sin explicaciones ni markdown.
            """.formatted(family.getName(), miembros, evalResumen, riskResumen);
    }

    // ─── Mapeo de respuesta IA al entity ─────────────────────────────────────

    private void applyAiResponse(FamilyDna dna, String rawJson) {
        try {
            String cleaned = extractJson(rawJson);
            Map<String, Object> parsed = objectMapper.readValue(cleaned, new TypeReference<>() {});

            dna.setValores(toJsonArray(parsed.get("valores")));
            dna.setFortalezas(toJsonArray(parsed.get("fortalezas")));
            dna.setSombras(toJsonArray(parsed.get("sombras")));
            dna.setPatrones(toJsonArray(parsed.get("patrones")));
            dna.setEstiloComunicacion(asString(parsed.get("estiloComunicacion")));
            dna.setRitmoFamiliar(asString(parsed.get("ritmoFamiliar")));
            dna.setPotencialOculto(toJsonRaw(parsed.get("potencialOculto")));
            dna.setNarrativaIa(asString(parsed.get("narrativaIa")));

        } catch (Exception e) {
            log.error("[DNA] Error parseando respuesta IA: {}", e.getMessage());
            dna.setNarrativaIa("El ADN familiar está siendo sintetizado. Realiza una evaluación para enriquecer este perfil.");
        }
    }

    private String extractJson(String raw) {
        if (raw == null) return "{}";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        return raw;
    }

    @SuppressWarnings("unchecked")
    private String toJsonArray(Object obj) {
        try {
            if (obj instanceof List<?> list) return objectMapper.writeValueAsString(list);
            return "[]";
        } catch (Exception e) { return "[]"; }
    }

    @SuppressWarnings("unchecked")
    private String toJsonRaw(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) { return "[]"; }
    }

    private String asString(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    // ─── DTO mapping ──────────────────────────────────────────────────────────

    private FamilyDnaDto toDto(FamilyDna dna) {
        return new FamilyDnaDto(
                dna.getFamilyId(),
                parseList(dna.getValores()),
                parseList(dna.getFortalezas()),
                parseList(dna.getSombras()),
                parseList(dna.getPatrones()),
                dna.getEstiloComunicacion(),
                dna.getRitmoFamiliar(),
                parsePotencial(dna.getPotencialOculto()),
                dna.getNarrativaIa(),
                dna.getVersion() != null ? dna.getVersion() : 1,
                dna.getUpdatedAt()
        );
    }

    private List<String> parseList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) { return Collections.emptyList(); }
    }

    private List<FamilyDnaDto.PotencialMiembroDto> parsePotencial(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<Map<String, String>> raw = objectMapper.readValue(json, new TypeReference<>() {});
            return raw.stream()
                    .map(m -> new FamilyDnaDto.PotencialMiembroDto(
                            m.getOrDefault("miembro", ""),
                            m.getOrDefault("talento", ""),
                            m.getOrDefault("descripcion", "")))
                    .collect(Collectors.toList());
        } catch (Exception e) { return Collections.emptyList(); }
    }

    // ─── Snapshot para ContextSynthesizer ────────────────────────────────────

    /**
     * Retorna un resumen textual compacto del ADN para inyectar en el prompt de IA.
     * Falla silenciosamente — nunca bloquea el chat.
     */
    @Transactional(readOnly = true)
    public String buildDnaContextBlock(Long familyId) {
        try {
            return dnaRepository.findByFamilyId(familyId).map(dna -> {
                StringBuilder sb = new StringBuilder("ADN Familiar:\n");
                List<String> vals = parseList(dna.getValores());
                List<String> fors = parseList(dna.getFortalezas());
                List<String> som  = parseList(dna.getSombras());
                List<String> pat  = parseList(dna.getPatrones());
                if (!vals.isEmpty()) sb.append("  Valores: ").append(String.join(", ", vals)).append("\n");
                if (!fors.isEmpty()) sb.append("  Fortalezas: ").append(String.join(", ", fors)).append("\n");
                if (!som.isEmpty())  sb.append("  Sombras: ").append(String.join(", ", som)).append("\n");
                if (!pat.isEmpty())  sb.append("  Patrones: ").append(String.join(", ", pat)).append("\n");
                if (dna.getEstiloComunicacion() != null) sb.append("  Comunicación: ").append(dna.getEstiloComunicacion()).append("\n");
                if (dna.getRitmoFamiliar() != null) sb.append("  Ritmo: ").append(dna.getRitmoFamiliar()).append("\n");
                return sb.toString();
            }).orElse(null);
        } catch (Exception e) {
            log.warn("[DNA] No se pudo construir bloque de contexto para familia {}: {}", familyId, e.getMessage());
            return null;
        }
    }
}
