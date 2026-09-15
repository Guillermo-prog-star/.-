package com.integrityfamily.dto.home;

public record ResumeBlock(
    String instructionsKey,
    Command confirmCommand
) {
    public ResumeBlock {
        if (instructionsKey == null) {
            throw new IllegalArgumentException("instructionsKey is required");
        }
        if (confirmCommand == null) {
            throw new IllegalArgumentException("confirmCommand is required");
        }
    }
}
