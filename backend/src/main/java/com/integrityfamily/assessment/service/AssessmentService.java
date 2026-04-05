package com.integrityfamily.assessment.service;

import com.integrityfamily.assessment.domain.Assessment;
import com.integrityfamily.assessment.domain.AssessmentDetail;
import com.integrityfamily.assessment.domain.Question;
import com.integrityfamily.assessment.repository.AssessmentRepository;
import com.integrityfamily.assessment.repository.QuestionRepository;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final FamilyRepository familyRepository;
    private final QuestionRepository questionRepository;

    // --- 1. GENERACIÓN DE EVALUACIÓN (20 reactivos aleatorios) ---
    
    public List<Question> generateRandomAssessment() {
        List<Question> fullAssessment = new ArrayList<>();
        String[] dimensions = {"EMOCIONES", "COMUNICACION", "HABITOS", "TIEMPOS"};

        for (String dim : dimensions) {
            fullAssessment.addAll(questionRepository.findRandomQuestionsByDimension(dim, 5));
        }
        
        Collections.shuffle(fullAssessment);
        return fullAssessment;
    }

    // --- 2. MÉTODO FILTRADO (El que causó el error de Maven) ---

    public List<Question> getFilteredQuestions(String dimension, String area) {
        // William, este método ahora permite al Controller compilar. 
        // Trae 10 preguntas por dimensión como base.
        return questionRepository.findRandomQuestionsByDimension(dimension, 10);
    }

    // --- 3. MÉTODOS DE CONSULTA E HISTORIAL ---

    public Assessment getLatestByFamily(Long familyId) {
        return assessmentRepository.findFirstByFamilyIdOrderByCreatedAtDesc(familyId)
                .orElse(null);
    }

    public List<Assessment> getHistoryByFamily(Long familyId) {
        return assessmentRepository.findByFamilyIdOrderByCreatedAtAsc(familyId);
    }

    public Map<String, Object> generateSpiritualSummary(Long familyId) {
        // Método requerido por el flujo de análisis
        return new HashMap<>(); 
    }

    // --- 4. LÓGICA DE GUARDADO (Cálculos William Lopez) ---

    @Transactional
    public Assessment saveAssessment(Long familyId, Map<String, Integer> responses) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada en el Nodo Armenia"));

        Assessment assessment = Assessment.builder()
                .family(family)
                .build();

        double emotionalSum = 0, financialSum = 0;
        int emotionalCount = 0, financialCount = 0;

        List<AssessmentDetail> details = new ArrayList<>();
        
        for (Map.Entry<String, Integer> entry : responses.entrySet()) {
            String category = entry.getKey().startsWith("EMO") ? "EMOCIONAL" : "FINANCIERO";
            
            details.add(AssessmentDetail.builder()
                    .assessment(assessment)
                    .category(category)
                    .questionKey(entry.getKey())
                    .score(entry.getValue())
                    .build());

            if (category.equals("EMOCIONAL")) {
                emotionalSum += entry.getValue();
                emotionalCount++;
            } else {
                financialSum += entry.getValue();
                financialCount++;
            }
        }

        // Cálculos sobre base 5 (William Lopez)
        double eScore = emotionalCount > 0 ? (emotionalSum / (emotionalCount * 5)) * 100 : 0;
        double fScore = financialCount > 0 ? (financialSum / (financialCount * 5)) * 100 : 0;

        assessment.setEmotionalScore(eScore);
        assessment.setFinancialScore(fScore);
        assessment.setGlobalScore((eScore + fScore) / 2);
        assessment.setDetails(details);

        return assessmentRepository.save(assessment);
    }
}