package com.integrityfamily.alexa.repository;

import com.integrityfamily.alexa.domain.AlexaOAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AlexaOAuthTokenRepository extends JpaRepository<AlexaOAuthToken, String> {

    Optional<AlexaOAuthToken> findByAccessTokenAndRevokedFalse(String accessToken);

    Optional<AlexaOAuthToken> findByRefreshTokenAndRevokedFalse(String refreshToken);

    @Transactional
    @Modifying
    @Query("UPDATE AlexaOAuthToken t SET t.accessToken = :newToken, t.expiresAt = :expiresAt WHERE t.refreshToken = :refreshToken AND t.revoked = false")
    int rotateAccessToken(String refreshToken, String newToken, long expiresAt);
}
