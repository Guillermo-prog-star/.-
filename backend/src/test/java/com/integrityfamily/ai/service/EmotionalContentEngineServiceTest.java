package com.integrityfamily.ai.service;

import com.integrityfamily.ai.dto.EmotionalContentDtos.*;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.provider.TaskType;
import com.integrityfamily.ai.service.AiProviderSelector;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmotionalContentEngineService")
class EmotionalContentEngineServiceTest {

    @Mock EmotionalStimulusRepository emotionalStimulusRepository;
    @Mock ReflectiveSessionRepository reflectiveSessionRepository;
    @Mock FamilyRepository            familyRepository;
    @Mock MemberRepository            memberRepository;
    @Mock AiProviderSelector          aiProviderSelector;
    @Mock AiProvider                  aiProvider;
    @InjectMocks EmotionalContentEngineService service;

    private static final long FAM_ID     = 1L;
    private static final long MEM_ID     = 10L;
    private static final long STIM_ID    = 100L;

    private final Family         family   = Family.builder().id(FAM_ID).name("García").build();
    private final FamilyMember   member   = FamilyMember.builder().id(MEM_ID).fullName("Ana").role("MADRE").build();
    private final EmotionalStimulus stimulus = EmotionalStimulus.builder()
            .id(STIM_ID).title("Pantalla y familia").category("PRESENCIA").type("VIDEO").build();

    private final ReflectionRequest req = new ReflectionRequest(FAM_ID, MEM_ID, "Reflexión de prueba.", 4);

    private void stubHappyPath() {
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        when(memberRepository.findById(MEM_ID)).thenReturn(Optional.of(member));
        when(emotionalStimulusRepository.findById(STIM_ID)).thenReturn(Optional.of(stimulus));
        when(reflectiveSessionRepository
                .findFirstByFamilyIdAndMemberIdAndStimulusId(FAM_ID, MEM_ID, STIM_ID))
                .thenReturn(Optional.empty());
        when(reflectiveSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
    }

    // ── getActiveStimulus ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getActiveStimulus")
    class GetActiveStimulus {

        @Test
        @DisplayName("sin estímulos → Optional.empty")
        void noStimulus_emptyOptional() {
            when(emotionalStimulusRepository.findFirstByTypeOrderByCreatedAtDesc("VIDEO"))
                    .thenReturn(Optional.empty());

            assertThat(service.getActiveStimulus()).isEmpty();
        }

        @Test
        @DisplayName("con estímulo → retorna el primero")
        void withStimulus_returnsFirst() {
            when(emotionalStimulusRepository.findFirstByTypeOrderByCreatedAtDesc("VIDEO"))
                    .thenReturn(Optional.of(stimulus));

            assertThat(service.getActiveStimulus()).contains(stimulus);
        }
    }

    // ── processReflection ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("processReflection")
    class ProcessReflection {

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processReflection(STIM_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(FAM_ID));
        }

        @Test
        @DisplayName("miembro no encontrado → IllegalArgumentException")
        void memberNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findById(MEM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processReflection(STIM_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(MEM_ID));
        }

        @Test
        @DisplayName("estímulo no encontrado → IllegalArgumentException")
        void stimulusNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findById(MEM_ID)).thenReturn(Optional.of(member));
            when(emotionalStimulusRepository.findById(STIM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.processReflection(STIM_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(STIM_ID));
        }

        @Test
        @DisplayName("sesión previa con JSON válido → retorna cacheado sin llamar a AI")
        void existingSession_returnsCache() {
            String cachedJson = """
                    {"empathy":5,"avoidance":1,"disconnection":2,"activePresence":4,"reactivity":1,
                    "feedback":"Cached","recommendedAction":"Cached action"}
                    """;
            ReflectiveSession existing = ReflectiveSession.builder()
                    .inferenceResult(cachedJson).build();
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findById(MEM_ID)).thenReturn(Optional.of(member));
            when(emotionalStimulusRepository.findById(STIM_ID)).thenReturn(Optional.of(stimulus));
            when(reflectiveSessionRepository
                    .findFirstByFamilyIdAndMemberIdAndStimulusId(FAM_ID, MEM_ID, STIM_ID))
                    .thenReturn(Optional.of(existing));

            EmotionalInferenceDto result = service.processReflection(STIM_ID, req);

            assertThat(result.empathy()).isEqualTo(5);
            assertThat(result.feedback()).isEqualTo("Cached");
            verifyNoInteractions(aiProvider);
        }

