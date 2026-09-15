package com.integrityfamily.dto.home;

import java.util.List;
import java.util.UUID;

public record ViewerContext(
    UUID memberId,
    ViewerRole role,
    List<String> permissions
) {
    public ViewerContext {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("permissions is required");
        }
        permissions = List.copyOf(permissions);
    }
}
