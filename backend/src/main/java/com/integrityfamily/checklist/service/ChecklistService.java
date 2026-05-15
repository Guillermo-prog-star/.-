package com.integrityfamily.checklist.service;

import com.integrityfamily.domain.ChecklistItem;
import com.integrityfamily.domain.repository.ChecklistRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private static final Logger log = LoggerFactory.getLogger(ChecklistService.class);
    private final ChecklistRepository checklistRepository;
    private final FamilyRepository familyRepository;

    public List<ChecklistItem> getFamilyChecklist(Long familyId) {
        return checklistRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    @Transactional
    public void markAsCompleted(Long id, String completedBy) {
        ChecklistItem item = checklistRepository.findById(id).orElseThrow();
        item.setCompleted(true);
        item.setCompletedBy(completedBy);
        item.setCompletedAt(LocalDateTime.now());
        checklistRepository.save(item);

        // [SDD Spec] EvaluaciÃƒÂ³n Determinista de Hito
        Long familyId = item.getFamily().getId();
        String source = item.getSource();
        
        long pending = checklistRepository.countByFamilyIdAndSourceAndCompletedFalse(familyId, source);
        if (pending == 0) {
            log.info("Ã°Å¸Å½Â¯ [MILESTONE-READY] Todas las tareas de la fuente '{}' han sido completadas para la familia {}.", source, familyId);
        } else {
            log.info("Ã°Å¸â€œË† [PROGRESS] Tareas pendientes para '{}': {}", source, pending);
        }
    }

    /**
     * Mapea un texto masivo de la IA (pilar, crisis, etc) y extrae actividades.
     * Replicando la lÃƒÂ³gica del monolito: extraer lÃƒÂ­neas con -, * o n.
     */
    @Transactional
    public int extractAndAdd(String text, String source, Long familyId) {
        Family family = familyRepository.findById(familyId).orElseThrow();
        List<String> lines = extractActionableLines(text);
        int added = 0;

        for (String line : lines) {
            ChecklistItem item = new ChecklistItem();
            item.setFamily(family);
            item.setDescription(line);
            item.setSource(source);
            item.setDimension(detectDimension(line));
            checklistRepository.save(item);
            added++;
        }
        
        log.info("Ã°Å¸â€œâ€¹ [CHECKLIST] Se han extraÃƒÂ­do {} nuevas actividades para la familia {}", added, familyId);
        return added;
    }

    private List<String> extractActionableLines(String text) {
        List<String> result = new ArrayList<>();
        if (text == null) return result;

        // Regex para lÃƒÂ­neas que empiezan con -, *, o 1. (tal cual el monolito)
        Pattern pattern = Pattern.compile("^\\s*([-*]|\\d+\\.)\\s+(.*)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String content = matcher.group(2).trim();
            if (content.length() >= 10 && !content.startsWith("#")) {
                result.add(content);
            }
        }
        return result;
    }

    @Transactional
    public ChecklistItem createChecklistItem(Long familyId, String description, String dimension, String source) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));
        ChecklistItem item = ChecklistItem.builder()
                .family(family)
                .description(description)
                .dimension(dimension != null ? dimension : "General")
                .source(source != null ? source : "SENTINEL")
                .completed(false)
                .createdAt(LocalDateTime.now())
                .build();
        return checklistRepository.save(item);
    }

    private String detectDimension(String text) {
        String t = text.toLowerCase();
        if (t.contains("reconoci") || t.contains("identidad") || t.contains("ver") || t.contains("observar")) return "Reconocimiento";
        if (t.contains("amor") || t.contains("afecto") || t.contains("cariÃƒÂ±o") || t.contains("vinculo")) return "Amor";
        if (t.contains("entrega") || t.contains("servicio") || t.contains("compromiso") || t.contains("donacion")) return "Entrega";
        return "General";
    }
}


