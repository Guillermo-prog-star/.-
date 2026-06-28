package com.integrityfamily.capital.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyIcafAnswer;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.FamilyIcafAnswerRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IcafQuestionnaireService — Unit Tests")
class IcafQuestionnaireServiceTest {

    @Mock QuestionRepository         questionRepo;
    @Mock FamilyIcafAnswerRepository answerRepo;
    @Mock FamilyRepository           familyRepo;

    @InjectMocks IcafQuestionnaireService service;

    private static final Long   FAM_ID    = 1L;
    private static final String CONFIANZA = IcafQuestionnaireService.DOMAIN_CONFIANZA;
    private static final String BIENESTAR = IcafQuestionnaireService.DOMAIN_BIENESTAR;

    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Question question(String key, String direction) {
        return Question.builder()
                .questionKey(key)
                .text("Pregunta " + key)
                .icafDomain(CONFIANZA)
                .direction(direction)
                .active(true)
                .sortOrder(1)
                .build();
    }

    private FamilyIcafAnswer answer(String key, int score) {
        return FamilyIcafAnswer.builder()
                .family(family)
                .questionKey(key)
                .icafDomain(CONFIANZA)
                .score(score)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("getQuestions()")
    class GetQuestions {

        @Test
        @DisplayName("delega al repositorio con tipo ICAF y dominio")
        void delegatesToRepo() {
            List<Question> expected = List.of(question("ICAF_CONF_001", "POSITIVE"));
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA)).thenReturn(expected);

            List<Question> result = service.getQuestions(CONFIANZA);

            assertThat(result).isEqualTo(expected);
            verify(questionRepo).findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder("ICAF", CONFIANZA);
        }