        @Test
        @DisplayName("AI retorna JSON válido → valores mapeados correctamente, sesión guardada")
        void validAiJson_mappedAndSaved() {
            stubHappyPath();
            String json = """
                    {"empathy":4,"avoidance":2,"disconnection":3,"activePresence":5,"reactivity":1,
                    "feedback":"Excelente reflexión","recommendedAction":"Misión del hogar"}
                    """;
            when(aiProvider.generateRawResponse(any())).thenReturn(json);

            EmotionalInferenceDto result = service.processReflection(STIM_ID, req);

            assertThat(result.empathy()).isEqualTo(4);
            assertThat(result.activePresence()).isEqualTo(5);
            assertThat(result.feedback()).isEqualTo("Excelente reflexión");
            verify(reflectiveSessionRepository).save(any(ReflectiveSession.class));
        }

        @Test
        @DisplayName("AI falla → fallback por defecto (empathy=3, reactivity=2)")
        void aiFails_defaultFallback() {
            stubHappyPath();
            when(aiProvider.generateRawResponse(any())).thenThrow(new RuntimeException("AI down"));

            EmotionalInferenceDto result = service.processReflection(STIM_ID, req);

            assertThat(result.empathy()).isEqualTo(3);
            assertThat(result.reactivity()).isEqualTo(2);
            verify(reflectiveSessionRepository).save(any(ReflectiveSession.class));
        }

        @Test
        @DisplayName("AI falla, reflexión contiene 'pantalla' → disconnection=4, presence=2")
        void aiFails_pantalla_highDisconnection() {
            ReflectionRequest pantallaReq = new ReflectionRequest(FAM_ID, MEM_ID, "El celular y la pantalla nos distancian.", 2);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findById(MEM_ID)).thenReturn(Optional.of(member));
            when(emotionalStimulusRepository.findById(STIM_ID)).thenReturn(Optional.of(stimulus));
            when(reflectiveSessionRepository
                    .findFirstByFamilyIdAndMemberIdAndStimulusId(FAM_ID, MEM_ID, STIM_ID))
                    .thenReturn(Optional.empty());
            when(reflectiveSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
            when(aiProvider.generateRawResponse(any())).thenThrow(new RuntimeException("AI down"));

            EmotionalInferenceDto result = service.processReflection(STIM_ID, pantallaReq);

            assertThat(result.disconnection()).isEqualTo(4);
            assertThat(result.activePresence()).isEqualTo(2);
        }

        @Test
        @DisplayName("AI falla, reflexión contiene 'estrés' → reactivity=4, presence=2")
        void aiFails_estres_highReactivity() {
            ReflectionRequest estresReq = new ReflectionRequest(FAM_ID, MEM_ID, "El estrés del trabajo me agota.", 2);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findById(MEM_ID)).thenReturn(Optional.of(member));
            when(emotionalStimulusRepository.findById(STIM_ID)).thenReturn(Optional.of(stimulus));
            when(reflectiveSessionRepository
                    .findFirstByFamilyIdAndMemberIdAndStimulusId(FAM_ID, MEM_ID, STIM_ID))
                    .thenReturn(Optional.empty());
            when(reflectiveSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
            when(aiProvider.generateRawResponse(any())).thenThrow(new RuntimeException("AI down"));

            EmotionalInferenceDto result = service.processReflection(STIM_ID, estresReq);

            assertThat(result.reactivity()).isEqualTo(4);
            assertThat(result.activePresence()).isEqualTo(2);
        }

