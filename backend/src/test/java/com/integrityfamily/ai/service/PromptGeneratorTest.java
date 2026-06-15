package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.AiContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromptGenerator")
class PromptGeneratorTest {

    @Spy ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks PromptGenerator generator;

    // ── buildPrompt — routing ─────────────────────────────────────────────────

    @Nested
    @DisplayName("buildPrompt — routing de modo")
    class BuildPromptRouting {

        @Test
        @DisplayName("context=null → fallback básico con identity y mensaje")
        void nullContext_fallbackPrompt() {
            String result = generator.buildPrompt("Hola", null);

            assertThat(result).contains("Hola");
            assertThat(result).contains("system_identity");
        }

        @Test
        @DisplayName("activeMember=null → modo FAMILY")
        void nullActiveMember_familyMode() {
            AiContext ctx = mock(AiContext.class);
            // activeMember() devuelve null por defecto → modo FAMILY
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildPrompt("Consulta familiar", ctx);

            assertThat(result).contains("MODO: FAMILIA");
        }

        @Test
        @DisplayName("activeMember presente, isGuardian=false → modo MEMBER")
        void memberNotGuardian_memberMode() {
            AiContext ctx = mock(AiContext.class);
            AiContext.ActiveMemberProfile member = mock(AiContext.ActiveMemberProfile.class);
            when(member.isGuardian()).thenReturn(false);
            when(member.fullName()).thenReturn("Ana");
            when(member.role()).thenReturn("MADRE");
            when(member.consciousnessLevel()).thenReturn("Reactiva");
            when(ctx.activeMember()).thenReturn(member);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildPrompt("¿Cómo mejoro?", ctx);

            assertThat(result).contains("MODO: MIEMBRO INDIVIDUAL");
            assertThat(result).contains("Ana");
        }

        @Test
        @DisplayName("activeMember presente, isGuardian=true → modo GUARDIAN")
        void memberIsGuardian_guardianMode() {
            AiContext ctx = mock(AiContext.class);
            AiContext.ActiveMemberProfile guardian = mock(AiContext.ActiveMemberProfile.class);
            when(guardian.isGuardian()).thenReturn(true);
            when(guardian.fullName()).thenReturn("Carlos");
            when(guardian.role()).thenReturn("GUARDIAN"); // necesario para buildCriticalThinkingBlock
            when(ctx.activeMember()).thenReturn(guardian);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildPrompt("¿Qué hago?", ctx);

            assertThat(result).contains("MODO: GUARDIÁN FAMILIAR");
            assertThat(result).contains("Carlos");
        }

        @Test
        @DisplayName("mensaje del usuario siempre incluido en el prompt resultante")
        void userMessageAlwaysIncluded() {
            AiContext ctx = mock(AiContext.class);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildPrompt("Mensaje de prueba único", ctx);

            assertThat(result).contains("Mensaje de prueba único");
        }
    }

    // ── buildIdentityAnalysisPrompt ──────────────────────────────────────────

    @Nested
    @DisplayName("buildIdentityAnalysisPrompt")
    class BuildIdentityAnalysisPrompt {

        @Test
        @DisplayName("mensajes numerados incluidos en el prompt")
        void messagesNumbered() {
            String result = generator.buildIdentityAnalysisPrompt(
                    List.of("Primer mensaje", "Segundo mensaje"), "PADRE", "Luis");

            assertThat(result).contains("1. Primer mensaje");
            assertThat(result).contains("2. Segundo mensaje");
        }

        @Test
        @DisplayName("nombre y rol del miembro incluidos en el prompt")
        void memberNameAndRoleIncluded() {
            String result = generator.buildIdentityAnalysisPrompt(
                    List.of("Hola"), "MADRE", "Lucía");

            assertThat(result).contains("Lucía");
            assertThat(result).contains("MADRE");
        }

        @Test
        @DisplayName("prompt solicita JSON con campos de identidad")
        void promptRequestsIdentityJson() {
            String result = generator.buildIdentityAnalysisPrompt(
                    List.of("msg"), "HIJO", "Mateo");

            assertThat(result).contains("communicationStyle");
            assertThat(result).contains("reflexivityLevel");
            assertThat(result).contains("changeResistance");
        }
    }

    // ── buildSessionSynthesisPrompt ──────────────────────────────────────────

    @Nested
    @DisplayName("buildSessionSynthesisPrompt")
    class BuildSessionSynthesisPrompt {

        @Test
        @DisplayName("mensajes del usuario incluidos")
        void userMessagesIncluded() {
            String result = generator.buildSessionSynthesisPrompt(
                    List.of("Siento frustración", "Pero intento mejorar"),
                    "PADRE", "Roberto", "SUPPORT", "ESCALATING");

            assertThat(result).contains("Siento frustración");
            assertThat(result).contains("Pero intento mejorar");
        }

        @Test
        @DisplayName("nombre, rol, objetivo y arco emocional incluidos")
        void metadataIncluded() {
            String result = generator.buildSessionSynthesisPrompt(
                    List.of("msg1"), "MADRE", "Sofia", "PLANNING", "STABLE");

            assertThat(result).contains("Sofia");
            assertThat(result).contains("MADRE");
            assertThat(result).contains("PLANNING");
            assertThat(result).contains("STABLE");
        }

        @Test
        @DisplayName("prompt solicita JSON de síntesis")
        void promptRequestsSynthesisJson() {
            String result = generator.buildSessionSynthesisPrompt(
                    List.of("texto"), "HIJO", "Juan", "GENERAL", "MILD_TENSION");

            assertThat(result).contains("importanceScore");
            assertThat(result).contains("emotionalSummary");
        }
    }
}
