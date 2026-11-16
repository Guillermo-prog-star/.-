package com.integrityfamily.biography.service;

import com.integrityfamily.biography.domain.BiographyAnswer;
import com.integrityfamily.biography.domain.BiographyQuestion;
import com.integrityfamily.biography.dto.BiographyDtos.AnswerRequest;
import com.integrityfamily.biography.dto.BiographyDtos.AnswerResponse;
import com.integrityfamily.biography.dto.BiographyDtos.ChapterResponse;
import com.integrityfamily.biography.repository.BiographyAnswerRepository;
import com.integrityfamily.biography.repository.BiographyQuestionRepository;
import com.integrityfamily.auth.service.AuditService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.AuditEventType;
import com.integrityfamily.domain.EntryVisibility;
import com.integrityfamily.domain.EpistemicSource;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BiographyService — Unit Tests")
class BiographyServiceTest {

    @Mock BiographyQuestionRepository questionRepository;
    @Mock BiographyAnswerRepository answerRepository;
    @Mock MemberRepository memberRepository;
    @Mock AuditService auditService;

    @InjectMocks
    BiographyService biographyService;

    private Family family;
    private FamilyMember author;
    private BiographyQuestion question;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia López").build();
        author = FamilyMember.builder().id(10L).family(family).email("autor@familia.test").build();
        question = BiographyQuestion.builder()
                .id(100L).chapterNumber(1).chapterTitle("Los comienzos")
                .questionKey("CH01_Q01_NACIMIENTO").promptText("¿Qué sabes de tu nacimiento?")
                .sortOrder(1).build();
    }

    @Nested
    @DisplayName("getCatalog()")
    class GetCatalog {

        @Test
        @DisplayName("agrupa las preguntas por capítulo, en orden")
        void shouldGroupByChapter() {
            BiographyQuestion q2 = BiographyQuestion.builder()
                    .id(101L).chapterNumber(1).chapterTitle("Los comienzos")
                    .questionKey("CH01_Q02").promptText("¿Qué te contaron?").sortOrder(2).build();
            BiographyQuestion q3 = BiographyQuestion.builder()
                    .id(200L).chapterNumber(2).chapterTitle("Infancia")
                    .questionKey("CH02_Q01").promptText("Un día cualquiera").sortOrder(1).build();
            when(questionRepository.findAllByOrderByChapterNumberAscSortOrderAsc())
                    .thenReturn(List.of(question, q2, q3));

            List<ChapterResponse> chapters = biographyService.getCatalog();

            assertThat(chapters).hasSize(2);
            assertThat(chapters.get(0).chapterNumber()).isEqualTo(1);
            assertThat(chapters.get(0).questions()).hasSize(2);
            assertThat(chapters.get(1).chapterTitle()).isEqualTo("Infancia");
        }
    }

    @Nested
    @DisplayName("upsertAnswer()")
    class UpsertAnswer {

        @Test
        @DisplayName("miembro inexistente → BusinessException NOT_FOUND")
        void shouldThrow_whenMemberNotFound() {
            when(memberRepository.findById(10L)).thenReturn(Optional.empty());
            AnswerRequest req = new AnswerRequest("texto", null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> biographyService.upsertAnswer(1L, 100L, req, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("miembro de otra familia → BusinessException FORBIDDEN")
        void shouldThrow_whenMemberBelongsToAnotherFamily() {
            FamilyMember other = FamilyMember.builder().id(10L).family(Family.builder().id(2L).build()).build();
            when(memberRepository.findById(10L)).thenReturn(Optional.of(other));
            AnswerRequest req = new AnswerRequest("texto", null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> biographyService.upsertAnswer(1L, 100L, req, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("sin autor resuelto (null) → BusinessException FORBIDDEN, sin tocar el repositorio")
        void shouldThrow_whenAuthorIdNull() {
            AnswerRequest req = new AnswerRequest("texto", null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> biographyService.upsertAnswer(1L, 100L, req, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));

            verifyNoInteractions(memberRepository, questionRepository, answerRepository);
        }

        @Test
        @DisplayName("primera respuesta → crea una nueva fila PRIVATE por defecto")
        void shouldCreate_whenNoExistingAnswer() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(author));
            when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
            when(answerRepository.findByMemberIdAndQuestionId(10L, 100L)).thenReturn(Optional.empty());
            when(answerRepository.save(any(BiographyAnswer.class))).thenAnswer(i -> i.getArgument(0));

            AnswerRequest req = new AnswerRequest("Nací en 1980", EpistemicSource.OBSERVED, "1980", null,
                    "Hoy lo valoro distinto", null, null, 80, null, null, null);

            AnswerResponse result = biographyService.upsertAnswer(1L, 100L, req, 10L);

            assertThat(result.answerText()).isEqualTo("Nací en 1980");
            assertThat(result.visibility()).isEqualTo(EntryVisibility.PRIVATE);
            assertThat(result.completedAt()).isNull();

            ArgumentCaptor<BiographyAnswer> captor = ArgumentCaptor.forClass(BiographyAnswer.class);
            verify(answerRepository).save(captor.capture());
            assertThat(captor.getValue().getMember()).isEqualTo(author);
            assertThat(captor.getValue().getFamily()).isEqualTo(family);
        }

        @Test
        @DisplayName("respuesta existente → actualiza en lugar de crear otra fila")
        void shouldUpdate_whenAnswerExists() {
            BiographyAnswer existing = BiographyAnswer.builder()
                    .id(500L).family(family).member(author).question(question)
                    .answerText("versión vieja").visibility(EntryVisibility.PRIVATE).build();
            when(memberRepository.findById(10L)).thenReturn(Optional.of(author));
            when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
            when(answerRepository.findByMemberIdAndQuestionId(10L, 100L)).thenReturn(Optional.of(existing));
            when(answerRepository.save(any(BiographyAnswer.class))).thenAnswer(i -> i.getArgument(0));

            AnswerRequest req = new AnswerRequest("versión corregida", null, null, null, null, null, null, null,
                    EntryVisibility.SHARED_WITH_FAMILY, true, null);

            AnswerResponse result = biographyService.upsertAnswer(1L, 100L, req, 10L);

            assertThat(result.answerText()).isEqualTo("versión corregida");
            assertThat(result.visibility()).isEqualTo(EntryVisibility.SHARED_WITH_FAMILY);
            assertThat(result.completedAt()).isNotNull();

            ArgumentCaptor<BiographyAnswer> captor = ArgumentCaptor.forClass(BiographyAnswer.class);
            verify(answerRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(500L);
            verify(auditService).registerSystemEvent(eq("autor@familia.test"), eq(AuditEventType.BIOGRAPHY_VISIBILITY_CHANGED), anyString());
        }

        @Test
        @DisplayName("visibility omitida en una respuesta compartida → se conserva SHARED_WITH_FAMILY, sin auditar")
        void shouldKeepVisibility_whenOmitted() {
            BiographyAnswer existing = BiographyAnswer.builder()
                    .id(500L).family(family).member(author).question(question)
                    .answerText("versión vieja").visibility(EntryVisibility.SHARED_WITH_FAMILY).build();
            when(memberRepository.findById(10L)).thenReturn(Optional.of(author));
            when(questionRepository.findById(100L)).thenReturn(Optional.of(question));
            when(answerRepository.findByMemberIdAndQuestionId(10L, 100L)).thenReturn(Optional.of(existing));
            when(answerRepository.save(any(BiographyAnswer.class))).thenAnswer(i -> i.getArgument(0));

            AnswerRequest req = new AnswerRequest("versión nueva", null, null, null, null, null, null, null, null, true, null);

            AnswerResponse result = biographyService.upsertAnswer(1L, 100L, req, 10L);

            assertThat(result.visibility()).isEqualTo(EntryVisibility.SHARED_WITH_FAMILY);
            verifyNoInteractions(auditService);
        }
    }

    @Nested
    @DisplayName("getAnswers()")
    class GetAnswers {

        @Test
        @DisplayName("delega en el repositorio con el filtro de visibilidad del viewer")
        void shouldDelegateToRepositoryWithViewerFilter() {
            when(memberRepository.findById(20L)).thenReturn(Optional.of(
                    FamilyMember.builder().id(20L).family(family).build()));
            BiographyAnswer a = BiographyAnswer.builder()
                    .id(1L).family(family).member(author).question(question)
                    .answerText("respuesta").visibility(EntryVisibility.SHARED_WITH_FAMILY).build();
            when(answerRepository.findVisibleToMember(1L, 20L, 30L)).thenReturn(List.of(a));

            List<AnswerResponse> result = biographyService.getAnswers(1L, 20L, 30L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).answerText()).isEqualTo("respuesta");
            verify(answerRepository).findVisibleToMember(1L, 20L, 30L);
        }

        @Test
        @DisplayName("miembro objetivo de otra familia → BusinessException FORBIDDEN")
        void shouldThrow_whenTargetMemberBelongsToAnotherFamily() {
            FamilyMember other = FamilyMember.builder().id(20L).family(Family.builder().id(2L).build()).build();
            when(memberRepository.findById(20L)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> biographyService.getAnswers(1L, 20L, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("deleteAnswer()")
    class DeleteAnswer {

        @Test
        @DisplayName("borra la respuesta propia cuando existe")
        void shouldDelete_whenAnswerExists() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(author));
            BiographyAnswer existing = BiographyAnswer.builder().id(500L).family(family).member(author).question(question).build();
            when(answerRepository.findByMemberIdAndQuestionId(10L, 100L)).thenReturn(Optional.of(existing));

            biographyService.deleteAnswer(1L, 100L, 10L);

            verify(answerRepository).delete(existing);
        }

        @Test
        @DisplayName("borrado → registra BIOGRAPHY_ANSWER_DELETED")
        void shouldAudit_whenDeleted() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(author));
            BiographyAnswer existing = BiographyAnswer.builder().id(500L).family(family).member(author).question(question).build();
            when(answerRepository.findByMemberIdAndQuestionId(10L, 100L)).thenReturn(Optional.of(existing));

            biographyService.deleteAnswer(1L, 100L, 10L);

            verify(auditService).registerSystemEvent(eq("autor@familia.test"), eq(AuditEventType.BIOGRAPHY_ANSWER_DELETED), anyString());
        }

        @Test
        @DisplayName("sin respuesta previa → BusinessException NOT_FOUND, no llama delete ni audita")
        void shouldThrowNotFound_whenNoAnswerExists() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(author));
            when(answerRepository.findByMemberIdAndQuestionId(10L, 100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> biographyService.deleteAnswer(1L, 100L, 10L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

            verify(answerRepository, never()).delete(any());
            verifyNoInteractions(auditService);
        }
    }

    @Nested
    @DisplayName("getPerspectives()")
    class GetPerspectives {

        @Test
        @DisplayName("delega en la query con el mismo filtro de visibilidad (sin bypass para viewer null)")
        void shouldDelegateToVisibilityQuery() {
            BiographyAnswer a = BiographyAnswer.builder()
                    .id(1L).family(family).member(author).question(question)
                    .answerText("mi versión").visibility(EntryVisibility.SHARED_WITH_FAMILY).build();
            when(answerRepository.findPerspectivesVisibleToMember(1L, "boda", null)).thenReturn(List.of(a));

            List<AnswerResponse> result = biographyService.getPerspectives(1L, "boda", null);

            assertThat(result).extracting(AnswerResponse::answerText).containsExactly("mi versión");
        }
    }
}
