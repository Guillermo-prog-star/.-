package com.integrityfamily.ai.controller;

import com.integrityfamily.ai.dto.ChatRequest;
import com.integrityfamily.ai.dto.ChatResponse;
import com.integrityfamily.ai.service.AiService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.exception.NotFoundException;
import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * ChatController: Motor de interacción entre el Nodo Armenia y la IA.
 * Sincronizado con la entidad Family (Campo 'name').
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiService ai;
    private final FamilyRepository famRepo;
    private final EvaluationRepository evaluationRepo;

    public ChatController(AiService ai, FamilyRepository famRepo, EvaluationRepository evaluationRepo) {
        this.ai = ai;
        this.famRepo = famRepo;
        this.evaluationRepo = evaluationRepo;
    }

    /**
     * Procesa mensajes de chat integrando el contexto real de la familia.
     */
    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest req) {
        Family family = famRepo.findById(req.familyId())
                .orElseThrow(() -> new NotFoundException("Familia no encontrada con ID: " + req.familyId()));

        String reply = ai.chat(req.message(), family, "");

        return ApiResponse.ok(new ChatResponse(
                reply,
                family.getFamilyCode(),
                family.getCurrentMilestone()
        ));
    }

    /**
     * Genera reportes de bienestar basados en una evaluación específica.
     * CORRECCIÓN: Se usa getName() para coincidir con la entidad Family.
     */
    @PostMapping("/report/{evaluationId}")
    public ApiResponse<ChatResponse> generateAutoReport(@PathVariable Long evaluationId) throws IOException {
        Evaluation evaluation = evaluationRepo.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Evaluación no encontrada"));

        // Obtención del nombre real usando el getter correcto de la entidad
        String familyRealName = evaluation.getFamily().getName(); 

        String advice = "Integrity Family ha procesado la evaluación " + evaluationId + 
                        ". El reporte de bienestar para la familia " + 
                        familyRealName + " está listo.";

        return ApiResponse.ok(new ChatResponse(
                advice,
                evaluation.getFamily().getFamilyCode(),
                "COMPLETED"
        ));
    }
}