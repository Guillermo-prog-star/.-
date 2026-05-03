package com.integrityfamily.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentResult {
    private String text;
    private double score;
    private String label;
    private LocalDateTime timestamp;
}


