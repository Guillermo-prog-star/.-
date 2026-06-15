package com.integrityfamily.ai.service;

import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConversationSessionService")
class ConversationSessionServiceTest {

    @Mock ConversationSessionRepository sessionRepository;
    @InjectMocks ConversationSessionService service;

    private static final long FAM_ID = 1L;
    private static final long MEM_ID = 10L;
    private static final long SES_ID = 99L;

    private ConversationSession activeSession() {
        return ConversationSession.builder()
                .id(SES_ID).familyId(FAM_ID).memberId(MEM_ID).goal("GENERAL").build();
    }

    private ConversationSession staleSession() {
        return ConversationSession.builder()
                .id(77L).familyId(FAM_ID).memberId(MEM_ID).goal("GENERAL")
                .startedAt(LocalDateTime.now().minusHours(6)).build();
    }

    // ── findOrCreateSession ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findOrCreateSession")
    class FindOrCreateSession {

        @Test
        @DisplayName("sesión activa encontrada → retornada directamente sin crear nueva")
        void activeFound_returnedDirectly() {
            ConversationSession s = activeSession();
            when(sessionRepository.findActiveSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(s));

            ConversationSession result = service.findOrCreateSession(FAM_ID, MEM_ID, "SUPPORT");

            assertThat(result).isSameAs(s);
            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("sin sesión activa ni stale → crea y guarda sesión nueva con goal")
        void noActiveNoStale_createsNewSession() {
            when(sessionRepository.findActiveSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of());
            when(sessionRepository.findOpenStaleSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class)))
                    .thenReturn(List.of());
            when(sessionRepository.save(any())).thenAnswer(inv -> {
                ConversationSession s = inv.getArgument(0);
                s = ConversationSession.builder().id(100L)
                        .familyId(s.getFamilyId()).memberId(s.getMemberId())
                        .goal(s.getGoal()).build();
                return s;
            });

            ConversationSession result = service.findOrCreateSession(FAM_ID, MEM_ID, "PLANNING");

            assertThat(result.getFamilyId()).isEqualTo(FAM_ID);
            assertThat(result.getMemberId()).isEqualTo(MEM_ID);
            assertThat(result.getGoal()).isEqualTo("PLANNING");
        }

        @Test
        @DisplayName("sesión stale encontrada → cerrada (ABANDONED) antes de crear la nueva")
        void staleExists_closedBeforeCreatingNew() {
            ConversationSession stale = staleSession();
            when(sessionRepository.findActiveSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of());
            when(sessionRepository.findOpenStaleSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class)))
                    .thenReturn(List.of(stale));
            when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.findOrCreateSession(FAM_ID, MEM_ID, "GENERAL");

            assertThat(stale.getOutcome()).isEqualTo("ABANDONED");
            assertThat(stale.getEndedAt()).isNotNull();
            verify(sessionRepository, times(2)).save(any()); // 1 stale + 1 nueva
        }
    }

    // ── updateEmotionalState ─────────────────────────────────────────────────

    @Nested
    @DisplayName("updateEmotionalState")
    class UpdateEmotionalState {

        @Test
        @DisplayName("sesión no encontrada → no hace nada (ifPresent)")
        void sessionNotFound_noOp() {
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.empty());

            service.updateEmotionalState(SES_ID, "ANXIOUS");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("sesión encontrada → emotionalState actualizado y guardado")
        void sessionFound_updatesAndSaves() {
            ConversationSession s = activeSession();
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.of(s));
            when(sessionRepository.save(s)).thenReturn(s);

            service.updateEmotionalState(SES_ID, "HOPEFUL");

            assertThat(s.getEmotionalState()).isEqualTo("HOPEFUL");
            verify(sessionRepository).save(s);
        }
    }

    // ── incrementTurnCount ───────────────────────────────────────────────────

    @Nested
    @DisplayName("incrementTurnCount")
    class IncrementTurnCount {

        @Test
        @DisplayName("sesión no encontrada → retorna 0")
        void sessionNotFound_returnsZero() {
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.empty());

            int result = service.incrementTurnCount(SES_ID);

            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("turnCount=2 → incrementa a 3, guarda y retorna 3")
        void turnCount_incrementedAndReturned() {
            ConversationSession s = ConversationSession.builder()
                    .id(SES_ID).familyId(FAM_ID).memberId(MEM_ID).turnCount(2).build();
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.of(s));
            when(sessionRepository.save(s)).thenReturn(s);

            int result = service.incrementTurnCount(SES_ID);

            assertThat(result).isEqualTo(3);
            assertThat(s.getTurnCount()).isEqualTo(3);
        }
    }

    // ── getActiveSession ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getActiveSession")
    class GetActiveSession {

        @Test
        @DisplayName("familyId=null → retorna null sin consultar repo")
        void nullFamilyId_returnsNull() {
            assertThat(service.getActiveSession(null, MEM_ID)).isNull();
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("memberId=null → retorna null sin consultar repo")
        void nullMemberId_returnsNull() {
            assertThat(service.getActiveSession(FAM_ID, null)).isNull();
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("sin sesión activa → retorna null")
        void noActiveSession_returnsNull() {
            when(sessionRepository.findActiveSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of());

            assertThat(service.getActiveSession(FAM_ID, MEM_ID)).isNull();
        }

        @Test
        @DisplayName("sesión activa encontrada → retornada")
        void activeFound_returned() {
            ConversationSession s = activeSession();
            when(sessionRepository.findActiveSessionsForMember(
                    eq(FAM_ID), eq(MEM_ID), any(LocalDateTime.class), any(Pageable.class)))
                    .thenReturn(List.of(s));

            assertThat(service.getActiveSession(FAM_ID, MEM_ID)).isSameAs(s);
        }
    }

    // ── closeSession ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("closeSession")
    class CloseSession {

        @Test
        @DisplayName("sesión no encontrada → no hace nada")
        void sessionNotFound_noOp() {
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.empty());

            service.closeSession(SES_ID, "COMPLETED");

            verify(sessionRepository, never()).save(any());
        }

        @Test
        @DisplayName("sesión activa → cerrada con outcome y endedAt asignados")
        void activeSession_closedWithOutcome() {
            ConversationSession s = activeSession(); // endedAt=null → isActive()=true
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.of(s));
            when(sessionRepository.save(s)).thenReturn(s);

            service.closeSession(SES_ID, "COMPLETED");

            assertThat(s.getOutcome()).isEqualTo("COMPLETED");
            assertThat(s.getEndedAt()).isNotNull();
        }

        @Test
        @DisplayName("sesión ya cerrada (isActive=false) → no se guarda de nuevo")
        void alreadyClosed_notSavedAgain() {
            ConversationSession s = ConversationSession.builder()
                    .id(SES_ID).familyId(FAM_ID).memberId(MEM_ID)
                    .endedAt(LocalDateTime.now().minusHours(1)).outcome("ABANDONED").build();
            when(sessionRepository.findById(SES_ID)).thenReturn(Optional.of(s));

            service.closeSession(SES_ID, "COMPLETED");

            verify(sessionRepository, never()).save(any());
        }
    }
}
