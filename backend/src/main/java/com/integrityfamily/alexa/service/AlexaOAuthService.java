package com.integrityfamily.alexa.service;

import com.integrityfamily.alexa.config.AlexaProperties;
import com.integrityfamily.alexa.domain.AlexaOAuthCode;
import com.integrityfamily.alexa.domain.AlexaOAuthToken;
import com.integrityfamily.alexa.repository.AlexaOAuthCodeRepository;
import com.integrityfamily.alexa.repository.AlexaOAuthTokenRepository;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlexaOAuthService {

    private final AlexaProperties alexaProperties;
    private final AlexaOAuthCodeRepository codeRepository;
    private final AlexaOAuthTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Valida credenciales y emite un authorization code de un solo uso. */
    @Transactional
    public String createAuthorizationCode(String email, String password,
                                          String redirectUri, String codeChallenge) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }

        Long familyId = (user.getFamily() != null) ? user.getFamily().getId() : null;
        if (familyId == null) {
            throw new IllegalStateException("El usuario no pertenece a ninguna familia");
        }

        long now = nowSeconds();
        String code = randomToken(32);

        codeRepository.save(AlexaOAuthCode.builder()
                .code(code)
                .userId(user.getId())
                .familyId(familyId)
                .redirectUri(redirectUri)
                .codeChallenge(codeChallenge)
                .expiresAt(now + alexaProperties.getCodeTtlSeconds())
                .used(false)
                .createdAt(now)
                .build());

        log.info("[ALEXA-OAUTH] Authorization code emitido para usuario {} familia {}", user.getId(), familyId);
        return code;
    }

    /** Intercambia authorization code + PKCE verifier por access + refresh token. */
    @Transactional
    public Map<String, Object> exchangeCode(String code, String redirectUri, String codeVerifier) {
        AlexaOAuthCode stored = codeRepository.findByCodeAndUsedFalse(code)
                .orElseThrow(() -> new IllegalArgumentException("Código inválido o ya utilizado"));

        if (stored.getExpiresAt() < nowSeconds()) {
            throw new IllegalArgumentException("Código expirado");
        }
        if (!stored.getRedirectUri().equals(redirectUri)) {
            throw new IllegalArgumentException("redirect_uri no coincide");
        }

        // Verificar PKCE S256
        String expectedChallenge = sha256Base64Url(codeVerifier);
        if (!expectedChallenge.equals(stored.getCodeChallenge())) {
            throw new IllegalArgumentException("PKCE verifier inválido");
        }

        // Invalidar código (un solo uso)
        stored.setUsed(true);
        codeRepository.save(stored);

        // Emitir tokens
        long now = nowSeconds();
        String accessToken = randomToken(48);
        String refreshToken = randomToken(48);
        long expiresAt = now + alexaProperties.getTokenTtlSeconds();

        tokenRepository.save(AlexaOAuthToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(stored.getUserId())
                .familyId(stored.getFamilyId())
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(now)
                .build());

        log.info("[ALEXA-OAUTH] Tokens emitidos para usuario {} familia {}", stored.getUserId(), stored.getFamilyId());
        return tokenResponse(accessToken, refreshToken, alexaProperties.getTokenTtlSeconds());
    }

    /** Rota el access token usando el refresh token. */
    @Transactional
    public Map<String, Object> refreshToken(String refreshToken) {
        AlexaOAuthToken existing = tokenRepository.findByRefreshTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido o revocado"));

        long expiresAt = nowSeconds() + alexaProperties.getTokenTtlSeconds();
        String newAccessToken = randomToken(48);

        int updated = tokenRepository.rotateAccessToken(refreshToken, newAccessToken, expiresAt);
        if (updated == 0) {
            throw new IllegalStateException("No se pudo rotar el token");
        }

        log.info("[ALEXA-OAUTH] Access token rotado para usuario {}", existing.getUserId());
        return tokenResponse(newAccessToken, null, alexaProperties.getTokenTtlSeconds());
    }

    /** Resuelve un access token válido a su familia. */
    @Transactional(readOnly = true)
    public Optional<AlexaOAuthToken> resolveToken(String accessToken) {
        return tokenRepository.findByAccessTokenAndRevokedFalse(accessToken)
                .filter(t -> t.getExpiresAt() > nowSeconds());
    }

    private static Map<String, Object> tokenResponse(String accessToken, String refreshToken, long ttl) {
        var map = new java.util.LinkedHashMap<String, Object>();
        map.put("access_token", accessToken);
        if (refreshToken != null) map.put("refresh_token", refreshToken);
        map.put("token_type", "Bearer");
        map.put("expires_in", ttl);
        return map;
    }

    private static String randomToken(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String sha256Base64Url(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 no disponible", e);
        }
    }

    private static long nowSeconds() {
        return System.currentTimeMillis() / 1000L;
    }
}
