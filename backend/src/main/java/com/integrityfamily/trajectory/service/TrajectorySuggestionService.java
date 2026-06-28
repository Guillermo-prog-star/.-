package com.integrityfamily.trajectory.service;

import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.RiskTrajectory;
import com.integrityfamily.domain.TrajectoryStatus;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.RiskTrajectoryRepository;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.TrajectoryBankItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Sugiere trayectorias de riesgo relevantes a partir de las señales del estado longitudinal
 * y del historial de evaluación de la familia. No asigna: solo recomienda para revisión humana.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrajectorySuggestionService {

    private final FamilyLongitudinalStateRepository ltsRepository;
    private final FamilyRiskTrajectoryRepository familyTrajectoryRepo;
    private final RiskTrajectoryRepository trajectoryRepo;

    public record TrajectorySuggestion(
        String code,
        String name,
        String macrodomain,
        String severityDefault,
        String reason,
        int confidenceScore   // 1-100
    ) {}

    @Transactional(readOnly = true)
    public List<TrajectorySuggestion> suggest(Long familyId) {
        FamilyLongitudinalState lts = ltsRepository.findByFamilyId(familyId).orElse(null);

        // Códigos ya asignados (no sugerir de nuevo)
        Set<String> alreadyAssigned = familyTrajectoryRepo.findByFamilyId(familyId).stream()
            .filter(frt -> frt.getStatus() != TrajectoryStatus.CLOSED)
            .map(frt -> frt.getTrajectory().getCode())
            .collect(Collectors.toSet());

        Map<String, RiskTrajectory> bank = trajectoryRepo.findByActiveTrue().stream()
            .collect(Collectors.toMap(RiskTrajectory::getCode, t -> t));

        List<TrajectorySuggestion> suggestions = new ArrayList<>();

        if (lts == null) {
            log.debug("[SUGGESTIONS] Sin LTS para familia {}, sin sugerencias posibles", familyId);
            return suggestions;
        }

        String risk = lts.getCurrentRiskLevel();
        String trend = lts.getRiskTrend();
        boolean critico = "CRITICO".equalsIgnoreCase(risk);
        boolean alto = "ALTO".equalsIgnoreCase(risk) || critico;
        boolean deteriorando = "DETERIORATING".equalsIgnoreCase(trend) || "CRITICAL".equalsIgnoreCase(trend);
        boolean colapsoComunicacional = Boolean.TRUE.equals(lts.getCommunicationCollapseActive());
        boolean crisisReciente = lts.getCrisisCount30d() != null && lts.getCrisisCount30d() >= 1;
        boolean deterioroSostenido = lts.getConsecutiveDeteriorations() != null && lts.getConsecutiveDeteriorations() >= 3;

        // Emociones críticas → salud mental, identidad
        if (lts.getDimEmociones() != null && lts.getDimEmociones() < 40) {
            suggest(suggestions, bank, alreadyAssigned, "IDEACION_SUICIDA",
                "Dimensión emocional muy baja (" + String.format("%.0f", lts.getDimEmociones()) + "/100)",
                critico ? 80 : 60);
            suggest(suggestions, bank, alreadyAssigned, "AISLAMIENTO_SOCIAL",
                "Dimensión emocional muy baja (" + String.format("%.0f", lts.getDimEmociones()) + "/100)",
                55);
            suggest(suggestions, bank, alreadyAssigned, "AUTOLESIONES",
                "Dimensión emocional crítica",
                critico ? 70 : 45);
        }

        // Comunicación crítica
        if (colapsoComunicacional) {
            suggest(suggestions, bank, alreadyAssigned, "VIOLENCIA_INTRAFAMILIAR",
                "Colapso comunicacional activo detectado", 65);
            suggest(suggestions, bank, alreadyAssigned, "CRISIS_PAREJA",
                "Colapso comunicacional activo detectado", 70);
            suggest(suggestions, bank, alreadyAssigned, "DIVORCIO_SEPARACION",
                "Colapso comunicacional sostenido", 55);
        }
        if (lts.getDimComunicacion() != null && lts.getDimComunicacion() < 40) {
            suggest(suggestions, bank, alreadyAssigned, "CRIANZA_AUTORITARIA",
                "Comunicación familiar crítica (" + String.format("%.0f", lts.getDimComunicacion()) + "/100)", 50);
            suggest(suggestions, bank, alreadyAssigned, "CRIANZA_PERMISIVA",
                "Comunicación familiar crítica", 45);
        }

        // Hábitos críticos → adicciones
        if (lts.getDimHabitos() != null && lts.getDimHabitos() < 40) {
            suggest(suggestions, bank, alreadyAssigned, "CONSUMO_ALCOHOL_ADULTO",
                "Hábitos familiares críticos (" + String.format("%.0f", lts.getDimHabitos()) + "/100)", 60);
            suggest(suggestions, bank, alreadyAssigned, "CONSUMO_MARIHUANA",
                "Hábitos familiares críticos", 55);
            suggest(suggestions, bank, alreadyAssigned, "USO_PROBLEMATICO_VIDEOJUEGOS",
                "Hábitos familiares críticos", 45);
        }

        // Tiempos compartidos críticos
        if (lts.getDimTiempos() != null && lts.getDimTiempos() < 35) {
            suggest(suggestions, bank, alreadyAssigned, "ABANDONO_ADULTO_MAYOR",
                "Tiempo compartido muy bajo (" + String.format("%.0f", lts.getDimTiempos()) + "/100)", 50);
            suggest(suggestions, bank, alreadyAssigned, "JOVEN_SIN_PROYECTO",
                "Tiempo compartido muy bajo", 45);
        }

        // Crisis recientes
        if (crisisReciente) {
            suggest(suggestions, bank, alreadyAssigned, "VIOLENCIA_INTRAFAMILIAR",
                lts.getCrisisCount30d() + " crisis en los últimos 30 días", 70);
            suggest(suggestions, bank, alreadyAssigned, "DUELO_COMPLICADO",
                "Crisis reciente registrada", 50);
        }

        // Deterioro sostenido general
        if (deterioroSostenido) {
            suggest(suggestions, bank, alreadyAssigned, "DESEMPLEO_PROLONGADO",
                lts.getConsecutiveDeteriorations() + " registros negativos consecutivos", 55);
            suggest(suggestions, bank, alreadyAssigned, "ENDEUDAMIENTO_FAMILIAR",
                "Deterioro sostenido del estado familiar", 50);
        }

        // ICF muy bajo
        if (lts.getIcfCurrent() != null && lts.getIcfCurrent() < 35) {
            suggest(suggestions, bank, alreadyAssigned, "RUPTURA_GENERACIONAL",
                "ICF muy bajo (" + String.format("%.1f", lts.getIcfCurrent()) + "/100)", 45);
        }

        // Riesgo crítico general
        if (critico && deteriorando) {
            suggest(suggestions, bank, alreadyAssigned, "CRISIS_PAREJA",
                "Riesgo CRÍTICO con tendencia deteriorante", 85);
        }

        // Ordenar por confidencia y limitar a 5 sugerencias más relevantes
        suggestions.sort(Comparator.comparingInt(TrajectorySuggestion::confidenceScore).reversed());
        return suggestions.stream().limit(5).toList();
    }

    private void suggest(List<TrajectorySuggestion> list, Map<String, RiskTrajectory> bank,
                         Set<String> alreadyAssigned, String code, String reason, int score) {
        if (alreadyAssigned.contains(code)) return;
        RiskTrajectory t = bank.get(code);
        if (t == null) return;
        // No duplicar el mismo código
        boolean exists = list.stream().anyMatch(s -> s.code().equals(code));
        if (exists) return;
        list.add(new TrajectorySuggestion(t.getCode(), t.getName(),
            t.getMacrodomain().name(), t.getSeverityDefault(), reason, score));
    }
}
