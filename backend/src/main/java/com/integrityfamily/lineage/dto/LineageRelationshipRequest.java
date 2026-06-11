package com.integrityfamily.lineage.dto;

import jakarta.validation.constraints.NotNull;

public record LineageRelationshipRequest(
        @NotNull Long fromMemberId,
        @NotNull Long toMemberId,
        String relationshipType,
        Boolean isCouple
) {}
