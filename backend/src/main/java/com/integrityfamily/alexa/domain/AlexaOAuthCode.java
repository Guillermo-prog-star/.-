package com.integrityfamily.alexa.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alexa_oauth_codes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlexaOAuthCode {

    @Id
    @Column(length = 128)
    private String code;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "redirect_uri", nullable = false, columnDefinition = "TEXT")
    private String redirectUri;

    @Column(name = "code_challenge", nullable = false, length = 256)
    private String codeChallenge;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false)
    private long createdAt;
}
