package com.integrityfamily.lineage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LineageMemberRequest(
        String firstName,
        String lastName,
        String avatarInitials,
        String avatarColor,

        /** Nivel relativo al ancla: -3 (tatarabuelos) a +3 (bisnietos) */
        @NotNull Integer generation,

        /** founding | builder | responsible | current | future | projected */
        String generationType,
        Boolean isAnchor,

        /** alive | deceased | unknown | future */
        @NotNull String status,

        Integer birthYear,
        Boolean birthYearApproximate,
        String birthDate,
        Integer deathYear,
        String deathDate,
        String origin,
        String roleLabel,
        @Min(0) @Max(100) Integer confidenceLevel,
        String dataSource,

        // ── Campos de evolución ───────────────────────────────────────
        String story,
        String valores,
        String aprendizajes,
        String erroresSuperados,
        String tradiciones,
        String misionesCumplidas,
        String legadoPersonal,

        String photoUrl,
        Float positionX,
        Float positionY,
        Long familyMemberId,
        List<LineageEventRequest> events
) {}
