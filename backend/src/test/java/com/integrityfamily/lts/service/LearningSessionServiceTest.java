package com.integrityfamily.lts.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.lts.domain.*;
import com.integrityfamily.lts.repository.*;
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
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link LearningSessionService}.
 *
 * Documenta el flujo completo del LTS (Learning Transformation System):
 *   createSession → addAttempt → defineError → formulateHypothesis → generateInsight
 *
 * No levanta contexto Spring — Mockito strict stubs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LearningSessionService — Unit Tests")
class LearningSessionServiceTest {

    @Mock SessionRepository     sessionRepository;
    @Mock AttemptRepository     attemptRepository;
    @Mock ErrorRepository       errorRepository;
    @Mock HypothesisRepository  hypothesisRepository;
    @Mock InsightRepository     insightRepository;
    @Mock FamilyRepository      familyRepository;
    @Mock MemberRepository      memberRepository;

    @InjectMocks LearningSessionService service;

    private Family       family;
    private FamilyMember member;
    private LearningSession session;

    @BeforeEach
    void setUp() {
        family  = Family.builder().id(1L).name("Los García").build();
        member  = FamilyMember.builder().id(10L).firstName("William").family(family).build();
        session = LearningSession.builder()
                .id(100L).family(family).member(member)
                .topic("Comunicación").objective("Mejorar la escucha activa")
                .status(LearningSession.SessionStatus.ACTIVE)
                .build();
    }

    // ───────────────────────────────────────────────────────────────────────
    //  createSession()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSession() — iniciar sesión de aprendizaje")
    class CreateSession {

        @Test
        @DisplayName("éxito: construye la sesión ACTIVE y la persiste")
        void createSession_success() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(sessionRepository.save(any())).thenReturn(session);

            LearningSession result = service.createSession(1L, 10L, "Comunicación", "Mejorar la escucha");

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(LearningSession.SessionStatus.ACTIVE);
            verify(sessionRepository).save(any(LearningSession.class));
        }

        @Test
        @DisplayName("los campos topic, objective, family y member se asignan a la entidad guardada")
        void createSession_fieldMapping() {
            ArgumentCaptor<LearningSession> captor = ArgumentCaptor.forClass(LearningSession.class);

            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(sessionRepository.save(captor.capture())).thenReturn(session);

            service.createSession(1L, 10L, "Hábitos", "Reducir pantallas");

            LearningSession captured = captor.getValue();
            assertThat(captured.getTopic()).isEqualTo("Hábitos");
            assertThat(captured.getObjective()).isEqualTo("Reducir pantallas");
            assertThat(captured.getFamily()).isSameAs(family);
            assertThat(captured.getMember()).isSameAs(member);
            assertThat(captured.getStatus()).isEqualTo(LearningSession.SessionStatus.ACTIVE);
        }

