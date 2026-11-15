package com.integrityfamily.ai.messaging;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.common.event.SystemEvent;
import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.PlanTask;
import com.integrityfamily.domain.TaskEvidence;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvidenceAnalysisConsumer — la IA comenta, no califica")
class EvidenceAnalysisConsumerTest {

    @Mock TaskEvidenceRepository taskEvidenceRepository;
    @Mock AiProvider aiProvider;
    @Mock UserNotificationService userNotificationService;
    @InjectMocks EvidenceAnalysisConsumer consumer;

    private Family family;
    private PlanTask task;
    private TaskEvidence evidence;

    @BeforeEach
    void setUp() {
        family = new Family();
        task = PlanTask.builder().id(3L).title("Escuchar y comprobar").accionConcreta("Resumir lo entendido").build();
        evidence = TaskEvidence.builder().id(7L).task(task).family(family)
                .title("Cena").description("Lo intentamos, salió a medias")
                .latitude(4.6).longitude(-74.1).build();
    }

    private SystemEvent eventFor(Object payload) {
        return SystemEvent.of("evidence.submitted", 1L, payload, "x@test");
    }

    @Test
    @DisplayName("Envía el comentario como notificación sin puntaje ni validación, y sin tocar la evidencia")
    void shouldNotifyFeedback_withoutScoringOrValidating() {
        when(taskEvidenceRepository.findById(7L)).thenReturn(Optional.of(evidence));
        when(aiProvider.generateRawResponse(anyString()))
                .thenReturn("```json\n{\"feedback\": \"Contaron que salió a medias. ¿Qué lo dificultó?\"}\n```");

        consumer.onEvidenceSubmitted(eventFor(7L));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(userNotificationService).push(eq(family), isNull(), eq("EVIDENCE_FEEDBACK"), anyString(), message.capture());
        assertThat(message.getValue()).contains("¿Qué lo dificultó?").doesNotContain("pts").doesNotContain("/100");
        assertThat(evidence.getAiScore()).isNull();
        assertThat(evidence.getDescription()).isEqualTo("Lo intentamos, salió a medias");
        assertThat(task.isCompleted()).isFalse();
        verify(taskEvidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("El prompt prohíbe calificar y no envía la ubicación al proveedor de IA")
    void prompt_forbidsScoring_andOmitsLocation() {
        String prompt = EvidenceAnalysisConsumer.buildPrompt(task, evidence);

        assertThat(prompt).contains("No califiques").contains("no es una ejecución incorrecta");
        assertThat(prompt).doesNotContain("4.6").doesNotContain("-74.1").doesNotContain("\"score\"");
    }

    @Test
    @DisplayName("Si la IA falla, no propaga la excepción ni notifica")
    void shouldSwallowAiFailure() {
        when(taskEvidenceRepository.findById(7L)).thenReturn(Optional.of(evidence));
        when(aiProvider.generateRawResponse(anyString())).thenThrow(new RuntimeException("down"));

        assertThatCode(() -> consumer.onEvidenceSubmitted(eventFor("7"))).doesNotThrowAnyException();
        verifyNoInteractions(userNotificationService);
    }

    @Test
    @DisplayName("Respuesta sin feedback utilizable: no notifica")
    void shouldNotNotify_whenFeedbackMissing() {
        when(taskEvidenceRepository.findById(7L)).thenReturn(Optional.of(evidence));
        when(aiProvider.generateRawResponse(anyString())).thenReturn("no es json");

        consumer.onEvidenceSubmitted(eventFor(7L));

        verifyNoInteractions(userNotificationService);
    }
}
