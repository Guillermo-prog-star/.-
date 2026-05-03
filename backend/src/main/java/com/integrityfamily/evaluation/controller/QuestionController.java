package com.integrityfamily.evaluation.controller;

import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;

/**
 * SDD: Controlador Maestro de Preguntas (Refactored).
 * Postura Técnica: Consolidado bajo el dominio centralizado.
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class QuestionController {

    private final QuestionRepository questionRepository;

    @GetMapping("/random")
    public List<Question> getRandomAssessment(@RequestParam(required = false) Long familyId) {
        if (familyId == null) return Collections.emptyList();
        
        // Lógica simplificada: 20 reactivos base
        return questionRepository.findAll().stream()
                .limit(20)
                .toList();
    }

    @GetMapping
    public List<Question> getAll() {
        return questionRepository.findAll();
    }

    @PostMapping
    public Question create(@RequestBody Question question) {
        return questionRepository.save(question);
    }
}
