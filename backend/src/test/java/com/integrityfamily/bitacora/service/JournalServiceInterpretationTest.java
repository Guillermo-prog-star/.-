package com.integrityfamily.bitacora.service;

import com.integrityfamily.bitacora.dto.JournalDtos.JournalCreateRequest;
import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JournalService — separar lo ocurrido de lo interpretado (ADR-018, V115)")
class JournalServiceInterpretationTest {

    @Mock PlanTaskRepository planTaskRepository;
    @Mock FamilyRepository familyRepository;
    @Mock TaskEvidenceRepository taskEvidenceRepository;
    @Mock ReflectionRepository reflectionRepository;
    @Mock LearningEntryRepository learningEntryRepository;
    @Mock JournalEntryRepository journalEntryRepository;
    @Mock MemberRepository memberRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks JournalService journalService;

    private Family family;
    private FamilyMember author;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia de prueba").build();
        author = new FamilyMember();
        author.setId(5L);
    }

    private JournalCreateRequest request(Map<JournalStep, ExplicitResponse> explicit,
                                         EntryVisibility visibility, String eventKey) {
        return new JournalCreateRequest(
                1L, "TASK", "COMUNICACION", "frustración", null, null, null,
                "La cocina sin recoger",
                "Le preguntaré qué impidió cumplir el acuerdo", null,
                "A las 8:00 p. m. la cocina seguía sin recoger; habíamos acordado las 7:30",
                "Me está desafiando",
                "No sé si tuvo algún imprevisto",
                "Tensión en la mandíbula",
                explicit, visibility, eventKey);
    }

    private void stubSave() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(memberRepository.findById(5L)).thenReturn(Optional.of(author));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Guarda hecho, interpretación, explicación alternativa y señal corporal por separado")
    void persistsSeparatedFields() {
        stubSave();
        Map<JournalStep, ExplicitResponse> explicit = new EnumMap<>(JournalStep.class);
        explicit.put(JournalStep.LEARNING, ExplicitResponse.NOT_HELPFUL);
        explicit.put(JournalStep.BODY_SIGNAL, ExplicitResponse.NONE_NOTICED);

        JournalEntry saved = journalService.createJournal(request(explicit, null, " evt-1 "), 5L);

        assertThat(saved.getObservations()).startsWith("A las 8:00");
        assertThat(saved.getInterpretation()).isEqualTo("Me está desafiando");
        assertThat(saved.getAlternativeExplanation()).isEqualTo("No sé si tuvo algún imprevisto");
        assertThat(saved.getBodySignal()).isEqualTo("Tensión en la mandíbula");
        assertThat(saved.getMember()).isSameAs(author);
        assertThat(saved.getVisibility()).isEqualTo(EntryVisibility.PRIVATE);
        assertThat(saved.getPerceivedEventKey()).isEqualTo("evt-1");
        assertThat(JournalService.readExplicitResponses(saved))
                .containsEntry(JournalStep.LEARNING, ExplicitResponse.NOT_HELPFUL)
                .containsEntry(JournalStep.BODY_SIGNAL, ExplicitResponse.NONE_NOTICED)
                .hasSize(2);
    }

    @Test
    @DisplayName("Sin respuestas explícitas -> columna NULL (no respondido), no un mapa vacío")
    void emptyExplicitResponses_areNull() {
        stubSave();

        JournalEntry saved = journalService.createJournal(request(Map.of(), EntryVisibility.SHARED_WITH_FAMILY, null), 5L);

        assertThat(saved.getExplicitResponses()).isNull();
        assertThat(JournalService.readExplicitResponses(saved)).isEmpty();
        assertThat(saved.getVisibility()).isEqualTo(EntryVisibility.SHARED_WITH_FAMILY);
    }

    @Test
    @DisplayName("NOT_HELPFUL fuera de comprobar/revisar -> 400 y no se guarda nada")
    void notHelpful_onlyOnReviewSteps() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(memberRepository.findById(5L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> journalService.createJournal(
                request(Map.of(JournalStep.INTERPRETATION, ExplicitResponse.NOT_HELPFUL), null, null), 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INTERPRETATION");
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("perceivedEventKey de más de 64 caracteres -> 400")
    void perceivedEventKey_tooLong() {
        when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        when(memberRepository.findById(5L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> journalService.createJournal(request(null, null, "x".repeat(65)), 5L))
                .isInstanceOf(BusinessException.class);
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("JSON ilegible en explicit_responses -> mapa vacío, sin excepción")
    void unreadableJson_isEmpty() {
        JournalEntry entry = JournalEntry.builder().id(9L).explicitResponses("{no-json").build();
        assertThat(JournalService.readExplicitResponses(entry)).isEmpty();
    }
}
