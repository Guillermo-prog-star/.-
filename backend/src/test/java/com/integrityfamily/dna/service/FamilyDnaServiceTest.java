package com.integrityfamily.dna.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.dna.domain.FamilyDna;
import com.integrityfamily.dna.dto.FamilyDnaDto;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.domain.Evaluation;
import com.integrityfamily.domain.EvaluationStatus;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.EvaluationRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyDnaService")
class FamilyDnaServiceTest {

    @Mock FamilyDnaRepository    dnaRepository;
    @Mock FamilyRepository       familyRepository;
    @Mock EvaluationRepository   evaluationRepository;
    @Mock RiskSnapshotRepository riskRepository;
    @Mock AiProvider             aiProvider;
    @Spy  ObjectMapper           objectMapper = new ObjectMapper();

    @InjectMocks FamilyDnaService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Family family(long id) {
        return Family.builder().id(id).name("Familia López").build();
    }

    private FamilyDna fullDna(long familyId) {
        return FamilyDna.builder()
                .familyId(familyId)
                .valores("[\"Amor\",\"Respeto\"]")
                .fortalezas("[\"Unidad\"]")
                .sombras("[\"Enojo\"]")
                .patrones("[\"Reunión dominical\"]")
                .estiloComunicacion("Cálido y directo")
                .ritmoFamiliar("Ritmo semanal activo")
                .potencialOculto("[{\"miembro\":\"Juan\",\"talento\":\"Liderazgo\",\"descripcion\":\"Natural\"}]")
                .narrativaIa("Una familia con potencial enorme.")
                .version(2)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByFamilyId
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByFamilyId")
    class FindByFamilyId {

        @Test
        @DisplayName("sin ADN registrado → Optional vacío")
        void noDna_returnsEmpty() {
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.empty());

            assertThat(service.findByFamilyId(1L)).isEmpty();
        }

        @Test
        @DisplayName("ADN existente → DTO con campos mapeados correctamente")
        void dnaExists_mappedToDto() {
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(fullDna(1L)));

            FamilyDnaDto dto = service.findByFamilyId(1L).orElseThrow();

            assertThat(dto.familyId()).isEqualTo(1L);
            assertThat(dto.valores()).containsExactly("Amor", "Respeto");
            assertThat(dto.fortalezas()).containsExactly("Unidad");
            assertThat(dto.narrativaIa()).isEqualTo("Una familia con potencial enorme.");
            assertThat(dto.version()).isEqualTo(2);
        }

        @Test
        @DisplayName("potencialOculto JSON → lista de PotencialMiembroDto")
        void potencialOculto_parsedToList() {
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(fullDna(1L)));

            FamilyDnaDto dto = service.findByFamilyId(1L).orElseThrow();

