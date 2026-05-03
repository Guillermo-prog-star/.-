package com.integrityfamily.lts.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.lts.domain.*;
import com.integrityfamily.lts.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LearningSessionService {

    private final SessionRepository sessionRepository;
    private final AttemptRepository attemptRepository;
    private final ErrorRepository errorRepository;
    private final HypothesisRepository hypothesisRepository;
    private final InsightRepository insightRepository;
    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public LearningSession createSession(Long familyId, Long memberId, String topic, String objective) {
        log.info("[LTS] Creando nueva sesión de aprendizaje para miembro {} en familia {}", memberId, familyId);
        
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada", "FAMILY_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));
        
        FamilyMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado", "MEMBER_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        LearningSession session = LearningSession.builder()
                .family(family)
                .member(member)
                .topic(topic)
                .objective(objective)
                .status(LearningSession.SessionStatus.ACTIVE)
                .build();

        return sessionRepository.save(session);
    }

    @Transactional
    public Attempt addAttempt(Long sessionId, String content) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Sesión no encontrada", "SESSION_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        List<Attempt> existing = attemptRepository.findBySessionIdOrderByVersionAsc(sessionId);
        int nextVersion = existing.size() + 1;

        Attempt attempt = Attempt.builder()
                .session(session)
                .version(nextVersion)
                .content(content)
                .build();

        log.info("[LTS] Registrando intento v{} para sesión {}", nextVersion, sessionId);
        return attemptRepository.save(attempt);
    }

    @Transactional
    public LearningError defineError(Long attemptId, String type, String description) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new BusinessException("Intento no encontrado", "ATTEMPT_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        LearningError error = LearningError.builder()
                .attempt(attempt)
                .errorType(type)
                .description(description)
                .build();

        log.info("[LTS] Error definido para el intento {}: {}", attemptId, type);
        return errorRepository.save(error);
    }

    @Transactional
    public Hypothesis formulateHypothesis(Long errorId, String content) {
        LearningError error = errorRepository.findById(errorId)
                .orElseThrow(() -> new BusinessException("Error no encontrado", "ERROR_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        Hypothesis hypothesis = Hypothesis.builder()
                .error(error)
                .content(content)
                .build();

        log.info("[LTS] Hipótesis formulada para el error {}: {}", errorId, content);
        return hypothesisRepository.save(hypothesis);
    }

    @Transactional
    public Insight generateInsight(Long sessionId, String whatLearned, String transfer) {
        LearningSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException("Sesión no encontrada", "SESSION_NOT_FOUND", org.springframework.http.HttpStatus.NOT_FOUND));

        Insight insight = Insight.builder()
                .session(session)
                .whatLearned(whatLearned)
                .transfer(transfer)
                .build();

        session.setStatus(LearningSession.SessionStatus.COMPLETED);
        sessionRepository.save(session);

        log.info("[LTS] Insight generado para sesión {}: {}", sessionId, whatLearned);
        return insightRepository.save(insight);
    }
}
