package com.integrityfamily.evaluation.controller;

import com.integrityfamily.assessment.domain.Question;
import com.integrityfamily.assessment.repository.QuestionRepository;
import com.integrityfamily.assessment.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") 
public class QuestionController {

    private final AssessmentService assessmentService;
    private final QuestionRepository questionRepository; // Añadido para persistencia directa

    /**
     * Endpoint Maestro para el Test de 20 Reactivos
     * GET /api/questions/random?familyId=XXX
     */
    @GetMapping("/random")
    public List<Question> getRandomAssessment(@RequestParam(required = false) Long familyId) {
        // William Lopez: Si no viene familyId, usamos una lógica base o retornamos vacío para evitar fallos
        if (familyId == null) return java.util.Collections.emptyList();
        return assessmentService.generateRandomAssessment(familyId);
    }

    /**
     * Obtener el banco completo de preguntas (Útil para administración)
     * GET /api/questions
     */
    @GetMapping
    public List<Question> getAll() {
        return questionRepository.findAll();
    }

    /**
     * Crear un nuevo reactivo en el banco de preguntas
     * POST /api/questions
     */
    @PostMapping
    public Question create(@RequestBody Question question) {
        return questionRepository.save(question);
    }
}