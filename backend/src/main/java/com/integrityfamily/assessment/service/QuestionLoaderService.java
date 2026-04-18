package com.integrityfamily.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.assessment.domain.Question;
import com.integrityfamily.assessment.repository.QuestionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * QuestionLoaderService: Motor de carga para el banco jerárquico de Integrity Family.
 * Procesa dimensiones (Reconocimiento/Amor) y áreas (Emociones/Comunicación).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class QuestionLoaderService {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        loadQuestionsIfNeeded();
    }

    /**
     * Aplana el JSON jerárquico y puebla la tabla de preguntas.
     * Dimensión = Primer nivel (reconocimiento, amor, etc.)
     * Área = Segundo nivel (emociones, comunicacion, etc.)
     */
    @Transactional
    public void loadQuestionsIfNeeded() {
        try {
            log.info(">>>> [NODO ARMENIA] Sincronizando Banco de 1.000 Reactivos (Modo Transformación)...");
            
            // William Lopez: Limpiamos para asegurar coherencia total con los nuevos escenarios reales
            questionRepository.deleteAll();

            InputStream inputStream = new ClassPathResource("questions-bank.json").getInputStream();
            JsonNode rootNode = objectMapper.readTree(inputStream);
            List<Question> allQuestions = new ArrayList<>();

            // 1. Recorrer Dimensiones (reconocimiento, amor, entrega)
            rootNode.fieldNames().forEachRemaining(dimensionName -> {
                JsonNode dimensionNode = rootNode.get(dimensionName);

                // 2. Recorrer Áreas (emociones, comunicacion, habitos, tiempos)
                dimensionNode.fieldNames().forEachRemaining(areaName -> {
                    JsonNode questionsArray = dimensionNode.get(areaName);

                    // 3. Crear Entidad por cada pregunta
                    questionsArray.forEach(qNode -> {
                        allQuestions.add(Question.builder()
                                .dimension(dimensionName.toUpperCase())
                                .area(areaName.toUpperCase())
                                .questionKey(qNode.get("id").asText())
                                .text(qNode.get("text").asText())
                                .active(true)
                                .build());
                    });
                });
            });

            // 4. Persistencia Atómica
            questionRepository.saveAll(allQuestions);
            log.info(">>>> [NODO ARMENIA] ¡ÉXITO TOTAL! Se han sembrado {} preguntas en el sistema.", allQuestions.size());

        } catch (Exception e) {
            log.error(">>>> [NODO ARMENIA] ERROR CRÍTICO procesando el banco jerárquico: {}", e.getMessage());
        }
    }
}