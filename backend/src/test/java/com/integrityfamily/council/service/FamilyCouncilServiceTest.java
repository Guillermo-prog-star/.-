package com.integrityfamily.council.service;

import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.context.service.FamilyContextEngine;
import com.integrityfamily.council.dto.CouncilRequest;
import com.integrityfamily.council.dto.CouncilResponse;
import com.integrityfamily.dna.service.FamilyDnaService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.tree.service.FamilyTreeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyCouncilService")
class FamilyCouncilServiceTest {

    @Mock FamilyRepository       familyRepository;
    @Mock FamilyLegacyRepository legacyRepository;
    @Mock FamilyDnaService       dnaService;
    @Mock FamilyContextEngine    contextEngine;
    @Mock FamilyTreeService      treeService;
    @Mock AiProvider             aiProvider;
    @InjectMocks FamilyCouncilService service;

    private static final long FAM_ID = 1L;

    private final Family family = Family.builder().id(FAM_ID).name("García").build();
    private final CouncilRequest req = new CouncilRequest("¿Cómo resolvemos el conflicto?", "CONFLICTO", null);

    /** Stubs mínimos para que consult() llegue hasta aiProvider sin fallar. */
    private void stubConsult(String dnaBlock, String contextBlock, String heritageBlock) {
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
        lenient().when(legacyRepository.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
        when(dnaService.buildDnaContextBlock(FAM_ID)).thenReturn(dnaBlock);
        when(contextEngine.buildContextBlock(FAM_ID)).thenReturn(contextBlock);
        when(treeService.buildTreeContextBlock(FAM_ID)).thenReturn(heritageBlock);
        when(aiProvider.generateRawResponse(any())).thenReturn("Respuesta del Consejo.");
    }

    // ── consulta básica ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("consult")
    class Consult {

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound_throws() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.consult(FAM_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(FAM_ID));
        }

        @Test
        @DisplayName("respuesta con espacios → almacenada stripped")
        void rawResponseStripped() {
            stubConsult(null, null, null);
            when(aiProvider.generateRawResponse(any())).thenReturn("  Consejo trimmed.  ");

            CouncilResponse resp = service.consult(FAM_ID, req);

            assertThat(resp.councilResponse()).isEqualTo("Consejo trimmed.");
        }

        @Test
        @DisplayName("familyId y familyName propagados al response")
        void familyDataPropagated() {
            stubConsult(null, null, null);

            CouncilResponse resp = service.consult(FAM_ID, req);

            assertThat(resp.familyId()).isEqualTo(FAM_ID);
            assertThat(resp.familyName()).isEqualTo("García");
            assertThat(resp.question()).isEqualTo(req.question());
            assertThat(resp.topic()).isEqualTo(req.topic());
        }
    }

    // ── hasConstitution / hasDna ──────────────────────────────────────────────

    @Nested
    @DisplayName("hasConstitution y hasDna")
    class Flags {

        @Test
        @DisplayName("sin legacy → hasConstitution=false")
        void noLegacy_hasConstitutionFalse() {
            stubConsult(null, null, null);

            assertThat(service.consult(FAM_ID, req).hasConstitution()).isFalse();
        }

        @Test
        @DisplayName("legacy sin foundingPrinciple → hasConstitution=false")
        void legacyWithoutFoundingPrinciple_hasConstitutionFalse() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(legacyRepository.findByFamilyId(FAM_ID))
                    .thenReturn(Optional.of(FamilyLegacy.builder().familyId(FAM_ID).build()));
            when(dnaService.buildDnaContextBlock(FAM_ID)).thenReturn(null);
            when(contextEngine.buildContextBlock(FAM_ID)).thenReturn(null);
            when(treeService.buildTreeContextBlock(FAM_ID)).thenReturn(null);
            when(aiProvider.generateRawResponse(any())).thenReturn("resp");

            assertThat(service.consult(FAM_ID, req).hasConstitution()).isFalse();
        }

        @Test
        @DisplayName("legacy con foundingPrinciple → hasConstitution=true")
        void legacyWithFoundingPrinciple_hasConstitutionTrue() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(legacyRepository.findByFamilyId(FAM_ID))
                    .thenReturn(Optional.of(FamilyLegacy.builder()
                            .familyId(FAM_ID).foundingPrinciple("Integridad ante todo").build()));
            when(dnaService.buildDnaContextBlock(FAM_ID)).thenReturn(null);
            when(contextEngine.buildContextBlock(FAM_ID)).thenReturn(null);
            when(treeService.buildTreeContextBlock(FAM_ID)).thenReturn(null);
            when(aiProvider.generateRawResponse(any())).thenReturn("resp");

            assertThat(service.consult(FAM_ID, req).hasConstitution()).isTrue();
        }

        @Test
        @DisplayName("dnaBlock=null → hasDna=false")
        void nullDnaBlock_hasDnaFalse() {
            stubConsult(null, null, null);

            assertThat(service.consult(FAM_ID, req).hasDna()).isFalse();
        }

        @Test
        @DisplayName("dnaBlock no-null → hasDna=true")
        void nonNullDnaBlock_hasDnaTrue() {
            stubConsult("ADN de la familia", null, null);

            assertThat(service.consult(FAM_ID, req).hasDna()).isTrue();
        }
    }

    // ── sourcesUsed ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sourcesUsed")
    class SourcesUsed {

        @Test
        @DisplayName("sin fuentes → lista vacía")
        void noSources_emptyList() {
            stubConsult(null, null, null);

            assertThat(service.consult(FAM_ID, req).sourcesUsed()).isEmpty();
        }

        @Test
        @DisplayName("dnaBlock → 'ADN Familiar' en sources")
        void dnaBlock_dnaInSources() {
            stubConsult("DNA", null, null);

            assertThat(service.consult(FAM_ID, req).sourcesUsed()).contains("ADN Familiar");
        }

        @Test
        @DisplayName("contextBlock → 'Estado actual' en sources")
        void contextBlock_contextInSources() {
            stubConsult(null, "contexto", null);

            assertThat(service.consult(FAM_ID, req).sourcesUsed()).contains("Estado actual");
        }

        @Test
        @DisplayName("heritageBlock → 'Herencia generacional' en sources")
        void heritageBlock_heritageInSources() {
            stubConsult(null, null, "herencia");

            assertThat(service.consult(FAM_ID, req).sourcesUsed()).contains("Herencia generacional");
        }

        @Test
        @DisplayName("legacy con misión → 'Misión y visión' en sources")
        void legacyWithMission_misionInSources() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(legacyRepository.findByFamilyId(FAM_ID))
                    .thenReturn(Optional.of(FamilyLegacy.builder()
                            .familyId(FAM_ID).familyMission("Crecer juntos").build()));
            when(dnaService.buildDnaContextBlock(FAM_ID)).thenReturn(null);
            when(contextEngine.buildContextBlock(FAM_ID)).thenReturn(null);
            when(treeService.buildTreeContextBlock(FAM_ID)).thenReturn(null);
            when(aiProvider.generateRawResponse(any())).thenReturn("resp");

            assertThat(service.consult(FAM_ID, req).sourcesUsed()).contains("Misión y visión");
        }
    }
}
