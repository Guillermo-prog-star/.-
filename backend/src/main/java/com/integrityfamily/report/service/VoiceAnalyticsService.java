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
 * SDD-VOICE-ANALYTICS: Motor de agregación de métricas de audio.
 * Postura Técnica: Se prioriza la eficiencia en BD y la seguridad de tipos en Java 17.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAnalyticsService {

    private final VoiceAuditRepository voiceAuditRepository;
    private final FamilyRepository familyRepository;

    /**
     * Genera un resumen ejecutivo de KPIs.
     * SDD FIX: HashMap explícito evita 'incompatible types' de inferencia de tipos mixtos.
     */
    public Map<String, Object> getSummaryStats() {
        log.info("📊 [VOICE-ANALYSIS] Calculando resumen global de auditoría");
        
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
     * Recupera trazabilidad de las últimas 10 interacciones con resolución de nombres.
     */
    public List<Map<String, Object>> getRecentInteractions() {
        log.info("🔍 [VOICE-ANALYSIS] Recuperando interacciones recientes");
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
     * Genera métricas de alcance territorial para reportes regionales.
     */
    public List<Map<String, Object>> getRegionalStats() {
        log.info("🗺️ [VOICE-ANALYSIS] Generando estadísticas regionales");
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


