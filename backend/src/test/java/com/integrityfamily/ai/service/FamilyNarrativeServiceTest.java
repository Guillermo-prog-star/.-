package com.integrityfamily.ai.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.scanner.dto.SubtleSignalRadarResponse;
import com.integrityfamily.scanner.service.SubtleSignalRadarService;
import com.integrityfamily.simulation.dto.FamilyScenarioResponse;
import com.integrityfamily.simulation.service.FamilyScenarioProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FamilyNarrativeService — Unit Tests")
class FamilyNarrativeServiceTest {

    @Mock SubtleSignalRadarService radarService;
    @Mock FamilyScenarioProjectionService scenarioService;
    @Mock PromptGenerator promptGenerator;
    @Mock AiProvider aiProvider;
    @Mock FamilyRepository familyRepository;
    @Mock FamilyLongitudinalStateRepository ltsRepository;

    @InjectMocks FamilyNarrativeService service;

    private static final Long FAM = 1L;
    private static final String NARRATIVE_TEXT = "La historia de esta familia está cambiando...";

    private Family family;
    private SubtleSignalRadarResponse radarResponse;
    private FamilyScenarioResponse scenarioResponse;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(FAM).name("Familia Test").build();

        SubtleSignalRadarResponse.IcfTrend icfTrend =
                new SubtleSignalRadarResponse.IcfTrend(65.0, 3.0, 5.0, "IMPROVING", "consciente");

        radarResponse = new SubtleSignalRadarResponse(
                FAM, 3,
                null, null, null, null,
                icfTrend,
                List.of(), List.of(), List.of(),
                75, "Narrativa local del radar", LocalDateTime.now()
        );

        FamilyScenarioResponse.ProjectionPoint pt4 =
                new FamilyScenarioResponse.ProjectionPoint(4, 67.0, 59.0, 75.0, "MODERADO");
        FamilyScenarioResponse.ProjectionPoint pt8 =
                new FamilyScenarioResponse.ProjectionPoint(8, 64.0, 56.0, 72.0, "MODERADO");
        FamilyScenarioResponse.ProjectionPoint pt12A =
                new FamilyScenarioResponse.ProjectionPoint(12, 60.0, 52.0, 68.0, "MODERADO");
        FamilyScenarioResponse.ProjectionPoint pt12B =
                new FamilyScenarioResponse.ProjectionPoint(12, 70.0, 64.0, 76.0, "BAJO");
        FamilyScenarioResponse.ProjectionPoint pt12C =
                new FamilyScenarioResponse.ProjectionPoint(12, 82.0, 77.0, 87.0, "BAJO");

        FamilyScenarioResponse.DimensionProjection dp =
                new FamilyScenarioResponse.DimensionProjection("emociones", 65.0, 67.0, 2.0, "IMPROVE");

        FamilyScenarioResponse.Scenario scenA = new FamilyScenarioResponse.Scenario(
                "Sin intervención", "A", 55, "SLIGHT_DECLINE",
                pt4, pt8, pt12A, dp, dp, dp, dp, "MODERADO", "Narrativa A", List.of("No hacer nada"));
        FamilyScenarioResponse.Scenario scenB = new FamilyScenarioResponse.Scenario(
                "Misiones actuales", "B", 65, "IMPROVE",
                pt4, pt8, pt12B, dp, dp, dp, dp, "BAJO", "Narrativa B", List.of("Hacer misiones"));
        FamilyScenarioResponse.Scenario scenC = new FamilyScenarioResponse.Scenario(
                "Intervención intensiva", "C", 40, "STRONG_IMPROVE",
                pt4, pt8, pt12C, dp, dp, dp, dp, "BAJO", "Narrativa C", List.of("Máximo esfuerzo"));

        scenarioResponse = new FamilyScenarioResponse(
                FAM, 65.0, scenA, scenB, scenC,
                "El futuro está en sus manos.", "Ventana de oportunidad: 8 semanas.",
                LocalDateTime.now()
        );

