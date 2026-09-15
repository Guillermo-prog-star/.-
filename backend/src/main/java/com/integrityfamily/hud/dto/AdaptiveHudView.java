package com.integrityfamily.hud.dto;

import com.integrityfamily.dto.home.ResponseMetadata;
import com.integrityfamily.dto.home.ViewerContext;

public sealed interface AdaptiveHudView permits FamilyHudView, ProfessionalHudView {
    String contractVersion();
    HudType hudType();
    ViewerContext viewerContext();
    ResponseMetadata responseMetadata();
}
