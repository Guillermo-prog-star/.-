package com.integrityfamily.familyhome.port;

import com.integrityfamily.dto.home.SafetyMode;

public record SafetyPresentationCandidate(
    SafetyMode mode,
    String title,
    String message
) {
}
