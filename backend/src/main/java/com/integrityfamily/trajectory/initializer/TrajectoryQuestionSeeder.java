package com.integrityfamily.trajectory.initializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrajectoryQuestionSeeder implements CommandLineRunner {

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Iniciando validación de banco de preguntas de trayectorias (JSON)...");
        try {
            ClassPathResource resource = new ClassPathResource("data/questions-bank-risk-trajectories-1500.json");
            if (!resource.exists()) {
                log.warn("El archivo questions-bank-risk-trajectories-1500.json no existe en el classpath.");
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                QuestionBankPayload payload = objectMapper.readValue(is, QuestionBankPayload.class);
                if (payload != null && payload.getQuestions() != null) {
                    int updated = 0;
                    int inserted = 0;
                    List<Question> batch = new ArrayList<>();
                    
                    for (QuestionDto dto : payload.getQuestions()) {
                        Optional<Question> existingOpt = questionRepository.findByQuestionKey(dto.getId());
                        Question q = existingOpt.orElseGet(Question::new);

                        q.setQuestionKey(dto.getId());
                        q.setText(dto.getQuestion());
                        q.setPillarName(dto.getPhase());
                        q.setDimension(dto.getDomainCode());
                        q.setCategory(dto.getTrajectoryCode());
                        q.setRiskType(dto.getRiskLevel());
                        q.setQuestionType("TRAJECTORY");
                        q.setActive(dto.isActive());
                        q.setVersion(String.valueOf(dto.getVersion()));
                        if (dto.getWeight() != null) {
                            q.setWeight(dto.getWeight());
                        } else {
                            q.setWeight(1);
                        }
                        
                        if (dto.getSignals() != null && !dto.getSignals().isEmpty()) {
                            String signals = String.join(", ", dto.getSignals());
                            if (signals.length() > 255) {
                                signals = signals.substring(0, 252) + "...";
                            }
                            q.setAdaptiveTriggers(signals);
                        }

                        if (existingOpt.isPresent()) {
                            updated++;
                        } else {
                            inserted++;
                        }
                        batch.add(q);
                    }
                    
                    questionRepository.saveAll(batch);
                    log.info("✅ [SEEDER] Banco de trayectorias finalizado: {} insertadas, {} actualizadas.", inserted, updated);
                }
            }
        } catch (Exception e) {
            log.error("Error procesando questions-bank-risk-trajectories-1500.json", e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionBankPayload {
        private List<QuestionDto> questions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionDto {
        private String id;
        private String domain;
        private String domainCode;
        private String trajectory;
        private String trajectoryCode;
        private String phase;
        private String phaseCode;
        private String question;
        private String riskLevel;
        private List<String> signals;
        private Integer weight;
        private boolean active;
        private Integer version;
    }
}
