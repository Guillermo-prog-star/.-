package com.integrityfamily.lineage.dto;

import java.util.List;

public record LineageMemberResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String avatarInitials,
        String avatarColor,
        Integer generation,
        String generationType,
        Boolean isAnchor,
        String status,
        Integer birthYear,
        Boolean birthYearApproximate,
        String birthDate,
        Integer deathYear,
        String deathDate,
        String origin,
        String roleLabel,
        Integer confidenceLevel,
        String dataSource,
        String calculatedAge,

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
        List<LineageEventResponse> events
) {}
