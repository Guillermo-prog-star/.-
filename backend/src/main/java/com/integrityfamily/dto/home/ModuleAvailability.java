package com.integrityfamily.dto.home;

public record ModuleAvailability(
    ModuleStatus todayBlock,
    ModuleStatus dimensionsBlock,
    ModuleStatus memoryBlock
) {
    public ModuleAvailability {
        if (todayBlock == null) {
            throw new IllegalArgumentException("todayBlock status is required");
        }
        if (dimensionsBlock == null) {
            throw new IllegalArgumentException("dimensionsBlock status is required");
        }
        if (memoryBlock == null) {
            throw new IllegalArgumentException("memoryBlock status is required");
        }
    }
}
