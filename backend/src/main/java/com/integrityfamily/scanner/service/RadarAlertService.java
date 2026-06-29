package com.integrityfamily.scanner.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Envía alertas proactivas WhatsApp cuando el Radar detecta señales de alta intensidad.
 *
 * Se puede llamar explícitamente desde el controlador (POST /radar/alert)
 * o desde un scheduler diario.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RadarAlertService {

    private final SubtleSignalRadarService radarService;
    private final FamilyRepository familyRepository;
    private final WhatsAppService whatsAppService;

    /**
     * Analiza el radar para una familia y envía WhatsApp si hay señales HIGH.
     * Si no tiene WhatsApp configurado, solo registra el log.
     *
     * @return true si se envió la alerta, false si no había señales HIGH o no hay número configurado.
     */
    @Transactional(readOnly = true)
    public boolean checkAndAlert(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        SubtleSignalRadarResponse radar = radarService.analyze(familyId);

        List<SubtleSignalRadarResponse.MicroSignal> highSignals = radar.microSignals().stream()
                .filter(s -> "HIGH".equals(s.severity()))
                .collect(Collectors.toList());

        if (highSignals.isEmpty()) {
            log.debug("[RADAR-ALERT] Familia {} sin señales HIGH — no se envía alerta.", familyId);
            return false;
        }

        String message = buildAlertMessage(family.getName(), highSignals, radar.confidenceScore());

        if (family.getWhatsapp() != null && !family.getWhatsapp().isBlank()) {
            log.info("[RADAR-ALERT] Enviando alerta WhatsApp a familia {} ({} señales HIGH)",
                    familyId, highSignals.size());
            try {
                whatsAppService.sendToFamily(family, message);
                return true;
            } catch (Exception e) {
                log.warn("[RADAR-ALERT] Error enviando WhatsApp a familia {}: {}", familyId, e.getMessage());
                return false;
            }
        } else {
            log.info("[RADAR-ALERT] Familia {} tiene {} señales HIGH pero no tiene WhatsApp configurado.",
                    familyId, highSignals.size());
            return false;
        }
    }

    private String buildAlertMessage(String familyName, List<SubtleSignalRadarResponse.MicroSignal> signals,
                                      int confidence) {
        StringBuilder sb = new StringBuilder();
        sb.append("🔴 *Radar de Señales Sutiles — Alerta para ").append(familyName).append("*\n\n");
        sb.append("El sistema detectó ").append(signals.size())
          .append(" señal").append(signals.size() > 1 ? "es" : "")
          .append(" de alta intensidad en su familia:\n\n");

        for (SubtleSignalRadarResponse.MicroSignal signal : signals) {
            sb.append("• *").append(capitalize(signal.dimension())).append("*: ")
              .append(signal.description()).append("\n");
        }

        sb.append("\n_Confianza del análisis: ").append(confidence).append("%_\n");
        sb.append("\nEntre a la aplicación para ver su plan de acción completo y los escenarios futuros.");
        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
