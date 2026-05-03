package com.integrityfamily.evaluation.service;

import org.springframework.stereotype.Service;

import com.integrityfamily.assessment.domain.Question;

import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionService {

    private final List<Question> questions = new ArrayList<>();

    public List<Question> findAll() {
        return questions;
    }

    public Question save(Question question) {
        questions.add(question);
        return question;
    }
}


