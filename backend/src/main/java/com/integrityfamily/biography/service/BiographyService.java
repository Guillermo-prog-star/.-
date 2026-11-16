package com.integrityfamily.biography.service;

import com.integrityfamily.biography.domain.BiographyAnswer;
import com.integrityfamily.biography.domain.BiographyQuestion;
import com.integrityfamily.biography.dto.BiographyDtos.AnswerRequest;
import com.integrityfamily.biography.dto.BiographyDtos.AnswerResponse;
import com.integrityfamily.biography.dto.BiographyDtos.ChapterResponse;
import com.integrityfamily.biography.dto.BiographyDtos.QuestionResponse;
import com.integrityfamily.biography.repository.BiographyAnswerRepository;
import com.integrityfamily.biography.repository.BiographyQuestionRepository;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.EntryVisibility;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diario guiado biográfico: un miembro narra, en primera persona, su propia
 * historia de vida respondiendo al catálogo fijo de {@link BiographyQuestion}
 * (12 capítulos). Distinto de {@code LineageService} (ancestros narrados por
 * un tercero) y de {@code JournalService} (hitos del programa de 36 meses).
 * Auditoría (ADR-017): no se audita cada edición de texto (~75 preguntas
 * por miembro, ruido masivo), solo los eventos discretos que afectan
 * privacidad o son irreversibles — cambio de visibilidad y borrado.
 */
@Service
@RequiredArgsConstructor
public class BiographyService {

    private final BiographyQuestionRepository questionRepository;
    private final BiographyAnswerRepository answerRepository;
    private final MemberRepository memberRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ChapterResponse> getCatalog() {
        List<BiographyQuestion> questions = questionRepository.findAllByOrderByChapterNumberAscSortOrderAsc();

        Map<Integer, List<BiographyQuestion>> byChapter = questions.stream()
                .collect(Collectors.groupingBy(BiographyQuestion::getChapterNumber, LinkedHashMap::new, Collectors.toList()));

        return byChapter.entrySet().stream()
                .map(entry -> new ChapterResponse(
                        entry.getKey(),
                        entry.getValue().get(0).getChapterTitle(),
                        entry.getValue().stream().map(this::toQuestionResponse).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnswerResponse> getAnswers(Long familyId, Long targetMemberId, Long viewerMemberId) {
        requireMemberInFamily(targetMemberId, familyId);
        return answerRepository.findVisibleToMember(familyId, targetMemberId, viewerMemberId)
                .stream().map(this::toAnswerResponse).toList();
    }

    @Transactional
    public AnswerResponse upsertAnswer(Long familyId, Long questionId, AnswerRequest req, Long authorMemberId) {
        FamilyMember author = requireMemberInFamily(authorMemberId, familyId);
        BiographyQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException("Pregunta no encontrada.", "BIOGRAPHY_QUESTION_NOT_FOUND", HttpStatus.NOT_FOUND));

        BiographyAnswer answer = answerRepository.findByMemberIdAndQuestionId(authorMemberId, questionId)
                .orElseGet(() -> BiographyAnswer.builder()
                        .family(author.getFamily())
                        .member(author)
                        .question(question)
                        .build());

        answer.setAnswerText(req.answerText());
        answer.setEpistemicSource(req.epistemicSource());
        answer.setOccurredPeriod(req.occurredPeriod());
        answer.setMeaningThen(req.meaningThen());
        answer.setCurrentMeaning(req.currentMeaning());
        answer.setRelatedSourceNote(req.relatedSourceNote());
        answer.setPerceivedEventKey(req.perceivedEventKey());
        answer.setConfidenceLevel(req.confidenceLevel());
        // Visibilidad omitida → se conserva la existente (nunca des-compartir ni compartir en silencio);
        // en una respuesta nueva, PRIVATE por defecto.
        EntryVisibility previousVisibility = answer.getVisibility();
        if (req.visibility() != null) {
            answer.setVisibility(req.visibility());
        } else if (previousVisibility == null) {
            answer.setVisibility(EntryVisibility.PRIVATE);
        }
        answer.setCorrectionNote(req.correctionNote());
        answer.setUpdatedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(req.completed()) && answer.getCompletedAt() == null) {
            answer.setCompletedAt(LocalDateTime.now());
        } else if (Boolean.FALSE.equals(req.completed())) {
            answer.setCompletedAt(null);
        }

        BiographyAnswer saved = answerRepository.save(answer);

        if (previousVisibility != null && previousVisibility != saved.getVisibility()) {
            auditService.registerSystemEvent(author.getEmail(), AuditEventType.BIOGRAPHY_VISIBILITY_CHANGED,
                    String.format("{\"familyId\":%d,\"memberId\":%d,\"questionId\":%d,\"from\":\"%s\",\"to\":\"%s\"}",
                            familyId, authorMemberId, questionId, previousVisibility, saved.getVisibility()));
        }

        return toAnswerResponse(saved);
    }

    @Transactional
    public void deleteAnswer(Long familyId, Long questionId, Long authorMemberId) {
        FamilyMember author = requireMemberInFamily(authorMemberId, familyId);
        BiographyAnswer answer = answerRepository.findByMemberIdAndQuestionId(authorMemberId, questionId)
                .orElseThrow(() -> new BusinessException("Respuesta no encontrada.", "BIOGRAPHY_ANSWER_NOT_FOUND", HttpStatus.NOT_FOUND));
        answerRepository.delete(answer);
        auditService.registerSystemEvent(author.getEmail(), AuditEventType.BIOGRAPHY_ANSWER_DELETED,
                String.format("{\"familyId\":%d,\"memberId\":%d,\"questionId\":%d}", familyId, authorMemberId, questionId));
    }

    private FamilyMember requireMemberInFamily(Long memberId, Long familyId) {
        if (memberId == null) {
            throw new BusinessException("No tienes un perfil de miembro en esta familia.", "BIOGRAPHY_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
        FamilyMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("Miembro no encontrado.", "BIOGRAPHY_MEMBER_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (member.getFamily() == null || !member.getFamily().getId().equals(familyId)) {
            throw new BusinessException("El miembro no pertenece a esta familia.", "BIOGRAPHY_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private QuestionResponse toQuestionResponse(BiographyQuestion q) {
        return new QuestionResponse(q.getId(), q.getQuestionKey(), q.getPromptText(), q.getDeepenText(), q.getSortOrder());
    }

    /**
     * "Otra perspectiva": todas las respuestas (visibles para el viewer) que
     * distintos miembros de la familia etiquetaron con la misma
     * {@code perceivedEventKey} — la misma escena contada por varias personas.
     */
    @Transactional(readOnly = true)
    public List<AnswerResponse> getPerspectives(Long familyId, String perceivedEventKey, Long viewerMemberId) {
        return answerRepository.findPerspectivesVisibleToMember(familyId, perceivedEventKey, viewerMemberId)
                .stream()
                .map(this::toAnswerResponse)
                .toList();
    }

    private AnswerResponse toAnswerResponse(BiographyAnswer a) {
        return new AnswerResponse(
                a.getQuestion().getId(),
                a.getQuestion().getQuestionKey(),
                a.getAnswerText(),
                a.getEpistemicSource(),
                a.getOccurredPeriod(),
                a.getMeaningThen(),
                a.getCurrentMeaning(),
                a.getRelatedSourceNote(),
                a.getPerceivedEventKey(),
                a.getConfidenceLevel(),
                a.getVisibility(),
                a.getCorrectionNote(),
                a.getCompletedAt(),
                a.getUpdatedAt());
    }
}
