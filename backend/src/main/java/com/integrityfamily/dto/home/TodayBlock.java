package com.integrityfamily.dto.home;

public record TodayBlock(
    NarrativeBlock narrativeBlock,
    Command primaryCommand,
    DisplayPace displayPace
) {
    public TodayBlock {
        if (narrativeBlock == null) {
            throw new IllegalArgumentException("narrativeBlock is required");
        }
        if (primaryCommand == null) {
            throw new IllegalArgumentException("primaryCommand is required");
        }
        if (displayPace == null) {
            throw new IllegalArgumentException("displayPace is required");
        }
    }
}
