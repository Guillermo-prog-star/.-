package com.integrityfamily.sistemas.catalog;

import com.integrityfamily.sistemas.dto.SistemasDtos.CatalogResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.LevelView;
import com.integrityfamily.sistemas.dto.SistemasDtos.MonthStep;
import com.integrityfamily.sistemas.dto.SistemasDtos.ScreenLink;
import com.integrityfamily.sistemas.dto.SistemasDtos.SystemView;

import java.util.List;

/**
 * Los diez sistemas y las pantallas que ya los cubren. No es estado de una familia.
 */
public final class SistemasCatalog {

    private SistemasCatalog() {}

    public static CatalogResponse catalog() {
        return new CatalogResponse(
                "Sistemas IF",
                "Dónde vive cada cosa y cuándo la revisamos",
                "Definir qué necesita la familia para organizar la información y qué prácticas la mantienen útil.",
                "Capturar sin procesar acumula pendientes. Planificar sin revisar deja planes viejos. Guardar conocimiento sin poder encontrarlo impide usarlo.",
                "Tener diez sistemas no exige diez aplicaciones. Una herramienta puede cubrir varias funciones si cada información tiene un destino claro.",
                List.of(levelOne(), levelTwo()),
                List.of(
                        new MonthStep(1, "Elegir la herramienta y unificar calendarios; empezar la revisión diaria."),
                        new MonthStep(2, "Hacer la primera revisión semanal completa."),
                        new MonthStep(3, "Pasar los compromisos de correo y chat a tareas o eventos."),
                        new MonthStep(4, "Repetir la revisión semanal y responder la prueba de las tres preguntas.")
                ),
                List.of(
                        "¿Hiciste al menos 3 de las 4 revisiones semanales?",
                        "¿Llegó algún compromiso a la fecha sin estar registrado?",
                        "¿La captura se vació al menos una vez por semana?"
                ),
                "Es una propuesta. Su funcionamiento se comprueba en la práctica, con la prueba de 4 semanas. La semanal es la que sostiene el sistema: si un día hay que recortar, se recorta la diaria, nunca la semanal."
        );
    }

    private static LevelView levelOne() {
        return new LevelView(1, "Nivel 1 — Estructura", "Qué debe existir y dónde se guarda cada cosa.", List.of(
                system(1, "Captura", "Función", "Recibir ideas, solicitudes y pendientes.",
                        "Regla", "Registrar rápidamente y decidir su destino al procesarlos.",
                        null, null,
                        "Diario (JournalEntry), con lo ocurrido separado de lo interpretado. En la app se escribe en la bitácora y en Mi Espacio.",
                        false, null, null,
                        List.of(link("Bitácora", "/logbook"), link("Mi Espacio", "/my-space"))),
                system(2, "Calendario", "Función", "Mostrar compromisos y tiempo reservado.",
                        "Regla", "Mantener una vista consolidada de los calendarios relevantes.",
                        null, null,
                        "Sprint de 7 o 15 días, planeación y días críticos.",
                        false, null, null,
                        List.of(link("Sprint familiar", "/sprint"),
                                link("Planeación", "/transformation/weekly-plan"),
                                link("Días críticos", "/crisis"))),
                system(3, "Tareas", "Función", "Gestionar acciones y proyectos.",
                        "Regla", "Cada acción indica qué hacer y quién responde. El responsable del plan es una propuesta: la familia decide.",
                        null, null,
                        "PlanTask y SprintMission. El responsable propuesto no es un compromiso aceptado.",
                        false, null, null,
                        List.of(link("Plan familiar", "/plans"),
                                link("Misiones del sprint", "/sprint"),
                                link("Evidencias", "/checklist"))),
                system(4, "Conocimiento", "Función", "Conservar información reutilizable.",
                        "Regla", "Guardar con título, contexto y fuente cuando corresponda.",
                        null, null,
                        "Centro de Documentación, documental, legado y biografía.",
                        false, null, null,
                        List.of(link("Centro de Documentación", "/documentation"),
                                link("Documental", "/documentary-maker"),
                                link("Legado", "/legado"),
                                link("Nuestra Historia", "/biography"))),
                system(5, "Comunicación", "Función", "Coordinar personas y acuerdos.",
                        "Regla", "Un compromiso que llega por mensaje se convierte en tarea o evento.",
                        null, null,
                        "Chat, consejo familiar y acuerdos.",
                        false, null, null,
                        List.of(link("Chat", "/chat"), link("Consejo familiar", "/family-council")))
        ));
    }

