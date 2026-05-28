package com.integrityfamily.ai.service;

import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestiona el ciclo de vida de una ConversationSession.
 * Una sesión agrupa todos los turnos de una conversación continua.
 * Si el miembro regresa al chat tras 4 h de inactividad, se abre una sesión nueva.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationSessionService {

    private static final int SESSION_IDLE_HOURS = 4;

    private final ConversationSessionRepository sessionRepository;

    /**
     * Busca la sesión activa más reciente del miembro en la familia.
     * Si no existe o expiró (> 4 h), cierra las sesiones obsoletas y crea una nueva.
     */
    @Transactional
    public ConversationSession findOrCreateSession(Long familyId, Long memberId, String goal) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(SESSION_IDLE_HOURS);
        List<ConversationSession> active = sessionRepository.findActiveSessionsForMember(
                familyId, memberId, cutoff, PageRequest.of(0, 1));

        if (!active.isEmpty()) {
            return active.get(0);
        }

        // Cerrar sesiones abiertas obsoletas antes de crear la nueva
        sessionRepository.findOpenStaleSessionsForMember(familyId, memberId, cutoff)
                .forEach(s -> {
                    s.setEndedAt(LocalDateTime.now());
                    s.setOutcome("ABANDONED");
                    sessionRepository.save(s);
                    log.debug("[SESSION] Sesión stale cerrada: id={} turnos={}", s.getId(), s.getTurnCount());
                });

        log.info("[SESSION] Nueva sesión: familia={} miembro={} goal={}", familyId, memberId, goal);
        return sessionRepository.save(ConversationSession.builder()
                .familyId(familyId)
                .memberId(memberId)
                .goal(goal)
                .build());
    }

    /** Actualiza el estado emocional inferido del turno actual. */
    @Transactional
    public void updateEmotionalState(Long sessionId, String emotionalState) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            s.setEmotionalState(emotionalState);
            sessionRepository.save(s);
        });
    }

    /**
     * Incrementa el contador de turnos al finalizar cada intercambio usuario↔IA.
     * Devuelve el nuevo valor para que el caller pueda tomar decisiones (ej. análisis periódico).
     */
    @Transactional
    public int incrementTurnCount(Long sessionId) {
        var ref = new int[]{0};
        sessionRepository.findById(sessionId).ifPresent(s -> {
            int next = s.getTurnCount() + 1;
            s.setTurnCount(next);
            sessionRepository.save(s);
            ref[0] = next;
        });
        return ref[0];
    }

    /** Cierra una sesión con el outcome dado (COMPLETED | ABANDONED | ESCALATED). */
    @Transactional
    public void closeSession(Long sessionId, String outcome) {
        sessionRepository.findById(sessionId).ifPresent(s -> {
            if (s.isActive()) {
                s.setEndedAt(LocalDateTime.now());
                s.setOutcome(outcome);
                sessionRepository.save(s);
                log.info("[SESSION] Sesión {} cerrada con outcome={} turnos={}", sessionId, outcome, s.getTurnCount());
            }
        });
    }
}