            assertThat(dto.potencialOculto()).hasSize(1);
            assertThat(dto.potencialOculto().get(0).miembro()).isEqualTo("Juan");
            assertThat(dto.potencialOculto().get(0).talento()).isEqualTo("Liderazgo");
            assertThat(dto.potencialOculto().get(0).descripcion()).isEqualTo("Natural");
        }

        @Test
        @DisplayName("version null → DTO usa 1 como valor por defecto")
        void versionNull_usesDefault() {
            FamilyDna d = FamilyDna.builder().familyId(1L).version(null).build();
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(d));

            FamilyDnaDto dto = service.findByFamilyId(1L).orElseThrow();

            assertThat(dto.version()).isEqualTo(1);
        }

        @Test
        @DisplayName("valores null → DTO retorna lista vacía (no NPE)")
        void nullJson_emptyList() {
            FamilyDna d = FamilyDna.builder().familyId(1L).valores(null).build();
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(d));

            FamilyDnaDto dto = service.findByFamilyId(1L).orElseThrow();

            assertThat(dto.valores()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // synthesize
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("synthesize")
    class Synthesize {

        private void stubEmptyRepos() {
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(riskRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.empty());
            when(dnaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound_throws() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.synthesize(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("IA responde JSON válido → campos del DTO mapeados correctamente")
        void validAiResponse_mapsAllFields() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family(1L)));
            stubEmptyRepos();
            when(aiProvider.generateRawResponse(any())).thenReturn(
                    "{\"valores\":[\"Amor\",\"Fe\"],\"fortalezas\":[\"Resiliencia\"]," +
                    "\"sombras\":[\"Conflictos\"],\"patrones\":[\"Reunión\"]," +
                    "\"estiloComunicacion\":\"Abierto\",\"ritmoFamiliar\":\"Semanal\"," +
                    "\"potencialOculto\":[],\"narrativaIa\":\"Familia hermosa.\"}");

            FamilyDnaDto dto = service.synthesize(1L);

            assertThat(dto.valores()).containsExactly("Amor", "Fe");
            assertThat(dto.estiloComunicacion()).isEqualTo("Abierto");
            assertThat(dto.narrativaIa()).isEqualTo("Familia hermosa.");
        }

        @Test
        @DisplayName("IA retorna null → extractJson devuelve '{}', narrativaIa es null (sin excepción)")
        void aiReturnsNull_emptyJson_noException() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family(1L)));
            stubEmptyRepos();
            when(aiProvider.generateRawResponse(any())).thenReturn(null);

            FamilyDnaDto dto = service.synthesize(1L);

            // null → extractJson → "{}" → parse OK → campos null, listas vacías
            assertThat(dto.narrativaIa()).isNull();
            assertThat(dto.valores()).isEmpty();
        }

        @Test
        @DisplayName("IA retorna texto inválido → fallback narrativaIa")
        void aiReturnsInvalidJson_fallbackNarrativa() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family(1L)));
            stubEmptyRepos();
            when(aiProvider.generateRawResponse(any())).thenReturn("esto no es json");

            FamilyDnaDto dto = service.synthesize(1L);

            assertThat(dto.narrativaIa()).contains("ADN familiar está siendo sintetizado");
        }

        @Test
        @DisplayName("JSON envuelto en markdown → extractJson lo extrae correctamente")
        void jsonWrappedInMarkdown_extracted() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family(1L)));
            stubEmptyRepos();
            when(aiProvider.generateRawResponse(any())).thenReturn(
                    "```json\n{\"narrativaIa\":\"Brillante.\",\"valores\":[]," +
                    "\"fortalezas\":[],\"sombras\":[],\"patrones\":[]}\n```");

            FamilyDnaDto dto = service.synthesize(1L);

            assertThat(dto.narrativaIa()).isEqualTo("Brillante.");
        }

        @Test
        @DisplayName("ADN existente → se actualiza el mismo objeto (no se crea uno nuevo)")
        void existingDna_updatesExistingObject() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family(1L)));
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of());
            when(riskRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            FamilyDna existing = fullDna(1L);
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(existing));
            when(dnaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(aiProvider.generateRawResponse(any())).thenReturn("{\"narrativaIa\":\"Actualizado.\"}");

            service.synthesize(1L);

            verify(dnaRepository).save(existing);
        }

        @Test
        @DisplayName("solo evaluaciones FINALIZED se incluyen en el prompt de IA")
        void onlyFinalizedEvals_includedInPrompt() {
            when(familyRepository.findById(1L)).thenReturn(Optional.of(family(1L)));
            Evaluation finalized = Evaluation.builder().id(1L).icf(72.0)
                    .status(EvaluationStatus.FINALIZED).dimensionScores(List.of()).build();
            Evaluation pending = Evaluation.builder().id(2L).icf(30.0)
                    .status(EvaluationStatus.STARTED).dimensionScores(List.of()).build();
            when(evaluationRepository.findByFamilyId(1L)).thenReturn(List.of(finalized, pending));
            when(riskRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.empty());
            when(dnaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(aiProvider.generateRawResponse(any())).thenReturn("{\"narrativaIa\":\"OK.\"}");

            service.synthesize(1L);

            verify(aiProvider).generateRawResponse(argThat(prompt ->
                    prompt.contains("72.0") && !prompt.contains("30.0")));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildDnaContextBlock
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("buildDnaContextBlock")
    class BuildDnaContextBlock {

        @Test
        @DisplayName("sin ADN registrado → retorna null")
        void noDna_returnsNull() {
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.empty());

            assertThat(service.buildDnaContextBlock(1L)).isNull();
        }

        @Test
        @DisplayName("ADN con valores → bloque contiene cabecera y sección de valores")
        void dnaWithValues_blockContainsValores() {
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(fullDna(1L)));

            String block = service.buildDnaContextBlock(1L);

            assertThat(block).contains("ADN Familiar:");
            assertThat(block).contains("Valores:");
            assertThat(block).contains("Amor");
        }

        @Test
        @DisplayName("ADN con todos los campos → bloque incluye todas las secciones")
        void dnaWithAllFields_fullBlock() {
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(fullDna(1L)));

            String block = service.buildDnaContextBlock(1L);

            assertThat(block).contains("Fortalezas:");
            assertThat(block).contains("Sombras:");
            assertThat(block).contains("Patrones:");
            assertThat(block).contains("Comunicación:");
            assertThat(block).contains("Ritmo:");
        }

        @Test
        @DisplayName("ADN sin listas ni narrativa → solo cabecera")
        void emptyDna_headerOnly() {
            FamilyDna empty = FamilyDna.builder().familyId(1L).build();
            when(dnaRepository.findByFamilyId(1L)).thenReturn(Optional.of(empty));

            String block = service.buildDnaContextBlock(1L);

            assertThat(block).isEqualTo("ADN Familiar:\n");
        }
    }
}
