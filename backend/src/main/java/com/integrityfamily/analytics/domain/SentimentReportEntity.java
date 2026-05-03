package com.integrityfamily.analytics.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sentiment_reports")
public class SentimentReportEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String executiveSummary;

    private Double positivePercentage;
    private Double neutralPercentage;
    private Double negativePercentage;
    private Integer totalSamples;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}


