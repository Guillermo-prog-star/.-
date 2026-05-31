package com.integrityfamily.legado.dto;

import lombok.Data;
import java.util.List;

@Data
public class LegacyRequest {
    // Historia
    private String historyLessons;
    private String historyConserve;
    private String historyAvoidErrors;
    private String historyToLeave;
    private String historyRecognition;
    // Constitución
    private String constitutionFamilyName;
    private Integer constitutionYear;
    private String foundingPrinciple;
    private String commitments;
    private String neverDo;
    private String conflictResolution;
    // Misión & Visión
    private String familyMission;
    private String familyVision;
    private String familyTagline;
    // Carta
    private String letterFrom;
    private String letterTo;
    private Integer letterOpenInYear;
    private String letterContent;
    private boolean letterSealed;
    // Valores
    private List<ValueDto> values;

    @Data
    public static class ValueDto {
        private Long id;
        private String icon;
        private String name;
        private String description;
        private int sortOrder;
    }
}
