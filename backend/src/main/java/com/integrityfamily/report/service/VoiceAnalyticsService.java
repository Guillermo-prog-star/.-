package com.integrityfamily.report.service;

import com.integrityfamily.report.repository.VoiceAuditRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SDD-VOICE-ANALYTICS: Motor de agregaciÃƒÂ³n de mÃƒÂ©tricas de audio.
 * Postura TÃƒÂ©cnica: Se prioriza la eficiencia en BD y la seguridad de tipos en Java 17.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAnalyticsService {

    private final VoiceAuditRepository voiceAuditRepository;
    private final FamilyRepository familyRepository;

    /**
     * Genera un resumen ejecutivo de KPIs.
     * SDD FIX: HashMap explÃƒÂ­cito evita 'incompatible types' de inferencia de tipos mixtos.
     */
    public Map<String, Object> getSummaryStats() {
        log.info("Ã°Å¸â€œÅ  [VOICE-ANALYSIS] Calculando resumen global de auditorÃƒÂ­a");
        
        long total = voiceAuditRepository.count();
        long successful = voiceAuditRepository.countSuccessfulMessages();
        double successRate = total == 0 ? 0 : (double) successful / total * 100;
        
        // SDD OPTIMIZATION: Consultas directas al repositorio sincronizado
        long activeFamilies = voiceAuditRepository.countDistinctFamilyId(); 
        long totalDuration = voiceAuditRepository.sumDurationSeconds();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMessages", total);
        stats.put("successRate", Math.round(successRate * 10.0) / 10.0);
        stats.put("activeFamilies", activeFamilies);
        stats.put("totalDuration", totalDuration);
        
        return stats;
    }

    /**
     * Recupera trazabilidad de las ÃƒÂºltimas 10 interacciones con resoluciÃƒÂ³n de nombres.
     */
    public List<Map<String, Object>> getRecentInteractions() {
        log.info("Ã°Å¸â€Â [VOICE-ANALYSIS] Recuperando interacciones recientes");
        return voiceAuditRepository.findTop10ByOrderByProcessedAtDesc().stream()
                .map(audit -> {
                    String familyName = familyRepository.findById(audit.getFamilyId())
                            .map(f -> f.getName()).orElse("Desconocido");
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("family", familyName);
                    item.put("municipio", audit.getMunicipio() != null ? audit.getMunicipio() : "N/A");
                    item.put("duration", audit.getDurationSeconds());
                    item.put("status", Boolean.TRUE.equals(audit.getSuccess()) ? "SUCCESS" : "ERROR");
                    item.put("processedAt", audit.getProcessedAt());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * Genera mÃƒÂ©tricas de alcance territorial para reportes regionales.
     */
    public List<Map<String, Object>> getRegionalStats() {
        log.info("Ã°Å¸â€”ÂºÃ¯Â¸Â [VOICE-ANALYSIS] Generando estadÃƒÂ­sticas regionales");
        return voiceAuditRepository.getRegionalUsage().stream()
                .map(row -> {
                    Map<String, Object> region = new HashMap<>();
                    region.put("name", row[0] != null ? row[0] : "Desconocido");
                    region.put("count", row[1]);
                    return region;
                })
                .collect(Collectors.toList());
    }
}


