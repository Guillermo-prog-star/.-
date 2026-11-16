package com.integrityfamily.dto.home;

public record ProgressBlock(
    int completed,
    int total,
    int percentage
) {
    public ProgressBlock {
        if (completed < 0) {
            throw new IllegalArgumentException("Completed tasks cannot be negative");
        }
        if (total < 1) {
            throw new IllegalArgumentException("Total tasks must be at least 1");
        }
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        if (completed > total) {
            throw new IllegalArgumentException("Completed tasks cannot exceed total tasks");
        }
    }
}
