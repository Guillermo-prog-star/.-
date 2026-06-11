package com.integrityfamily.lineage.dto;

import java.util.List;

public record LineageResponse(
        Long id,
        Long familyId,
        String lineageCode,
        String title,
        String description,
        Integer anchorGeneration,
        Integer maxPastGen,
        Integer maxFutureGen,
        String visionStatement,
        String foundingYear,
        List<LineageMemberResponse> members,
        List<LineageRelationshipResponse> relationships,
        List<GenerationInfoResponse> generationInfos
) {}
