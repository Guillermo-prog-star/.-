package com.integrityfamily.feedback.controller;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.Feedback;
import com.integrityfamily.domain.repository.FeedbackRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final FamilyRepository familyRepository;

    @PostMapping("/send")
    public ApiResponse<Feedback> submit(@RequestBody FeedbackRequest request) {
        Family family = familyRepository.findById(request.getFamilyId()).orElseThrow();
        
        Feedback fb = Feedback.builder()
                .family(family)
                .score(request.getScore())
                .comment(request.getComment())
                .type(request.getType())
                .milestoneAtMoment(family.getCurrentMilestone())
                .build();
        
        return ApiResponse.ok(feedbackRepository.save(fb));
    }

    @GetMapping("/all")
    public ApiResponse<List<Feedback>> getAll() {
        return ApiResponse.ok(feedbackRepository.findAllByOrderByCreatedAtDesc());
    }

    @Data
    public static class FeedbackRequest {
        private Long familyId;
        private int score;
        private String comment;
        private String type;
    }
}


