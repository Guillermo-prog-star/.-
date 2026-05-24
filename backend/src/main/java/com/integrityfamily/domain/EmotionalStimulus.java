package com.integrityfamily.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD: Entidad de Estímulo Emocional (Video, Audio, Texto reflexivo).
 */
@Entity
@Table(name = "emotional_stimuli")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class EmotionalStimulus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // VIDEO, AUDIO, TEXT

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "media_url", nullable = false, length = 1000)
    private String mediaUrl;

    @Column(nullable = false, length = 100)
    private String category; // PRESENCIA, EMPATIA, COMUNICACION

    @Column(name = "target_role", nullable = false, length = 50)
    private String targetRole; // FAMILIA, PADRE, MADRE, HIJO, ADOLESCENTE

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
