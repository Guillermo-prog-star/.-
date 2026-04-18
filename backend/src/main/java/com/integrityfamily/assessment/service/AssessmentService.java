package com.integrityfamily.assessment.service;

import com.integrityfamily.assessment.domain.Assessment;
import com.integrityfamily.assessment.domain.AssessmentDetail;
import com.integrityfamily.assessment.domain.Question;
import com.integrityfamily.assessment.repository.AssessmentRepository;
import com.integrityfamily.assessment.repository.QuestionRepository;
import com.integrityfamily.common.service.AiService;
import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentService {

    private static final Logger log = LoggerFactory.getLogger(AssessmentService.class);

    private final QuestionRepository questionRepository;
    private final AssessmentRepository assessmentRepository;
    private final FamilyRepository familyRepository;
    private final AiService aiService;

    public AssessmentService(QuestionRepository questionRepository,
                             AssessmentRepository assessmentRepository,
                             FamilyRepository familyRepository,
                             AiService aiService) {
        this.questionRepository = questionRepository;
        this.assessmentRepository = assessmentRepository;
        this.familyRepository = familyRepository;
        this.aiService = aiService;
    }

    public List<Question> getQuestionsForFamily(Long familyId) {
        Family family = familyRepository.findById(familyId).orElseThrow();
        String milestone = family.getCurrentMilestone();
        log.info("🎯 Cargando preguntas para hito: {}", milestone);
        
        if (milestone == null || milestone.equals("MES_00_DIAGNOSTICO_BASE")) {
            return questionRepository.findByDimension("RECONOCIMIENTO");
        }
        
        if (milestone.contains("MES_12") || milestone.contains("MES_24")) {
            return questionRepository.findAll(); // Carga completa para hitos mayores
        }

        return questionRepository.findByDimension("AMOR"); // Por defecto para transición
    }

    public List<Assessment> getHistoryByFamily(Long familyId) {
        return assessmentRepository.findByFamilyIdOrderByAssessmentDateDesc(familyId);
    }

    public Map<String, Object> generateSpiritualSummary(Long familyId) {
        Family family = familyRepository.findById(familyId).orElseThrow();
        Map<String, Object> context = new HashMap<>();
        context.put("familyName", family.getName());
        context.put("milestone", family.getCurrentMilestone());
        
        String synthesis = aiService.generateSynthesis(context);
        
        Map<String, Object> response = new HashMap<>();
        response.put("synthesis", synthesis);
        response.put("milestone", family.getCurrentMilestone());
        response.put("generatedAt", java.time.LocalDateTime.now());
        
        return response;
    }

    public Assessment saveAssessment(Long familyId, Map<String, Double> scores) {
        Family family = familyRepository.findById(familyId).orElseThrow();
        
        Assessment assessment = new Assessment();
        assessment.setFamily(family);
        
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            AssessmentDetail detail = new AssessmentDetail();
            detail.setAssessment(assessment);
            detail.setDimensionName(entry.getKey());
            detail.setScore(entry.getValue());
            assessment.getDetails().add(detail);
        }

        return assessmentRepository.save(assessment);
    }
}