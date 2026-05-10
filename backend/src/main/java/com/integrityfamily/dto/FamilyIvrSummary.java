package com.integrityfamily.dto;

public record FamilyIvrSummary(
        Long familyId,
        Integer totalConflicts,
        Integer repairedConflicts,
        Double averageRepairTimeHours,
        Double ivrScore // Índice de Velocidad de Reparación Familiar (0.0 a 100.0)
) {}
