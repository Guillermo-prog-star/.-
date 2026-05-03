package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.SentimentResult;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * SDD: Sentiment Analysis Engine
 * Objetivo: Procesar logs de comunicaciÃƒÂ³n para detectar tensiones.
 */
@Service
public class SentimentAnalysisService {

    public SentimentResult analyze(String text) {
        // 1. Limpieza de datos (Stop words, lemmatization)
        // 2. ClasificaciÃƒÂ³n: POSITIVE, NEUTRAL, NEGATIVE, CRISIS
        // 3. PuntuaciÃƒÂ³n: -1.0 a 1.0

        // SimulaciÃƒÂ³n de lÃƒÂ³gica SDD para el Nodo Armenia
        double score = calculateVaderScore(text);

        return SentimentResult.builder()
                .text(text)
                .score(score)
                .label(determineLabel(score))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String determineLabel(double score) {
        if (score < -0.5)
            return "CRISIS";
        if (score < 0)
            return "NEGATIVE";
        return "POSITIVE";
    }

    private double calculateVaderScore(String text) {
        if (text == null)
            return 0.0;
        String lowercaseText = text.toLowerCase();

        // LÃƒÂ³gica de pesos por palabras clave del Nodo Armenia
        if (lowercaseText.contains("crisis") || lowercaseText.contains("pelea"))
            return -0.9;
        if (lowercaseText.contains("bienvenido") || lowercaseText.contains("felicidades"))
            return 0.8;

        return 0.0;
    }
}


