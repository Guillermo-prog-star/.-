package com.integrityfamily.evaluation.service;

import com.integrityfamily.assessment.domain.Question;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias de QuestionService.
 *
 * QuestionService mantiene una lista en memoria sin repositorio —
 * se prueba directamente sin Mockito.
 */
@DisplayName("QuestionService — Unit Tests")
class QuestionServiceTest {

    private QuestionService questionService;

    @BeforeEach
    void setUp() {
        // Nueva instancia por test — la lista interna es fresh
        questionService = new QuestionService();
    }

    // ═══════════════════════════════════════════════════════════════════
    //  findAll()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("Servicio recién creado → retorna lista vacía")
        void shouldReturnEmptyList_initially() {
            List<Question> result = questionService.findAll();
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Después de guardar 2 preguntas → findAll retorna las 2")
        void shouldReturnAllSavedQuestions() {
            questionService.save(new Question());
            questionService.save(new Question());

            assertThat(questionService.findAll()).hasSize(2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  save()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("save() retorna la misma instancia que se pasó")
        void shouldReturnSameInstance() {
            Question q = new Question();
            Question result = questionService.save(q);

            assertThat(result).isSameAs(q);
        }

        @Test
        @DisplayName("save() agrega la pregunta a la lista interna")
        void shouldAddToInternalList() {
            Question q = new Question();
            questionService.save(q);

            assertThat(questionService.findAll()).contains(q);
        }

        @Test
        @DisplayName("Múltiples saves acumulan preguntas (lista crece)")
        void shouldAccumulateOnMultipleSaves() {
            questionService.save(new Question());
            questionService.save(new Question());
            questionService.save(new Question());

            assertThat(questionService.findAll()).hasSize(3);
        }

        @Test
        @DisplayName("save() con null no lanza excepción y se almacena en la lista")
        void shouldAcceptNull_withoutException() {
            questionService.save(null);

            // La lista in-memory acepta null sin validación
            assertThat(questionService.findAll()).hasSize(1);
            assertThat(questionService.findAll().get(0)).isNull();
        }
    }
}