        // Stub genérico usado por la mayoría de tests
        stubPromptGenerator("PROMPT_GENERADO");
        when(aiProvider.generateWithFullPrompt("PROMPT_GENERADO")).thenReturn(NARRATIVE_TEXT);
    }

    /** Stub el PromptGenerator para que devuelva el prompt deseado con cualquier argumento. */
    @SuppressWarnings("unchecked")
    private void stubPromptGenerator(String returnValue) {
        when(promptGenerator.buildEvolutiveNarrativePrompt(
                any(), any(), any(),
                anyDouble(), any(),
                any(), any(),
                anyInt(),
                any(), any(), any(),
                anyDouble(), any(),
                anyDouble(), any(),
                anyDouble(), any(),
                any(), any()
        )).thenReturn(returnValue);
    }

    private void stubBase() {
        when(familyRepository.findById(FAM)).thenReturn(Optional.of(family));
        when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.empty());
        when(radarService.analyze(FAM)).thenReturn(radarResponse);
        when(scenarioService.project(FAM)).thenReturn(scenarioResponse);
    }

    // ─── Flujo nominal ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate() — flujo nominal")
    class FlujoCorrecto {

        @Test
        @DisplayName("devuelve NarrativeResponse con familyId, familyName y narrative correctos")
        void generate_retornaRespuestaCompleta() {
            stubBase();

            FamilyNarrativeService.NarrativeResponse result = service.generate(FAM);

            assertThat(result.familyId()).isEqualTo(FAM);
            assertThat(result.familyName()).isEqualTo("Familia Test");
            assertThat(result.narrative()).isEqualTo(NARRATIVE_TEXT);
            assertThat(result.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("radarConfidence refleja el confidenceScore del radar")
        void generate_radarConfidenceDelRadar() {
            stubBase();

            FamilyNarrativeService.NarrativeResponse result = service.generate(FAM);

            assertThat(result.radarConfidence()).isEqualTo(75);
        }

        @Test
        @DisplayName("invoca aiProvider.generateWithFullPrompt (no generateResponse)")
        void generate_usaGenerateWithFullPrompt() {
            stubBase();

            service.generate(FAM);

            verify(aiProvider).generateWithFullPrompt("PROMPT_GENERADO");
        }

        @Test
        @DisplayName("invoca radarService y scenarioService exactamente una vez")
        void generate_invocaServiciosUnaVez() {
            stubBase();

            service.generate(FAM);

            verify(radarService).analyze(FAM);
            verify(scenarioService).project(FAM);
        }
    }

    // ─── Familia no encontrada ────────────────────────────────────────────────

    @Nested
    @DisplayName("generate() — familia no existe")
    class FamiliaNoExiste {

        @Test
        @DisplayName("lanza IllegalArgumentException cuando familia no existe")
        void generate_familiaInexistente_lanzaException() {
            when(familyRepository.findById(FAM)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generate(FAM))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ─── Con LTS ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generate() — con LTS")
    class ConLts {

        @Test
        @DisplayName("LTS presente → generate() completa sin excepción y devuelve narrativa")
        void generate_ltsPresente_completaCorrectamente() {
            when(familyRepository.findById(FAM)).thenReturn(Optional.of(family));
            FamilyLongitudinalState lts = FamilyLongitudinalState.builder()
                    .evolutionPhase("consciente")
                    .narrativeStage("CRECIMIENTO")
                    .criticalDimension("comunicacion")
                    .build();
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.of(lts));
            when(radarService.analyze(FAM)).thenReturn(radarResponse);
            when(scenarioService.project(FAM)).thenReturn(scenarioResponse);

            FamilyNarrativeService.NarrativeResponse result = service.generate(FAM);

            assertThat(result.narrative()).isEqualTo(NARRATIVE_TEXT);
            assertThat(result.familyName()).isEqualTo("Familia Test");
        }
    }

    // ─── Radar sin icfOverall ─────────────────────────────────────────────────

    @Nested
    @DisplayName("generate() — radar sin icfOverall")
    class RadarSinIcfOverall {

        @Test
        @DisplayName("radar con icfOverall null → icfDirection = STABLE, no lanza excepción")
        void radarSinIcfOverall_noLanzaExcepcion() {
            SubtleSignalRadarResponse radarSinIcf = new SubtleSignalRadarResponse(
                    FAM, 0,
                    null, null, null, null, null,
                    List.of(), List.of(), List.of(),
                    5, "Sin datos", LocalDateTime.now()
            );

            when(familyRepository.findById(FAM)).thenReturn(Optional.of(family));
            when(ltsRepository.findByFamilyId(FAM)).thenReturn(Optional.empty());
            when(radarService.analyze(FAM)).thenReturn(radarSinIcf);
            when(scenarioService.project(FAM)).thenReturn(scenarioResponse);

            FamilyNarrativeService.NarrativeResponse result = service.generate(FAM);

            assertThat(result).isNotNull();
            assertThat(result.narrative()).isEqualTo(NARRATIVE_TEXT);
        }
    }

    // ─── PromptGenerator recibe los ICF de escenarios ─────────────────────────

    @Nested
    @DisplayName("generate() — datos de escenarios pasados al prompt")
    class DatosEscenarios {

        @Test
        @DisplayName("generatedAt del response no es null")
        void generate_generatedAtNoNull() {
            stubBase();

            FamilyNarrativeService.NarrativeResponse result = service.generate(FAM);

            assertThat(result.generatedAt()).isNotNull();
        }
    }
}
