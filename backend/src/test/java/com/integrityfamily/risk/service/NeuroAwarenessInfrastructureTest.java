package com.integrityfamily.risk.service;

import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.QuestionOption;
import com.integrityfamily.domain.repository.QuestionRepository;
import com.integrityfamily.dto.EvaluationDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NeuroAwarenessInfrastructureTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private RiskAlgoV1Engine riskAlgo;

    @Test
    void testV83MigrationAndRelations() {
        // 1. & 2. Validar que la migración corre y no afecta antiguas (vienen sin opciones por defecto)
        Question oldQuestion = new Question();
        oldQuestion.setText("Pregunta Clásica");
        oldQuestion.setType("CORE");
        oldQuestion.setDimension("emociones");
        questionRepository.save(oldQuestion);

        Question savedOld = questionRepository.findById(oldQuestion.getId()).orElseThrow();
        assertThat(savedOld.getOptions()).isEmpty(); // No afecta antiguas

        // 3. Crear una pregunta NEURO_AWARENESS con opciones
        Question neuroQ = new Question();
        neuroQ.setText("Contexto: Situación de estrés...");
        neuroQ.setType("NEURO_AWARENESS");
        neuroQ.setDimension("comunicacion");
        neuroQ.setPhase("ACTION"); // Solo TIMING/ACTION aportan al promedio INC (ver RiskAlgoV1Engine)
        
        QuestionOption opt1 = new QuestionOption();
        opt1.setQuestion(neuroQ);
        opt1.setText("Reacciono mal");
        opt1.setScoreValue(2);
        
        neuroQ.getOptions().add(opt1);
        questionRepository.save(neuroQ);

        Question savedNeuro = questionRepository.findById(neuroQ.getId()).orElseThrow();
        assertThat(savedNeuro.getOptions()).hasSize(1);
        assertThat(savedNeuro.getOptions().get(0).getText()).isEqualTo("Reacciono mal");

        // 4. & 5. Validar motor de riesgo (INC vs ICF Histórico)
        EvaluationDtos.AnswerDto oldAnswer = new EvaluationDtos.AnswerDto(savedOld.getId(), 4, 4);
        EvaluationDtos.AnswerDto neuroAnswer = new EvaluationDtos.AnswerDto(savedNeuro.getId(), 2, 2);

        // a) Solo respuestas antiguas (ICF histórico intacto, INC = 0)
        RiskAlgoV1Engine.AlgoResult resultOld = riskAlgo.compute(List.of(oldAnswer), "W1");
        assertThat(resultOld.healthyIndex()).isGreaterThan(0); // ICF histórico se calcula normalmente
        assertThat(resultOld.inc()).isEqualTo(0.0); // INC es 0 si no hay NEURO_AWARENESS

        // b) Mezcla de respuestas (ICF calcula con ambas, INC calcula solo con NEURO_AWARENESS)
        // Valor 2 en neuro (promedio 2). Fórmula: ((2-1)/4)*100 = 25%
        RiskAlgoV1Engine.AlgoResult resultMixed = riskAlgo.compute(List.of(oldAnswer, neuroAnswer), "W1");
        assertThat(resultMixed.healthyIndex()).isGreaterThan(0);
        assertThat(resultMixed.inc()).isEqualTo(25.0);
    }
}
