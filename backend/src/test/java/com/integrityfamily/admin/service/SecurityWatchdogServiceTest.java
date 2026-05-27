package com.integrityfamily.admin.service;

import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Feedback;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityWatchdogService")
class SecurityWatchdogServiceTest {

    @Mock FamilyRepository   familyRepository;
    @Mock FeedbackRepository feedbackRepository;
    @Mock AdminAlertRepository alertRepository;

    @InjectMocks SecurityWatchdogService watchdog;

    // ── sin datos activos ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("sin datos activos")
    class NoData {

        @BeforeEach
        void stub() {
            when(familyRepository.findBySentinelActiveTrue()).thenReturn(List.of());
            when(feedbackRepository.findByScoreLessThanEqualAndCreatedAtAfter(eq(1), any(LocalDateTime.class)))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("no guarda ninguna alerta si no hay crisis ni feedback critico")
        void noActiveCrises_noNegativeFeedback_savesNothing() {
            watchdog.scanForAnomalies();
            verify(alertRepository, never()).save(any());
        }
    }

    // ── Crisis Sentinel ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("crisis Sentinel activa")
    class ActiveCrisis {

        @BeforeEach
        void stub() {
            Family family = Family.builder().id(1L).familyCode("ALFA-007").currentMilestone("W1").sentinelActive(true).build();
            when(familyRepository.findBySentinelActiveTrue()).thenReturn(List.of(family));
            when(feedbackRepository.findByScoreLessThanEqualAndCreatedAtAfter(eq(1), any(LocalDateTime.class)))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("guarda alerta CRITICAL cuando no existe previamente")
        void newAlert_savesCriticalAlert() {
            // title = "CRISIS ACTIVA: ALFA-007", message = "Protocolo Sentinel detectado en el hito W1"
            when(alertRepository.findByTitleAndMessage(anyString(), anyString())).thenReturn(Optional.empty());

            watchdog.scanForAnomalies();

            ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
            verify(alertRepository).save(captor.capture());
            AdminAlert saved = captor.getValue();
            assertThat(saved.getTitle()).contains("ALFA-007");
            assertThat(saved.getMessage()).contains("W1");
            assertThat(saved.getSeverity()).isEqualTo("CRITICAL");
        }

        @Test
        @DisplayName("no duplica alerta si ya existe")
        void existingAlert_skipsCreation() {
            when(alertRepository.findByTitleAndMessage(anyString(), anyString()))
                    .thenReturn(Optional.of(AdminAlert.builder().id(5L).title("already").message("exists").build()));

            watchdog.scanForAnomalies();

            verify(alertRepository, never()).save(any());
        }
    }

    // ── Feedback altamente negativo ───────────────────────────────────────────

    @Nested
    @DisplayName("feedback altamente negativo")
    class CriticalFeedback {

        private static final String COMMENT = "Muy mala experiencia";

        @BeforeEach
        void stub() {
            Family fam = Family.builder().id(2L).familyCode("ALFA-001").build();
            Feedback fb = Feedback.builder().id(10L).score(1).comment(COMMENT).family(fam).build();
            when(familyRepository.findBySentinelActiveTrue()).thenReturn(List.of());
            when(feedbackRepository.findByScoreLessThanEqualAndCreatedAtAfter(eq(1), any(LocalDateTime.class)))
                    .thenReturn(List.of(fb));
        }

        @Test
        @DisplayName("guarda alerta WARNING cuando no existe previamente")
        void newFeedback_savesWarningAlert() {
            // El servicio llama findByTitleAndMessage(fbTitle, fb.getComment())
            // es decir, el segundo argumento es el comentario crudo, no el mensaje formateado.
            when(alertRepository.findByTitleAndMessage(anyString(), eq(COMMENT))).thenReturn(Optional.empty());

            watchdog.scanForAnomalies();

            ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
            verify(alertRepository).save(captor.capture());
            AdminAlert saved = captor.getValue();
            assertThat(saved.getTitle()).contains("ALFA-001");
            assertThat(saved.getMessage()).contains(COMMENT);
            assertThat(saved.getSeverity()).isEqualTo("WARNING");
        }

        @Test
        @DisplayName("no duplica alerta WARNING si ya existe")
        void existingFeedbackAlert_skipsCreation() {
            when(alertRepository.findByTitleAndMessage(anyString(), eq(COMMENT)))
                    .thenReturn(Optional.of(AdminAlert.builder().id(9L).title("exists").message(COMMENT).build()));

            watchdog.scanForAnomalies();

            verify(alertRepository, never()).save(any());
        }
    }
}
