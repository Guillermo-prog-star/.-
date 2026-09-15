package com.integrityfamily.dto.home;

public record Command(
    String id,
    String label,
    CommandType type,
    String target,
    boolean enabled,
    boolean requiresConfirmation,
    String permission,
    String analyticsEvent,
    Boolean idempotencyRequired
) {
    public Command {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (label == null) {
            throw new IllegalArgumentException("label is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (target == null) {
            throw new IllegalArgumentException("target is required");
        }
    }
}
