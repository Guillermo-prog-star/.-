package com.integrityfamily.dto.home;

import java.util.List;

public record SafetyPresentation(
    SafetyMode mode,
    String title,
    String message,
    List<Command> allowedCommands
) {
    public SafetyPresentation {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
    }
}
