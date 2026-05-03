package com.integrityfamily.report.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SDD-SONIC-MONITOR-01: Voice Audit Entity.
 * Tracks metadata of voice interactions for administrative analytics.
 */
@Entity
@Table(name = "voice_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "municipio")
    private String municipio;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "provider_stt")
    private String providerStt;

    @Column(name = "provider_tts")
    private String providerTts;
}


