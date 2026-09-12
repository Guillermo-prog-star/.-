package com.integrityfamily.familyhome.application;

import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.dto.home.FamilyActionResult;

import java.util.UUID;

/**
 * IFRM-D Hito 5 — Family Action Engine.
 *
 * A diferencia de {@link FamilyHomeProjectionService} (lado de lectura), este motor
 * ejecuta comandos semánticos ({@code SUBMIT_ACTION}) que mutan el estado de la familia:
 * autoriza, valida el estado de journey vigente, ejecuta la acción de dominio real,
 * garantiza idempotencia y deja rastro auditable.
 *
 * Alcance actual: solo {@code accept-first-sprint} tiene un efecto de dominio real hoy
 * (crea el primer sprint de la familia). Los comandos {@code confirm-resume} y
 * {@code resume-journey} quedan pendientes hasta definir cómo se persiste el estado de
 * pausa/reanudación del journey (hoy {@code JourneyStage} es 100% derivado, no hay un
 * flag de "familia en pausa" que mutar).
 */
public interface FamilyActionEngine {
    FamilyActionResult acceptFirstSprint(
            UUID familyId,
            UUID authenticatedUserId,
            AcceptFirstSprintRequest request,
            String idempotencyKey);
}
