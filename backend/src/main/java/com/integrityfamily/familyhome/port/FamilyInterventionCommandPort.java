package com.integrityfamily.familyhome.port;

import com.integrityfamily.dto.home.AcceptFirstSprintRequest;

import java.util.UUID;

/** Lado de escritura de la intervención familiar (contraparte de {@link FamilyInterventionQueryPort}). */
public interface FamilyInterventionCommandPort {
    SprintSummarySnapshot acceptFirstSprint(UUID familyId, AcceptFirstSprintRequest request);
}
