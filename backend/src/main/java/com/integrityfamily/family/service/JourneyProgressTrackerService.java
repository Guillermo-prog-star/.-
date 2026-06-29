package com.integrityfamily.family.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyJourneySnapshot;
import com.integrityfamily.domain.repository.FamilyJourneySnapshotRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.family.dto.FamilyJourneyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Detecta cuando una familia avanza a un nuevo nivel del viaje y celebra el logro.
 *
 * Flujo por familia:
 *   1. Evalúa el nivel actual (FamilyJourneyService).
 *   2. Compara con el último snapshot persistido.
 *   3. Si subió de nivel: persiste snapshot con levelUp=true y envía WhatsApp.
 *   4. Si no subió: persiste snapshot de seguimiento (levelUp=false).
 *   5. Omite el snapshot si ya existe uno para hoy.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JourneyProgressTrackerService {

    private static final String[] LEVEL_NAMES = {
        "Plataforma", "Identidad Familiar", "Miembros", "Guardián Familiar",
        "ADN Familiar", "Diagnóstico Vivo", "Plan Familiar", "Misiones",
        "Sprint", "Daily", "Evidencias", "Consultor IA", "Consejo Familiar", "Legado"
    };

    private final FamilyJourneyService           journeyService;
    private final FamilyJourneySnapshotRepository snapshotRepository;
    private final FamilyRepository               familyRepository;
    private final WhatsAppService                whatsAppService;

    /**
     * Evalúa el progreso de una familia y detecta level-ups.
     *
     * @return true si se detectó y registró un nuevo nivel.
     */
    @Transactional
    public boolean trackAndCelebrate(Long familyId) {
        if (snapshotRepository.existsByFamilyIdAndSnapshotDate(familyId, LocalDate.now())) {
            log.debug("[JOURNEY-TRACKER] Familia {} ya tiene snapshot hoy — omitido.", familyId);
            return false;
        }

        FamilyJourneyResponse journey = journeyService.evaluate(familyId);
        int currentLevel    = journey.currentLevel();
        int currentProgress = journey.journeyProgress();

        Optional<FamilyJourneySnapshot> lastSnapshot =
                snapshotRepository.findTopByFamilyIdOrderBySnapshotDateDesc(familyId);

        boolean isLevelUp = lastSnapshot
                .map(s -> currentLevel > s.getJourneyLevel())
                .orElse(false); // primer snapshot nunca es level-up

        Integer previousLevel = isLevelUp ? lastSnapshot.map(FamilyJourneySnapshot::getJourneyLevel).orElse(null) : null;

        FamilyJourneySnapshot snapshot = new FamilyJourneySnapshot();
        snapshot.setFamilyId(familyId);
        snapshot.setJourneyLevel(currentLevel);
        snapshot.setJourneyProgress(currentProgress);
        snapshot.setLevelUp(isLevelUp);
        snapshot.setPreviousLevel(previousLevel);
        snapshot.setSnapshotDate(LocalDate.now());

        if (isLevelUp) {
            boolean sent = trySendCelebration(familyId, previousLevel, currentLevel, journey);
            snapshot.setCelebrationSent(sent);
            log.info("[JOURNEY-TRACKER] 🎉 Familia {} subió de nivel {} → {} (celebración: {})",
                    familyId, previousLevel, currentLevel, sent ? "enviada" : "sin WhatsApp");
        }

        snapshotRepository.save(snapshot);
        return isLevelUp;
    }

    /**
     * Reintenta enviar celebraciones pendientes (por si WhatsApp falló antes).
     */
    @Transactional
    public int retryCelebrations() {
        var pending = snapshotRepository.findByLevelUpTrueAndCelebrationSentFalse();
        int sent = 0;
        for (FamilyJourneySnapshot snap : pending) {
            boolean ok = trySendCelebration(
                    snap.getFamilyId(), snap.getPreviousLevel(), snap.getJourneyLevel(), null);
            if (ok) {
                snap.setCelebrationSent(true);
                snapshotRepository.save(snap);
                sent++;
            }
        }
        return sent;
    }

    // ─── Privados ────────────────────────────────────────────────────────────

    private boolean trySendCelebration(Long familyId, Integer fromLevel, int toLevel,
                                        FamilyJourneyResponse journey) {
        try {
            Family family = familyRepository.findById(familyId).orElse(null);
            if (family == null || family.getWhatsapp() == null || family.getWhatsapp().isBlank()) {
                return false;
            }
            String msg = buildCelebrationMessage(family.getName(), fromLevel, toLevel, journey);
            whatsAppService.sendToFamily(family, msg);
            return true;
        } catch (Exception e) {
            log.warn("[JOURNEY-TRACKER] Error enviando celebración a familia {}: {}", familyId, e.getMessage());
            return false;
        }
    }

    private String buildCelebrationMessage(String familyName, Integer fromLevel, int toLevel,
                                            FamilyJourneyResponse journey) {
        String newLevelName = toLevel < LEVEL_NAMES.length ? LEVEL_NAMES[toLevel] : "Nivel " + toLevel;
        String prevLevelName = (fromLevel != null && fromLevel < LEVEL_NAMES.length)
                ? LEVEL_NAMES[fromLevel] : "el nivel anterior";

        StringBuilder sb = new StringBuilder();
        sb.append("🎉 *¡Felicitaciones, ").append(familyName).append("!*\n\n");
        sb.append("Han superado *").append(prevLevelName).append("* y alcanzaron\n");
        sb.append("✨ *Nivel ").append(toLevel).append(": ").append(newLevelName).append("*\n\n");

        if (journey != null) {
            sb.append("📊 Progreso del viaje: *").append(journey.journeyProgress()).append("%*\n\n");
            sb.append("🧭 *Siguiente paso:*\n").append(journey.nextAction()).append("\n\n");
        }

        if (toLevel == 13) {
            sb.append("🏆 *¡Han completado el Viaje Familiar!*\n");
            sb.append("Su historia queda inscrita en el legado de Integrity Family.\n");
        }

        sb.append("\n_Continúa en la app para ver tu mapa completo._");
        return sb.toString();
    }
}
