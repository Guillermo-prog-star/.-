package com.integrityfamily.auth.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.RefreshToken;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.RefreshTokenRepository;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private final long refreshTokenDurationMs = 604800000L; // 7 días

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado al generar refresh token", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        // Revocar o eliminar tokens anteriores del usuario
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0 || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new BusinessException("La sesión ha expirado. Por favor inicia sesión nuevamente.", "REFRESH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Token de refresco no encontrado", "REFRESH_TOKEN_NOT_FOUND", HttpStatus.UNAUTHORIZED));
    }
}
