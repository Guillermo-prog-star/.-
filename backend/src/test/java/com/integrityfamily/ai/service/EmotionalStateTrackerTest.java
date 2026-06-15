package com.integrityfamily.ai.service;

import com.integrityfamily.domain.ConversationSession;
import com.integrityfamily.domain.repository.ChatMessageRepository;
import com.integrityfamily.domain.repository.ConversationSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmotionalStateTracker")
class EmotionalStateTrackerTest {

    @Mock ChatMessageRepository        chatMessageRepository;
    @Mock ConversationSessionRepository sessionRepository;
    @InjectMocks EmotionalStateTracker tracker;

    private static final long SESSION_ID = 1L;

    private void stubSnapshots(List<String> snapshots) {
        when(chatMessageRepository.findRecentUserSnapshotsForSession(eq(SESSION_ID), any(PageRequest.class)))
                .thenReturn(snapshots);
        lenient().when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("sessionId=null → 'STABLE' sin llamar repos")
        void nullSessionId_returnsStable() {
            String result = tracker.computeAndUpdateArc(null);

            assertThat(result).isEqualTo("STABLE");
            verifyNoInteractions(chatMessageRepository, sessionRepository);
        }

        @Test
        @DisplayName("repo lanza excepción → falla silenciosamente, retorna 'STABLE'")
        void repoThrows_silentFailure() {
            when(chatMessageRepository.findRecentUserSnapshotsForSession(eq(SESSION_ID), any()))
                    .thenThrow(new RuntimeException("DB error"));

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("STABLE");
        }
    }

    // ── computeArc ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("computeArc")
    class ComputeArc {

        @Test
        @DisplayName("sin snapshots → 'STABLE'")
        void emptySnapshots_stable() {
            stubSnapshots(List.of());

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("STABLE");
        }

        @Test
        @DisplayName("≥3 negativos → 'ESCALATED'")
        void threeNegatives_escalated() {
            stubSnapshots(List.of("ANXIOUS", "FRUSTRATED", "CONFUSED"));

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("ESCALATED");
        }

        @Test
        @DisplayName("últimos 2 negativos (< 3 total) → 'ESCALATING'")
        void lastTwoNegative_escalating() {
            // desc: índice 0 = más reciente
            stubSnapshots(List.of("ANXIOUS", "FRUSTRATED"));

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("ESCALATING");
        }

        @Test
        @DisplayName("último positivo, anterior negativo → 'DE_ESCALATING'")
        void lastPositivePrevNegative_deEscalating() {
            stubSnapshots(List.of("CONTENT", "ANXIOUS"));

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("DE_ESCALATING");
        }

        @Test
        @DisplayName("exactamente 1 negativo → 'MILD_TENSION'")
        void oneNegative_mildTension() {
            stubSnapshots(List.of("CONTENT", "CONTENT", "ANXIOUS"));

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("MILD_TENSION");
        }

        @Test
        @DisplayName("todos positivos → 'STABLE'")
        void allPositive_stable() {
            stubSnapshots(List.of("CONTENT", "HAPPY", "ENGAGED"));

            assertThat(tracker.computeAndUpdateArc(SESSION_ID)).isEqualTo("STABLE");
        }
    }

    // ── Persistencia ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("persistencia del arco")
    class Persistence {

        @Test
        @DisplayName("sesión encontrada → emotionalState actualizado y guardado")
        void sessionFound_arcSaved() {
            ConversationSession session = ConversationSession.builder().id(SESSION_ID).build();
            when(chatMessageRepository.findRecentUserSnapshotsForSession(eq(SESSION_ID), any()))
                    .thenReturn(List.of("ANXIOUS", "ANXIOUS", "ANXIOUS"));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(sessionRepository.save(any())).thenReturn(session);

            String result = tracker.computeAndUpdateArc(SESSION_ID);

            assertThat(result).isEqualTo("ESCALATED");
            assertThat(session.getEmotionalState()).isEqualTo("ESCALATED");
            verify(sessionRepository).save(session);
        }

        @Test
        @DisplayName("sesión no encontrada → arco calculado pero no persiste")
        void sessionNotFound_noSave() {
            when(chatMessageRepository.findRecentUserSnapshotsForSession(eq(SESSION_ID), any()))
                    .thenReturn(List.of("CONTENT"));
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            String result = tracker.computeAndUpdateArc(SESSION_ID);

            assertThat(result).isEqualTo("STABLE");
            verify(sessionRepository, never()).save(any());
        }
    }
}
