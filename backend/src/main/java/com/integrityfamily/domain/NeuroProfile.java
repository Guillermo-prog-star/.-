package com.integrityfamily.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Representa la arquitectura vectorial de consciencia de un individuo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NeuroProfile implements Serializable {
    private double somaticAwareness;
    private double emotionalAwareness;
    private double cognitiveAwareness;
    private double impulsiveAwareness;
    private double pauseCapacity;
    private double integrationScore;
}
