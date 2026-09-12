package com.integrityfamily.dto.home;

public record JourneyContext(
    JourneyStage stage,
    ProgressBlock progress,
    Command nextCommand
) {
    public JourneyContext {
        if (stage == null) {
            throw new IllegalArgumentException("stage is required");
        }
        if (progress == null) {
            throw new IllegalArgumentException("progress is required");
        }
    }
}
