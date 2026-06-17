package com.integrityfamily.alexa.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "alexa_oauth_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlexaOAuthToken {

    @Id
    @Column(name = "access_token", length = 128)
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, unique = true, length = 128)
    private String refreshToken;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false)
    private long createdAt;
}
