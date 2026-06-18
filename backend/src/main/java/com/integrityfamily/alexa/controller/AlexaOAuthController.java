package com.integrityfamily.alexa.controller;

import com.integrityfamily.alexa.service.AlexaOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/oauth/alexa")
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class AlexaOAuthController {

    private final AlexaOAuthService alexaOAuthService;

    /**
     * Alexa redirige aquí para iniciar Account Linking.
     * Devuelve el formulario de login HTML.
     */
    @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
    public String authorize(
            @RequestParam String response_type,
            @RequestParam String client_id,
            @RequestParam String redirect_uri,
            @RequestParam String state,
            @RequestParam String code_challenge,
            @RequestParam(defaultValue = "S256") String code_challenge_method) {

        return loginPageHtml(redirect_uri, state, code_challenge);
    }

    /**
     * El usuario envía el formulario de login.
     * Valida credenciales, emite authorization code y redirige a Alexa.
     */
    @PostMapping("/login")
    public void login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String redirect_uri,
            @RequestParam String state,
            @RequestParam String code_challenge,
            HttpServletResponse response) throws IOException {

        try {
            String code = alexaOAuthService.createAuthorizationCode(email, password, redirect_uri, code_challenge);
            String redirectUrl = redirect_uri
                    + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
            response.sendRedirect(redirectUrl);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("[ALEXA-OAUTH] Login fallido: {}", e.getMessage());
            String errorPage = loginPageHtml(redirect_uri, state, code_challenge)
                    .replace("<!--ERROR-->", "<p class=\"error\">" + escapeHtml(e.getMessage()) + "</p>");
            response.setContentType("text/html;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(errorPage);
        }
    }

    /**
     * Endpoint de token OAuth 2.0.
     * Soporta grant_type=authorization_code y grant_type=refresh_token.
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(@RequestParam Map<String, String> params) {
        String grantType = params.get("grant_type");
        try {
            if ("authorization_code".equals(grantType)) {
                String code = params.get("code");
                String redirectUri = params.get("redirect_uri");
                String codeVerifier = params.get("code_verifier");
                Map<String, Object> tokenResponse = alexaOAuthService.exchangeCode(code, redirectUri, codeVerifier);
                return ResponseEntity.ok(tokenResponse);

            } else if ("refresh_token".equals(grantType)) {
                String refreshToken = params.get("refresh_token");
                Map<String, Object> tokenResponse = alexaOAuthService.refreshToken(refreshToken);
                return ResponseEntity.ok(tokenResponse);

            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "unsupported_grant_type"));
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("[ALEXA-OAUTH] Token error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "invalid_grant", "error_description", e.getMessage()));
        }
    }

    private static String loginPageHtml(String redirectUri, String state, String codeChallenge) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Conectar Integrity Family con Alexa</title>
                  <style>
                    body { font-family: Arial, sans-serif; max-width: 400px; margin: 60px auto; padding: 0 20px; }
                    h1 { font-size: 1.4rem; color: #2d6a4f; }
                    input { width: 100%%; padding: 10px; margin: 8px 0; box-sizing: border-box; border: 1px solid #ccc; border-radius: 4px; }
                    button { width: 100%%; padding: 12px; background: #2d6a4f; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 1rem; }
                    button:hover { background: #1b4332; }
                    .error { color: #c0392b; font-size: 0.9rem; }
                  </style>
                </head>
                <body>
                  <h1>Conectar con Alexa</h1>
                  <p>Inicia sesión con tu cuenta de Integrity Family para vincularla con Alexa.</p>
                  <!--ERROR-->
                  <form method="POST" action="/oauth/alexa/login">
                    <input type="hidden" name="redirect_uri" value="%s">
                    <input type="hidden" name="state" value="%s">
                    <input type="hidden" name="code_challenge" value="%s">
                    <label>Email</label>
                    <input type="email" name="email" required autofocus>
                    <label>Contraseña</label>
                    <input type="password" name="password" required>
                    <button type="submit">Vincular cuenta</button>
                  </form>
                </body>
                </html>
                """.formatted(
                escapeHtml(redirectUri),
                escapeHtml(state),
                escapeHtml(codeChallenge));
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
