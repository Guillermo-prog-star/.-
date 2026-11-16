package com.integrityfamily.sistemas.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.sistemas.domain.ReviewItemKind;
import com.integrityfamily.sistemas.domain.SystemReviewType;
import com.integrityfamily.sistemas.dto.SistemasDtos.AdjustmentInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.ObjectiveInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.ProjectInput;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveReviewRequest;
import com.integrityfamily.sistemas.service.SistemasReviewRules.ValidatedReview;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SistemasReviewRules — límites de la salida escrita")
class SistemasReviewRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Nested
    @DisplayName("diaria")
    class Daily {

        @Test
        @DisplayName("acepta captura vacía, agenda comprobada y de 1 a 3 acciones")
        void acceptsOneToThreeActions() {
            ValidatedReview one = SistemasReviewRules.validate(daily(true, true, List.of("Llamar a la escuela")), TODAY);
            ValidatedReview three = SistemasReviewRules.validate(
                    daily(true, true, List.of("Una", "  Dos  ", "", "Tres")), TODAY);

            assertThat(one.type()).isEqualTo(SystemReviewType.DAILY);
            assertThat(one.items()).hasSize(1);
            assertThat(one.items().get(0).kind()).isEqualTo(ReviewItemKind.ACTION);
            assertThat(three.items()).extracting(item -> item.text()).containsExactly("Una", "Dos", "Tres");
        }

        @Test
        @DisplayName("rechaza captura sin vaciar, agenda sin comprobar, cero acciones o cuatro")
        void rejectsOutsideTheMinimum() {
            assertThatThrownBy(() -> SistemasReviewRules.validate(daily(false, true, List.of("Una")), TODAY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La revisión diaria exige dejar la captura vacía.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(daily(true, false, List.of("Una")), TODAY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La revisión diaria exige comprobar la agenda de hoy.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(daily(true, true, List.of("  ")), TODAY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La revisión diaria deja de 1 a 3 acciones para hoy.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(daily(true, true, List.of("1", "2", "3", "4")), TODAY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La revisión diaria deja de 1 a 3 acciones para hoy.");
        }
    }

    @Nested
    @DisplayName("semanal")
    class Weekly {

        @Test
        @DisplayName("acepta como máximo tres prioridades y las tres casillas")
        void acceptsUpToThree() {
            ValidatedReview review = SistemasReviewRules.validate(
                    weekly(true, true, true, List.of("Prioridad 1", "Prioridad 2")), TODAY);

            assertThat(review.items()).hasSize(2);
            assertThat(review.calendarReviewed()).isTrue();
            assertThat(review.periodLabel()).isEqualTo("Semana del 2026-11-16");
        }

        @Test
        @DisplayName("rechaza más de tres prioridades o una casilla sin marcar")
        void rejectsOverTheMax() {
            assertThatThrownBy(() -> SistemasReviewRules.validate(
                    weekly(true, true, true, List.of("1", "2", "3", "4")), TODAY))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La revisión semanal deja como máximo 3 prioridades, y al menos una.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(weekly(false, true, true, List.of("1")), TODAY))
                    .hasMessage("La revisión semanal exige marcar el calendario como revisado.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(weekly(true, false, true, List.of("1")), TODAY))
                    .hasMessage("La revisión semanal exige depurar las tareas.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(weekly(true, true, false, List.of("1")), TODAY))
                    .hasMessage("La revisión semanal exige pasar los compromisos de correo o chat a tarea o evento.");
        }
    }

    @Nested
    @DisplayName("mensual")
    class Monthly {

        @Test
        @DisplayName("una decisión por proyecto, solo del conjunto permitido")
        void acceptsOneDecisionPerProject() {
            ValidatedReview review = SistemasReviewRules.validate(monthly(false, List.of(
                    new ProjectInput("Huerto", "CONTINUAR"),
                    new ProjectInput("  ", "  "),
                    new ProjectInput("Lectura", "cerrar")
            )), TODAY);

            assertThat(review.items()).extracting(item -> item.decision()).containsExactly("CONTINUAR", "CERRAR");
            assertThat(review.stageChangeUnnoticed()).isFalse();
            for (String decision : List.of("CORREGIR", "PAUSAR")) {
                ValidatedReview accepted = SistemasReviewRules.validate(
                        monthly(true, List.of(new ProjectInput("Asunto", decision))), TODAY);
                assertThat(accepted.items().get(0).decision()).isEqualTo(decision);
            }
        }

        @Test
        @DisplayName("rechaza decisión ajena, proyecto sin decisión y la pregunta trimestral en blanco")
        void rejectsInvalidDecision() {
            assertThatThrownBy(() -> SistemasReviewRules.validate(monthly(null, List.of(new ProjectInput("Huerto", "CONTINUAR"))), TODAY))
                    .hasMessage("Responde si algún cambio de etapa pasó desapercibido.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(monthly(true, List.of(new ProjectInput("Huerto", "ARCHIVAR"))), TODAY))
                    .hasMessage("La decisión del proyecto no es válida. Usa continuar, corregir, pausar o cerrar.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(monthly(true, List.of(new ProjectInput("Huerto", null))), TODAY))
                    .hasMessage("Cada proyecto necesita una decisión: continuar, corregir, pausar o cerrar.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(monthly(true, List.of()), TODAY))
                    .hasMessage("La revisión mensual exige al menos un proyecto.");
        }
    }

    @Nested
    @DisplayName("anual")
    class Annual {

        @Test
        @DisplayName("cada objetivo trae criterio de logro y fecha de revisión, y no pasan de cinco")
        void requiresCriterionAndDate() {
            ValidatedReview review = SistemasReviewRules.validate(annual(List.of(
                    new ObjectiveInput("Cenar juntos entre semana", "Tres cenas sin pantallas", LocalDate.of(2027, 3, 1))
            )), TODAY);

            assertThat(review.items()).hasSize(1);
            assertThat(review.items().get(0).achievementCriterion()).isEqualTo("Tres cenas sin pantallas");
            assertThat(review.items().get(0).reviewOn()).isEqualTo(LocalDate.of(2027, 3, 1));

            assertThatThrownBy(() -> SistemasReviewRules.validate(annual(List.of(
                    new ObjectiveInput("Solo el deseo", null, LocalDate.of(2027, 3, 1)))), TODAY))
                    .hasMessage("Cada objetivo necesita su texto, su criterio de logro y la fecha en que se revisa.");

            assertThatThrownBy(() -> SistemasReviewRules.validate(annual(List.of(
                    new ObjectiveInput("Uno", "c", LocalDate.of(2027, 1, 1)),
                    new ObjectiveInput("Dos", "c", LocalDate.of(2027, 1, 1)),
                    new ObjectiveInput("Tres", "c", LocalDate.of(2027, 1, 1)),
                    new ObjectiveInput("Cuatro", "c", LocalDate.of(2027, 1, 1)),
                    new ObjectiveInput("Cinco", "c", LocalDate.of(2027, 1, 1)),
                    new ObjectiveInput("Seis", "c", LocalDate.of(2027, 1, 1))
            )), TODAY)).hasMessage("La revisión anual guarda pocos objetivos: entre 1 y 5.");
        }
    }

    @Nested
    @DisplayName("ajuste por cambio")
    class Change {

        @Test
        @DisplayName("exige qué ya no cabe y al menos un dejar, reducir o posponer")
        void requiresBothAnswers() {
            ValidatedReview review = SistemasReviewRules.validate(change(
                    "El colegio nuevo",
                    "Los entrenamientos de los martes ya no caben.",
                    List.of(new AdjustmentInput("Entrenamiento de los martes", "POSPONER"),
                            new AdjustmentInput("  ", null))), TODAY);

            assertThat(review.changeDescription()).isEqualTo("El colegio nuevo");
            assertThat(review.whatNoLongerFits()).contains("martes");
            assertThat(review.items()).hasSize(1);
            assertThat(review.items().get(0).decision()).isEqualTo("POSPONER");
        }

        @Test
        @DisplayName("rechaza el ajuste sin texto o con un tipo que no es dejar, reducir o posponer")
        void rejectsIncomplete() {
            assertThatThrownBy(() -> SistemasReviewRules.validate(change(
                    "Mudanza", " ", List.of(new AdjustmentInput("El club", "DEJAR"))), TODAY))
                    .hasMessage("Escribe qué compromisos ya no caben.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(change(
                    "Mudanza", "El club", List.of()), TODAY))
                    .hasMessage("Indica al menos una cosa que dejas, reduces o pospones.");
            assertThatThrownBy(() -> SistemasReviewRules.validate(change(
                    "Mudanza", "El club", List.of(new AdjustmentInput("El club", "CANCELAR"))), TODAY))
                    .hasMessage("Cada ajuste debe decir qué cambias y si lo dejas, lo reduces o lo pospones.");
        }
    }

    @Test
    @DisplayName("no acepta una fecha futura ni un tipo desconocido")
    void rejectsFutureAndUnknownType() {
        assertThatThrownBy(() -> SistemasReviewRules.validate(dailyOn(TODAY.plusDays(1)), TODAY))
                .hasMessage("La revisión no puede quedar en una fecha futura.");
        assertThatThrownBy(() -> SistemasReviewRules.validate(
                new SaveReviewRequest("OTRA", TODAY, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                TODAY)).hasMessage("El tipo de revisión no es válido.");
    }

    private static SaveReviewRequest daily(Boolean capture, Boolean agenda, List<String> actions) {
        return dailyOn(TODAY, capture, agenda, actions);
    }

    private static SaveReviewRequest dailyOn(LocalDate date) {
        return dailyOn(date, true, true, List.of("Una"));
    }

    private static SaveReviewRequest dailyOn(LocalDate date, Boolean capture, Boolean agenda, List<String> actions) {
        return new SaveReviewRequest("DAILY", date, null, capture, agenda, actions,
                null, null, null, null, null, null, null, null, null, null);
    }

    private static SaveReviewRequest weekly(Boolean calendar, Boolean tasks, Boolean commitments, List<String> priorities) {
        return new SaveReviewRequest("WEEKLY", TODAY, null, null, null, null,
                calendar, tasks, commitments, priorities, null, null, null, null, null, null);
    }

    private static SaveReviewRequest monthly(Boolean unnoticed, List<ProjectInput> projects) {
        return new SaveReviewRequest("MONTHLY", TODAY, null, null, null, null,
                null, null, null, null, projects, unnoticed, null, null, null, null);
    }

    private static SaveReviewRequest annual(List<ObjectiveInput> objectives) {
        return new SaveReviewRequest("ANNUAL", TODAY, null, null, null, null,
                null, null, null, null, null, null, objectives, null, null, null);
    }

    private static SaveReviewRequest change(String description, String noLongerFits, List<AdjustmentInput> adjustments) {
        return new SaveReviewRequest("CHANGE", TODAY, null, null, null, null,
                null, null, null, null, null, null, null, description, noLongerFits, adjustments);
    }
}