        @Test
        @DisplayName("AI falla, reflexión contiene 'conversar' → empathy=5, presence=4")
        void aiFails_conversar_highEmpathy() {
            ReflectionRequest conversarReq = new ReflectionRequest(FAM_ID, MEM_ID, "Me gusta conversar y escuchar a mi familia.", 5);
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(memberRepository.findById(MEM_ID)).thenReturn(Optional.of(member));
            when(emotionalStimulusRepository.findById(STIM_ID)).thenReturn(Optional.of(stimulus));
            when(reflectiveSessionRepository
                    .findFirstByFamilyIdAndMemberIdAndStimulusId(FAM_ID, MEM_ID, STIM_ID))
                    .thenReturn(Optional.empty());
            when(reflectiveSessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(aiProviderSelector.selectProvider(any(TaskType.class))).thenReturn(aiProvider);
            when(aiProvider.generateRawResponse(any())).thenThrow(new RuntimeException("AI down"));

            EmotionalInferenceDto result = service.processReflection(STIM_ID, conversarReq);

            assertThat(result.empathy()).isEqualTo(5);
            assertThat(result.activePresence()).isEqualTo(4);
        }
    }

    // ── getFamilyStats ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getFamilyStats")
    class GetFamilyStats {

        @Test
        @DisplayName("sin sesiones → ioc=0, totalReflections=0, promedios=0")
        void noSessions_allZero() {
            when(reflectiveSessionRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of());

            FamilyEmotionalStats stats = service.getFamilyStats(FAM_ID);

            assertThat(stats.ioc()).isEqualTo(0.0);
            assertThat(stats.totalReflections()).isEqualTo(0);
            assertThat(stats.averageEmpathy()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("sesiones con JSON inválido → ioc=50, defaults de promedio")
        void invalidJsonSessions_defaultStats() {
            ReflectiveSession bad = ReflectiveSession.builder()
                    .inferenceResult("not-json").build();
            when(reflectiveSessionRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(bad));

            FamilyEmotionalStats stats = service.getFamilyStats(FAM_ID);

            assertThat(stats.ioc()).isEqualTo(50.0);
            assertThat(stats.totalReflections()).isEqualTo(1);
            assertThat(stats.averageEmpathy()).isEqualTo(3.0);
            assertThat(stats.averagePresence()).isEqualTo(3.0);
            assertThat(stats.averageReactivity()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("una sesión con empathy=5, presence=5, reactivity=1 → ioc alto")
        void highEmpathyLowReactivity_highIoc() {
            // IOC = (5 + 5 + (6-1)) / 15 * 100 = (5+5+5)/15*100 = 100
            String json = """
                    {"empathy":5,"avoidance":1,"disconnection":1,"activePresence":5,"reactivity":1,
                    "feedback":"X","recommendedAction":"Y"}
                    """;
            ReflectiveSession session = ReflectiveSession.builder().inferenceResult(json).build();
            when(reflectiveSessionRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(session));

            FamilyEmotionalStats stats = service.getFamilyStats(FAM_ID);

            assertThat(stats.ioc()).isEqualTo(100.0);
            assertThat(stats.averageEmpathy()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("dos sesiones → promedio correcto de empathy")
        void twoSessions_averageEmpathy() {
            String json1 = """
                    {"empathy":2,"avoidance":2,"disconnection":2,"activePresence":3,"reactivity":3,
                    "feedback":"A","recommendedAction":"B"}
                    """;
            String json2 = """
                    {"empathy":4,"avoidance":2,"disconnection":2,"activePresence":3,"reactivity":3,
                    "feedback":"A","recommendedAction":"B"}
                    """;
            ReflectiveSession s1 = ReflectiveSession.builder().inferenceResult(json1).build();
            ReflectiveSession s2 = ReflectiveSession.builder().inferenceResult(json2).build();
            when(reflectiveSessionRepository.findByFamilyIdOrderByCreatedAtDesc(FAM_ID))
                    .thenReturn(List.of(s1, s2));

            FamilyEmotionalStats stats = service.getFamilyStats(FAM_ID);

            assertThat(stats.totalReflections()).isEqualTo(2);
            assertThat(stats.averageEmpathy()).isEqualTo(3.0); // (2+4)/2
        }
    }
}