        @Test
        @DisplayName("dominio sin preguntas → lista vacía")
        void emptyDomain() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    anyString(), anyString())).thenReturn(List.of());

            assertThat(service.getQuestions("desconocido")).isEmpty();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("getDomainScore()")
    class GetDomainScore {

        @Test
        @DisplayName("sin respuestas → Optional vacío")
        void noAnswers() {
            when(answerRepo.hasAnswers(FAM_ID, CONFIANZA)).thenReturn(false);

            assertThat(service.getDomainScore(FAM_ID, CONFIANZA)).isEmpty();
        }

        @Test
        @DisplayName("con respuestas avg=3 → score = (3-1)/4*100 = 50.0")
        void withAnswers() {
            when(answerRepo.hasAnswers(FAM_ID, CONFIANZA)).thenReturn(true);
            when(answerRepo.avgScoreByDomain(FAM_ID, CONFIANZA)).thenReturn(3.0);

            Optional<Double> score = service.getDomainScore(FAM_ID, CONFIANZA);

            assertThat(score).isPresent();
            assertThat(score.get()).isCloseTo(50.0, within(0.01));
        }

        @Test
        @DisplayName("avg=5 → score = 100.0")
        void maxScore() {
            when(answerRepo.hasAnswers(FAM_ID, CONFIANZA)).thenReturn(true);
            when(answerRepo.avgScoreByDomain(FAM_ID, CONFIANZA)).thenReturn(5.0);

            assertThat(service.getDomainScore(FAM_ID, CONFIANZA).get())
                    .isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("avg=1 → score = 0.0")
        void minScore() {
            when(answerRepo.hasAnswers(FAM_ID, CONFIANZA)).thenReturn(true);
            when(answerRepo.avgScoreByDomain(FAM_ID, CONFIANZA)).thenReturn(1.0);

            assertThat(service.getDomainScore(FAM_ID, CONFIANZA).get())
                    .isCloseTo(0.0, within(0.01));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("saveAnswers()")
    class SaveAnswers {

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound() {
            when(familyRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                service.saveAnswers(99L, Map.of("ICAF_CONF_001", 4), CONFIANZA, null)
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("99");
        }

        @Test
        @DisplayName("respuesta nueva (sin existente) → se guarda con answerRepo.save")
        void savesNewAnswer() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA))
                    .thenReturn(List.of(question("ICAF_CONF_001", "POSITIVE")));
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_001"))
                    .thenReturn(Optional.empty());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of(answer("ICAF_CONF_001", 4)));
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(1L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of("ICAF_CONF_001", 4), CONFIANZA, null);

            verify(answerRepo, times(1)).save(any(FamilyIcafAnswer.class));
            assertThat(result.savedCount()).isEqualTo(1);
            assertThat(result.domain()).isEqualTo(CONFIANZA);
        }

        @Test
        @DisplayName("respuesta existente → actualiza el score (no crea nuevo registro)")
        void updatesExistingAnswer() {
            FamilyIcafAnswer existing = answer("ICAF_CONF_001", 2);
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA))
                    .thenReturn(List.of(question("ICAF_CONF_001", "POSITIVE")));
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_001"))
                    .thenReturn(Optional.of(existing));
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of(existing));
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(1L);

            service.saveAnswers(FAM_ID, Map.of("ICAF_CONF_001", 5), CONFIANZA, "user@test.com");

            assertThat(existing.getScore()).isEqualTo(5);
            assertThat(existing.getAnsweredBy()).isEqualTo("user@test.com");
        }

        @Test
        @DisplayName("score fuera de rango (0 o 6) → se ignora, savedCount=0")
        void outOfRangeIgnored() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA)).thenReturn(List.of());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of());
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(0L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of("ICAF_CONF_001", 0, "ICAF_CONF_002", 6),
                            CONFIANZA, null);

            verify(answerRepo, never()).save(any());
            assertThat(result.savedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("pregunta POSITIVE score=5 → normalizada a 100.0")
        void positiveDirectionMax() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA))
                    .thenReturn(List.of(question("ICAF_CONF_001", "POSITIVE")));
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_001"))
                    .thenReturn(Optional.empty());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of(answer("ICAF_CONF_001", 5)));
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(1L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of("ICAF_CONF_001", 5), CONFIANZA, null);

            assertThat(result.score()).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("pregunta NEGATIVE score=1 → invertida a 100.0")
        void negativeDirectionInverted() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA))
                    .thenReturn(List.of(question("ICAF_CONF_007", "NEGATIVE")));
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_007"))
                    .thenReturn(Optional.empty());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of(answer("ICAF_CONF_007", 1)));
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(1L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of("ICAF_CONF_007", 1), CONFIANZA, null);

            // NEGATIVE: (5-1)/4*100 = 100
            assertThat(result.score()).isCloseTo(100.0, within(0.01));
        }

        @Test
        @DisplayName("pregunta NEGATIVE score=5 → invertida a 0.0")
        void negativeDirectionMin() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA))
                    .thenReturn(List.of(question("ICAF_CONF_007", "NEGATIVE")));
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_007"))
                    .thenReturn(Optional.empty());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of(answer("ICAF_CONF_007", 5)));
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(1L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of("ICAF_CONF_007", 5), CONFIANZA, null);

            // NEGATIVE: (5-5)/4*100 = 0
            assertThat(result.score()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("múltiples respuestas mixtas → score promedio correcto")
        void multipleAnswersMixedScore() {
            // POSITIVE score=5 → 100.0 ; POSITIVE score=1 → 0.0 → promedio = 50.0
            Question q1 = question("ICAF_CONF_001", "POSITIVE");
            Question q2 = question("ICAF_CONF_002", "POSITIVE");

            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA)).thenReturn(List.of(q1, q2));
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_001"))
                    .thenReturn(Optional.empty());
            when(answerRepo.findByFamilyIdAndQuestionKey(FAM_ID, "ICAF_CONF_002"))
                    .thenReturn(Optional.empty());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of(answer("ICAF_CONF_001", 5), answer("ICAF_CONF_002", 1)));
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(2L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID,
                            Map.of("ICAF_CONF_001", 5, "ICAF_CONF_002", 1),
                            CONFIANZA, null);

            assertThat(result.savedCount()).isEqualTo(2);
            assertThat(result.totalAnswered()).isEqualTo(2L);
            assertThat(result.score()).isCloseTo(50.0, within(0.01));
        }

        @Test
        @DisplayName("sin respuestas guardadas → score = 0.0")
        void emptyAnswers() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", CONFIANZA)).thenReturn(List.of());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(List.of());
            when(answerRepo.countAnsweredByDomain(FAM_ID, CONFIANZA)).thenReturn(0L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of(), CONFIANZA, null);

            assertThat(result.score()).isEqualTo(0.0);
            assertThat(result.savedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("DomainScoreResult contiene el dominio correcto")
        void resultContainsDomain() {
            when(questionRepo.findByQuestionTypeAndIcafDomainAndActiveTrueOrderBySortOrder(
                    "ICAF", BIENESTAR)).thenReturn(List.of());
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, BIENESTAR))
                    .thenReturn(List.of());
            when(answerRepo.countAnsweredByDomain(FAM_ID, BIENESTAR)).thenReturn(0L);

            IcafQuestionnaireService.DomainScoreResult result =
                    service.saveAnswers(FAM_ID, Map.of(), BIENESTAR, null);

            assertThat(result.domain()).isEqualTo(BIENESTAR);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    @Nested @DisplayName("getCurrentAnswers()")
    class GetCurrentAnswers {

        @Test
        @DisplayName("delega al repositorio")
        void delegatesToRepo() {
            List<FamilyIcafAnswer> expected = List.of(answer("ICAF_CONF_001", 4));
            when(answerRepo.findByFamilyIdAndIcafDomain(FAM_ID, CONFIANZA))
                    .thenReturn(expected);

            assertThat(service.getCurrentAnswers(FAM_ID, CONFIANZA)).isEqualTo(expected);
        }
    }
}
