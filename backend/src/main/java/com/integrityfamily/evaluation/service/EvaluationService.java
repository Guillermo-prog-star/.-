package com.integrityfamily.evaluation.service;

import com.integrityfamily.evaluation.domain.Evaluation;
import com.integrityfamily.evaluation.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;

    public List<Evaluation> findAll() {
        return evaluationRepository.findAll();
    }

    public Evaluation findById(Long id) {
        return evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));
    }

    public Evaluation create(Evaluation evaluation) {
        return evaluationRepository.save(evaluation);
    }

    public Evaluation update(Long id, Evaluation request) {
        Evaluation existing = evaluationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evaluación no encontrada"));

        return evaluationRepository.save(existing);
    }

    public void delete(Long id) {
        if (!evaluationRepository.existsById(id)) {
            throw new RuntimeException("Evaluación no encontrada");
        }
        evaluationRepository.deleteById(id);
    }
}