package com.integrityfamily.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    // ── buildGuardianMentorPrompt ────────────────────────────────────────────

    @Nested
    @DisplayName("buildGuardianMentorPrompt")
    class BuildGuardianMentorPrompt {

        @Test
        @DisplayName("incluye MODO GUARDIÁN FAMILIAR")
        void includesGuardianMode() {
            AiContext ctx = mock(AiContext.class);
            AiContext.ActiveMemberProfile member = mock(AiContext.ActiveMemberProfile.class);
            when(member.fullName()).thenReturn("Carlos");
            when(member.isGuardian()).thenReturn(true);
            when(member.role()).thenReturn("GUARDIAN");
            when(ctx.activeMember()).thenReturn(member);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildGuardianMentorPrompt("¿Qué hago?", ctx);

            assertThat(result).contains("MODO: GUARDIÁN FAMILIAR");
        }

        @Test
        @DisplayName("incluye el nombre del guardián en el prompt")
        void includesGuardianName() {
            AiContext ctx = mock(AiContext.class);
            AiContext.ActiveMemberProfile member = mock(AiContext.ActiveMemberProfile.class);
            when(member.fullName()).thenReturn("Carlos");
            when(member.isGuardian()).thenReturn(true);
            when(member.role()).thenReturn("GUARDIAN");
            when(ctx.activeMember()).thenReturn(member);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildGuardianMentorPrompt("Necesito ayuda", ctx);

            assertThat(result).contains("Carlos");
        }

        @Test
        @DisplayName("incluye el mensaje del usuario en el prompt")
        void includesUserMessage() {
            AiContext ctx = mock(AiContext.class);
            AiContext.ActiveMemberProfile member = mock(AiContext.ActiveMemberProfile.class);
            when(member.fullName()).thenReturn("Ana");
            when(member.isGuardian()).thenReturn(true);
            when(member.role()).thenReturn("GUARDIAN");
            when(ctx.activeMember()).thenReturn(member);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildGuardianMentorPrompt("Mensaje específico del guardián", ctx);

            assertThat(result).contains("Mensaje específico del guardián");
        }

        @Test
        @DisplayName("activeMember null → usa 'el Guardián' como nombre por defecto")
        void nullActiveMemberUsesDefault() {
            AiContext ctx = mock(AiContext.class);
            when(ctx.activeMember()).thenReturn(null);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildGuardianMentorPrompt("Hola", ctx);

            assertThat(result).contains("el Guardián");
        }
    }

    // ── buildMemberMentorPrompt ──────────────────────────────────────────────

    @Nested
    @DisplayName("buildMemberMentorPrompt")
    class BuildMemberMentorPrompt {

        private AiContext ctxFor(String name, String role, String consciousness) {
            AiContext ctx = mock(AiContext.class);
            AiContext.ActiveMemberProfile member = mock(AiContext.ActiveMemberProfile.class);
            when(member.fullName()).thenReturn(name);
            when(member.role()).thenReturn(role);
            when(member.consciousnessLevel()).thenReturn(consciousness);
            when(member.isGuardian()).thenReturn(false);
            when(ctx.activeMember()).thenReturn(member);
            when(ctx.sentinelActive()).thenReturn(false);
            return ctx;
        }

        @Test
        @DisplayName("incluye MODO MIEMBRO INDIVIDUAL")
        void includesMemberMode() {
            String result = generator.buildMemberMentorPrompt("¿Cómo mejoro?", ctxFor("Lucía", "MADRE", "Consciente"));
            assertThat(result).contains("MODO: MIEMBRO INDIVIDUAL");
        }

        @Test
        @DisplayName("incluye nombre del miembro")
        void includesMemberName() {
            String result = generator.buildMemberMentorPrompt("Mensaje", ctxFor("Marco", "PADRE", "Reactivo"));
            assertThat(result).contains("Marco");
        }

        @Test
        @DisplayName("incluye rol del miembro")
        void includesMemberRole() {
            String result = generator.buildMemberMentorPrompt("Msg", ctxFor("Sofía", "HIJA", "Inconsciente"));
            assertThat(result).contains("HIJA");
        }

        @Test
        @DisplayName("incluye nivel de conciencia")
        void includesConsciousnessLevel() {
            String result = generator.buildMemberMentorPrompt("Msg", ctxFor("Pedro", "HIJO", "Pleno"));
            assertThat(result).contains("Pleno");
        }

        @Test
        @DisplayName("incluye el mensaje del usuario")
        void includesUserMessage() {
            String result = generator.buildMemberMentorPrompt("Pregunta única ABC", ctxFor("Laura", "MADRE", "Consciente"));
            assertThat(result).contains("Pregunta única ABC");
        }
    }

    // ── buildFamilyMentorPrompt ──────────────────────────────────────────────

    @Nested
    @DisplayName("buildFamilyMentorPrompt")
    class BuildFamilyMentorPrompt {

        @Test
        @DisplayName("incluye MODO FAMILIA")
        void includesFamilyMode() {
            AiContext ctx = mock(AiContext.class);
            when(ctx.activeMember()).thenReturn(null);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildFamilyMentorPrompt("¿Cómo avanzamos?", ctx);

            assertThat(result).contains("MODO: FAMILIA");
        }

        @Test
        @DisplayName("incluye el mensaje del usuario en <user_input>")
        void includesUserMessage() {
            AiContext ctx = mock(AiContext.class);
            when(ctx.activeMember()).thenReturn(null);
            when(ctx.sentinelActive()).thenReturn(false);

            String result = generator.buildFamilyMentorPrompt("Mensaje familiar único", ctx);

            assertThat(result).contains("Mensaje familiar único");
        }
    }

    // ── buildGuardianBriefingPrompt ──────────────────────────────────────────

    @Nested
    @DisplayName("buildGuardianBriefingPrompt")
    class BuildGuardianBriefingPrompt {

        @Test
        @DisplayName("incluye el nombre del guardián")
        void includesGuardianName() {
            String result = generator.buildGuardianBriefingPrompt(
                    "Roberto", "LOW", 3, 1, List.of("Ana"), "Ana", 5, "Hito Alpha", 0.65);
            assertThat(result).contains("Roberto");
        }

        @Test
        @DisplayName("fatiga HIGH incluye alerta de agotamiento")
        void highFatigueIncludesAlert() {
            String result = generator.buildGuardianBriefingPrompt(
                    "Rosa", "HIGH", 1, 3, List.of("Luis", "Pedro", "María"), null, 0, "Hito Beta", 0.30);
            assertThat(result).containsAnyOf("ALERTA", "ÚNICO miembro activo", "agotamiento");
        }

        @Test
        @DisplayName("fatiga LOW incluye mensaje de participación saludable")
        void lowFatiguePositiveMessage() {
            String result = generator.buildGuardianBriefingPrompt(
                    "Laura", "LOW", 4, 0, List.of(), null, 0, "Hito Gamma", 0.80);
            assertThat(result).contains("saludable");
        }

        @Test
        @DisplayName("miembros inactivos aparecen en el briefing")
        void inactiveMembersListed() {
            String result = generator.buildGuardianBriefingPrompt(
                    "Diego", "MILD", 2, 2, List.of("Carlos", "Marta"), "Carlos", 10, "Hito Delta", 0.50);
            assertThat(result).containsAnyOf("Carlos", "Marta");
        }

        @Test
        @DisplayName("sin miembros inactivos → mensaje positivo de participación")
        void noInactiveMembers() {
            String result = generator.buildGuardianBriefingPrompt(
                    "Elena", "LOW", 4, 0, List.of(), null, 0, "Hito Epsilon", 0.90);
            assertThat(result).contains("Todos los miembros");
        }

        @Test
        @DisplayName("tasa de completación incluida en el briefing")
        void completionRateIncluded() {
            String result = generator.buildGuardianBriefingPrompt(
                    "Mario", "LOW", 3, 0, List.of(), null, 0, "Hito Zeta", 0.75);
            assertThat(result).contains("75");
        }
    }

    // ── buildReengagementPrompt ──────────────────────────────────────────────

    @Nested
    @DisplayName("buildReengagementPrompt")
    class BuildReengagementPrompt {

        @Test
        @DisplayName("incluye nombre del guardián")
        void includesGuardianName() {
            String result = generator.buildReengagementPrompt("Ana", "Luis", 7, "Los Pérez");
            assertThat(result).contains("Ana");
        }

        @Test
        @DisplayName("incluye nombre del miembro objetivo")
        void includesTargetName() {
            String result = generator.buildReengagementPrompt("Ana", "Luis", 7, "Los Pérez");
            assertThat(result).contains("Luis");
        }

        @Test
        @DisplayName("incluye los días de inactividad")
        void includesDaysSinceActivity() {
            String result = generator.buildReengagementPrompt("Rosa", "Pedro", 14, "Familia Martínez");
            assertThat(result).contains("14");
        }

        @Test
        @DisplayName("incluye el nombre de la familia")
        void includesFamilyName() {
            String result = generator.buildReengagementPrompt("Tomás", "Carmen", 3, "Los González");
            assertThat(result).contains("Los González");
        }
    }

    // ── buildDashboardInsightPrompt ──────────────────────────────────────────

    @Nested
    @DisplayName("buildDashboardInsightPrompt")
    class BuildDashboardInsightPromptTest {

        private Family family(String name) {
            Family f = new Family();
            f.setName(name);
            f.setCurrentMilestone("Hito Test");
            return f;
        }

        @Test
        @DisplayName("incluye el nombre de la familia")
        void includesFamilyName() {
            String result = generator.buildDashboardInsightPrompt(
                    family("Familia García"), Map.of("EMOCIONES", 45.0), "HIGH");
            assertThat(result).contains("Familia García");
        }

        @Test
        @DisplayName("incluye el nivel de riesgo")
        void includesRiskLevel() {
            String result = generator.buildDashboardInsightPrompt(
                    family("Test"), Map.of("COMUNICACION", 72.0), "MODERADO");
            assertThat(result).contains("MODERADO");
        }

        @Test
        @DisplayName("incluye valores de dimensiones en el JSON")
        void includesDimensionValues() {
            String result = generator.buildDashboardInsightPrompt(
                    family("Test"), Map.of("HABITOS", 55.5), "BAJO");
            assertThat(result).contains("HABITOS");
        }
    }

    // ── buildMissionGenerationPrompt ─────────────────────────────────────────

    @Nested
    @DisplayName("buildMissionGenerationPrompt")
    class BuildMissionGenerationPromptTest {

        private Family family(String name) {
            Family f = new Family();
            f.setName(name);
            f.setCurrentMilestone("Hito Misión");
            return f;
        }

        @Test
        @DisplayName("incluye el nombre de la familia")
        void includesFamilyName() {
            String result = generator.buildMissionGenerationPrompt(
                    family("Familia López"), Map.of("Integridad", 60.0), "MODERADO");
            assertThat(result).contains("Familia López");
        }

        @Test
        @DisplayName("incluye el nivel de riesgo")
        void includesRiskLevel() {
            String result = generator.buildMissionGenerationPrompt(
                    family("Test"), Map.of("EMOCIONES", 40.0), "ALTO");
            assertThat(result).contains("ALTO");
        }

        @Test
        @DisplayName("solicita output en formato JSON con campo title")
        void requestsJsonOutput() {
            String result = generator.buildMissionGenerationPrompt(
                    family("Test"), Map.of("COMUNICACION", 50.0), "BAJO");
            assertThat(result).contains("\"title\"");
        }

        @Test
        @DisplayName("incluye los 5 horizontes temporales de la misión")
        void includesTimeHorizons() {
            String result = generator.buildMissionGenerationPrompt(
                    family("Test"), Map.of("HABITOS", 60.0), "BAJO");
            assertThat(result).containsAnyOf("1 mes", "3 meses", "6 meses");
        }
    }

    // ── buildDiagnosticMissionsPrompt ────────────────────────────────────────

    @Nested
    @DisplayName("buildDiagnosticMissionsPrompt")
    class BuildDiagnosticMissionsPrompt {

        private Family family(String name) {
            Family f = new Family();
            f.setName(name);
            f.setCurrentMilestone("Hito Diagnóstico");
            return f;
        }

        private FamilyMember member(String name, String role) {
            FamilyMember m = new FamilyMember();
            m.setFullName(name);
            m.setRole(role);
            return m;
        }

        @Test
        @DisplayName("incluye el nombre del miembro en el prompt")
        void includesMemberName() {
            String result = generator.buildDiagnosticMissionsPrompt(
                    family("Los Ramírez"), member("Valeria", "MADRE"), "{}", 72.0, "BAJO");
            assertThat(result).contains("Valeria");
        }

        @Test
        @DisplayName("incluye el rol del miembro")
        void includesMemberRole() {
            String result = generator.buildDiagnosticMissionsPrompt(
                    family("Los Ramírez"), member("Tomás", "PADRE"), "{}", 55.0, "MODERADO");
            assertThat(result).contains("PADRE");
        }

        @Test
        @DisplayName("member null → usa nombre por defecto 'Miembro'")
        void nullMemberUsesDefault() {
            String result = generator.buildDiagnosticMissionsPrompt(
                    family("Test"), null, "{}", 50.0, "BAJO");
            assertThat(result).contains("Miembro");
        }

        @Test
        @DisplayName("incluye el score ICF")
        void includesIcfScore() {
            String result = generator.buildDiagnosticMissionsPrompt(
                    family("Test"), member("Rosa", "HIJA"), "{}", 88.0, "BAJO");
            assertThat(result).contains("88");
        }
    }
}
