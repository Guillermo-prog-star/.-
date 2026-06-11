package com.integrityfamily.lineage.dto;

public record LineageRelationshipResponse(
        Long id,
        Long fromMemberId,
        Long toMemberId,
        String relationshipType,
        Boolean isCouple
) {}