    private static LevelView levelTwo() {
        return new LevelView(2, "Nivel 2 — Cómo hacer que los sistemas funcionen",
                "Cuándo revisarlos, qué decidir y cómo ajustarlos. Cada revisión deja una salida escrita.",
                List.of(
                        system(6, "Revisión diaria", "Práctica", "Revisión diaria",
                                "Pregunta principal", "¿Qué requiere atención hoy?",
                                "Resultado esperado", "Agenda comprobada y de 1 a 3 acciones para hoy. La captura queda vacía.",
                                "Check-in diario del sprint (attempt_status). Si ese día ya hay check-in de quien revisa, la revisión queda ligada. No crea otro.",
                                false, "/sistemas/revision/diaria", "5–10 min",
                                List.of(link("Hacer la revisión", "/sistemas/revision/diaria"),
                                        link("Check-in del sprint", "/logbook"))),
                        system(7, "Planificación semanal", "Práctica", "Planificación semanal",
                                "Pregunta principal", "¿Qué podemos completar esta semana?",
                                "Resultado esperado", "Calendario revisado, tareas depuradas y como máximo 3 prioridades.",
                                "Retrospectiva del sprint. Si ya existe una de esta semana, la revisión queda ligada. No cierra el sprint ni le pide a la IA que decida.",
                                false, "/sistemas/revision/semanal", "30–60 min",
                                List.of(link("Hacer la revisión", "/sistemas/revision/semanal"),
                                        link("Sprint y retrospectiva", "/sprint"))),
                        system(8, "Ajuste por cambio", "Práctica", "Ajuste por cambio",
                                "Pregunta principal", "¿Qué cambió en nuestras condiciones?",
                                "Resultado esperado", "Qué compromisos ya no caben, y qué se deja, se reduce o se pospone.",
                                "Hitos y etapas. No es una revisión de calendario: se hace cuando algo cambia (un nacimiento, un curso, una mudanza) y, una vez por trimestre, dentro de la mensual.",
                                true, "/sistemas/revision/cambio", "Cuando algo cambia",
                                List.of(link("Registrar el ajuste", "/sistemas/revision/cambio"),
                                        link("Ruta y hitos", "/transformation/route"))),
                        system(9, "Revisión mensual", "Práctica", "Revisión mensual",
                                "Pregunta principal", "¿Qué avanza y qué está bloqueado?",
                                "Resultado esperado", "Una decisión por proyecto: continuar, corregir, pausar o cerrar.",
                                "Hitos M1, M2… del plan. La decisión queda escrita aquí.",
                                false, "/sistemas/revision/mensual", "30 min",
                                List.of(link("Hacer la revisión", "/sistemas/revision/mensual"),
                                        link("Plan e hitos", "/plans"))),
                        system(10, "Revisión anual", "Práctica", "Revisión anual y objetivos",
                                "Pregunta principal", "¿Qué resultados queremos y por qué?",
                                "Resultado esperado", "Pocos objetivos, cada uno con criterio de logro y fecha de revisión.",
                                "Visión del plan y hitos M12 y M24.",
                                false, "/sistemas/revision/anual", "2–3 h",
                                List.of(link("Hacer la revisión", "/sistemas/revision/anual"),
                                        link("Visión del plan", "/plans"),
                                        link("Ruta de 36 meses", "/transformation/route")))
                ));
    }

    private static SystemView system(int number, String name, String functionLabel, String functionText,
                                     String ruleLabel, String ruleText, String outcomeLabel, String outcomeText,
                                     String whereInIf, boolean eventTriggered, String reviewPath, String duration,
                                     List<ScreenLink> links) {
        return new SystemView(number, name, functionLabel, functionText, ruleLabel, ruleText,
                outcomeLabel, outcomeText, whereInIf, eventTriggered, reviewPath, duration, links);
    }

    private static ScreenLink link(String label, String route) {
        return new ScreenLink(label, route);
    }
}
