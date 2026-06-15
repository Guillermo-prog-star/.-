package com.integrityfamily.legado.service;

import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.domain.FamilyValue;
import com.integrityfamily.legado.dto.LegacyRequest;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.legado.repository.FamilyValueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LegacyService")
class LegacyServiceTest {

    @Mock FamilyLegacyRepository legacyRepo;
    @Mock FamilyValueRepository  valueRepo;
    @InjectMocks LegacyService service;

    private static final long FAM_ID = 1L;

    private FamilyLegacy legacy() {
        return FamilyLegacy.builder().familyId(FAM_ID).build();
    }

    private LegacyRequest emptyReq() {
        return new LegacyRequest(); // @Data → sin campos seteados → null/false
    }

    // ── getOrCreate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("legado existente → retornado sin guardar")
        void found_returnedDirectly() {
            FamilyLegacy existing = legacy();
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(existing));

            FamilyLegacy result = service.getOrCreate(FAM_ID);

            assertThat(result).isSameAs(existing);
            verify(legacyRepo, never()).save(any());
        }

        @Test
        @DisplayName("sin legado → crea y guarda nuevo con familyId")
        void notFound_createsAndSaves() {
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());
            when(legacyRepo.save(any())).thenReturn(legacy());

            FamilyLegacy result = service.getOrCreate(FAM_ID);

            assertThat(result).isNotNull();
            verify(legacyRepo).save(argThat(l -> FAM_ID == l.getFamilyId()));
        }
    }

    // ── getValues ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getValues")
    class GetValues {

        @Test
        @DisplayName("delega al valueRepo ordenado por sortOrder")
        void delegatesToRepo() {
            List<FamilyValue> values = List.of(FamilyValue.builder().familyId(FAM_ID).name("Amor").build());
            when(valueRepo.findByFamilyIdOrderBySortOrder(FAM_ID)).thenReturn(values);

            assertThat(service.getValues(FAM_ID)).isSameAs(values);
        }
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("values=null → no toca valueRepo")
        void nullValues_doesNotCallValueRepo() {
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(legacy()));
            when(legacyRepo.save(any())).thenReturn(legacy());

            service.save(FAM_ID, emptyReq()); // values=null

            verifyNoInteractions(valueRepo);
        }

        @Test
        @DisplayName("values no-null → elimina antiguos y guarda nuevos")
        void withValues_deletesOldAndSavesNew() {
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(legacy()));
            when(legacyRepo.save(any())).thenReturn(legacy());

            LegacyRequest req = emptyReq();
            LegacyRequest.ValueDto v = new LegacyRequest.ValueDto();
            v.setName("Respeto");
            v.setIcon("🤝");
            v.setSortOrder(1);
            req.setValues(List.of(v));

            service.save(FAM_ID, req);

            verify(valueRepo).deleteByFamilyId(FAM_ID);
            verify(valueRepo).saveAll(argThat(list -> {
                @SuppressWarnings("unchecked")
                List<FamilyValue> vals = (List<FamilyValue>) list;
                return vals.size() == 1 && "Respeto".equals(vals.get(0).getName());
            }));
        }

        @Test
        @DisplayName("carta sellada → letterContent NO se sobreescribe")
        void sealedLetter_contentNotOverwritten() {
            FamilyLegacy existing = FamilyLegacy.builder()
                    .familyId(FAM_ID).letterSealed(true).letterContent("Contenido original").build();
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(existing));
            when(legacyRepo.save(any())).thenReturn(existing);

            LegacyRequest req = emptyReq();
            req.setLetterContent("Contenido nuevo");
            req.setLetterSealed(true);

            service.save(FAM_ID, req);

            assertThat(existing.getLetterContent()).isEqualTo("Contenido original");
        }

        @Test
        @DisplayName("carta no sellada → letterContent SÍ se sobreescribe")
        void unsealedLetter_contentOverwritten() {
            FamilyLegacy existing = FamilyLegacy.builder()
                    .familyId(FAM_ID).letterSealed(false).letterContent("Viejo").build();
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(existing));
            when(legacyRepo.save(any())).thenReturn(existing);

            LegacyRequest req = emptyReq();
            req.setLetterContent("Nuevo");
            req.setLetterSealed(false);

            service.save(FAM_ID, req);

            assertThat(existing.getLetterContent()).isEqualTo("Nuevo");
        }

        @Test
        @DisplayName("legado existente → encontrado en repo y misión/visión actualizados")
        void existingLegacy_updatedAndSaved() {
            FamilyLegacy existing = legacy();
            when(legacyRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(existing));
            when(legacyRepo.save(any())).thenReturn(existing);

            LegacyRequest req = emptyReq();
            req.setFamilyMission("Crecer juntos");
            req.setFamilyVision("Familia resiliente");

            service.save(FAM_ID, req);

            assertThat(existing.getFamilyMission()).isEqualTo("Crecer juntos");
            assertThat(existing.getFamilyVision()).isEqualTo("Familia resiliente");
        }
    }
}
