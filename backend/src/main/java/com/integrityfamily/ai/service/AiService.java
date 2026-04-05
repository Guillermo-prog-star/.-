package com.integrityfamily.ai.service;

import com.integrityfamily.family.domain.Family;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.StringJoiner;

@Service
public class AiService {

    public String chat(String message, Family family, String context) {
        String cleanMessage = message == null || message.isBlank()
                ? "Mensaje vacío"
                : message.trim();

        String cleanContext = context == null ? "" : context.trim();

        StringBuilder reply = new StringBuilder();
        reply.append("He recibido tu mensaje: ").append(cleanMessage).append(". ");

        if (!cleanContext.isBlank()) {
            reply.append("Contexto adicional: ").append(cleanContext).append(". ");
        }

        reply.append("Sugerencia inicial: fortalecer la comunicación, revisar acuerdos familiares y definir una acción pequeña para esta semana.");

        return reply.toString();
    }

    public String getPersonalizedAdvice(Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return "No hay puntajes disponibles. Recomendación inicial: realizar una revisión general de emociones, comunicación, hábitos y tiempos.";
        }

        StringJoiner joiner = new StringJoiner(", ");
        String weakestDimension = null;
        double weakestScore = Double.MAX_VALUE;

        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            String dimension = entry.getKey();
            Double score = entry.getValue();

            double safeScore = score == null ? 0.0 : score;
            joiner.add(dimension + "=" + safeScore);

            if (safeScore < weakestScore) {
                weakestScore = safeScore;
                weakestDimension = dimension;
            }
        }

        StringBuilder advice = new StringBuilder();
        advice.append("Resumen de puntajes: ").append(joiner).append(". ");

        if (weakestDimension != null) {
            advice.append("La dimensión con mayor atención requerida es '")
                  .append(weakestDimension)
                  .append("'. ");
        }

        advice.append("Plan sugerido: conversar en familia, identificar una causa concreta y definir un hábito semanal medible.");

        return advice.toString();
    }
}