        @Test
        @DisplayName("familia no encontrada → BusinessException FAMILY_NOT_FOUND 404")
        void createSession_familyNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSession(99L, 10L, "T", "O"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("FAMILY_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("miembro no encontrado → BusinessException MEMBER_NOT_FOUND 404")
        void createSession_memberNotFound() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSession(1L, 99L, "T", "O"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("MEMBER_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  addAttempt()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addAttempt() — registrar intento en sesión")
    class AddAttempt {

        @Test
        @DisplayName("primer intento: version = 1 (sin intentos previos)")
        void addAttempt_firstAttempt_version1() {
            when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
            when(attemptRepository.findBySessionIdOrderByVersionAsc(100L)).thenReturn(List.of());

            ArgumentCaptor<Attempt> captor = ArgumentCaptor.forClass(Attempt.class);
            when(attemptRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.addAttempt(100L, "Mi primer intento");

            assertThat(captor.getValue().getVersion()).isEqualTo(1);
            assertThat(captor.getValue().getContent()).isEqualTo("Mi primer intento");
            assertThat(captor.getValue().getSession()).isSameAs(session);
        }

        @Test
        @DisplayName("tercer intento: version = 3 (2 intentos previos)")
        void addAttempt_thirdAttempt_version3() {
            Attempt a1 = Attempt.builder().id(1L).session(session).version(1).content("v1").build();
            Attempt a2 = Attempt.builder().id(2L).session(session).version(2).content("v2").build();

            when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
            when(attemptRepository.findBySessionIdOrderByVersionAsc(100L)).thenReturn(List.of(a1, a2));

            ArgumentCaptor<Attempt> captor = ArgumentCaptor.forClass(Attempt.class);
            when(attemptRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.addAttempt(100L, "Mi tercer intento");

            assertThat(captor.getValue().getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("sesión no encontrada → BusinessException SESSION_NOT_FOUND 404")
        void addAttempt_sessionNotFound() {
            when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addAttempt(999L, "contenido"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("SESSION_NOT_FOUND"));
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  defineError()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("defineError() — clasificar el error del intento")
    class DefineError {

        @Test
        @DisplayName("éxito: construye LearningError con tipo y descripción")
        void defineError_success() {
            Attempt attempt = Attempt.builder().id(1L).session(session).version(1).content("v1").build();

            when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));
            when(errorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LearningError result = service.defineError(1L, "CONCEPTUAL", "Confundió el concepto clave");

            assertThat(result.getAttempt()).isSameAs(attempt);
            assertThat(result.getErrorType()).isEqualTo("CONCEPTUAL");
            assertThat(result.getDescription()).isEqualTo("Confundió el concepto clave");
        }

        @Test
        @DisplayName("intento no encontrado → BusinessException ATTEMPT_NOT_FOUND 404")
        void defineError_attemptNotFound() {
            when(attemptRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.defineError(99L, "LOGICAL", "descripción"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ATTEMPT_NOT_FOUND"));
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  formulateHypothesis()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("formulateHypothesis() — registrar hipótesis de corrección")
    class FormulateHypothesis {

        @Test
        @DisplayName("éxito: construye Hypothesis vinculada al error")
        void formulateHypothesis_success() {
            Attempt attempt = Attempt.builder().id(1L).session(session).version(1).build();
            LearningError error = LearningError.builder().id(5L).attempt(attempt).errorType("LOGICAL").description("desc").build();

            when(errorRepository.findById(5L)).thenReturn(Optional.of(error));
            when(hypothesisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Hypothesis result = service.formulateHypothesis(5L, "Si practicamos la empatía mejoramos");

            assertThat(result.getError()).isSameAs(error);
            assertThat(result.getContent()).isEqualTo("Si practicamos la empatía mejoramos");
        }

        @Test
        @DisplayName("error no encontrado → BusinessException ERROR_NOT_FOUND 404")
        void formulateHypothesis_errorNotFound() {
            when(errorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.formulateHypothesis(99L, "hipótesis"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("ERROR_NOT_FOUND"));
        }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  generateInsight()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateInsight() — consolidar aprendizaje y cerrar sesión")
    class GenerateInsight {

        @Test
        @DisplayName("éxito: crea el Insight, marca sesión COMPLETED y persiste ambos")
        void generateInsight_success() {
            when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);
            when(insightRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Insight result = service.generateInsight(100L, "Aprendí a escuchar", "Lo aplicaré en cenas familiares");

            assertThat(result.getWhatLearned()).isEqualTo("Aprendí a escuchar");
            assertThat(result.getTransfer()).isEqualTo("Lo aplicaré en cenas familiares");
            assertThat(result.getSession()).isSameAs(session);
            assertThat(session.getStatus()).isEqualTo(LearningSession.SessionStatus.COMPLETED);
            verify(sessionRepository).save(session);  // session marcada COMPLETED
            verify(insightRepository).save(any(Insight.class));
        }

        @Test
        @DisplayName("sesión no encontrada → BusinessException SESSION_NOT_FOUND 404")
        void generateInsight_sessionNotFound() {
            when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generateInsight(999L, "aprendizaje", "transferencia"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("SESSION_NOT_FOUND"));

            verify(insightRepository, never()).save(any());
        }
    }
}
