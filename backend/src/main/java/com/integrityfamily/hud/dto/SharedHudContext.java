package com.integrityfamily.hud.dto;

import com.integrityfamily.dto.home.ResponseMetadata;
import com.integrityfamily.dto.home.ViewerContext;
import java.util.UUID;

public record SharedHudContext(
    UUID familyId,
    String displayName,
    ViewerContext viewerContext,
    String currentLocale,
    UUID correlationId,
    ResponseMetadata responseMetadata
) {}
