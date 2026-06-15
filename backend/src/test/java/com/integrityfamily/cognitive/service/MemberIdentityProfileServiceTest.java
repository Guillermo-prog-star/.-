package com.integrityfamily.cognitive.service;

import com.integrityfamily.domain.MemberIdentityProfile;
import com.integrityfamily.domain.repository.MemberIdentityProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MemberIdentityProfileService")
class MemberIdentityProfileServiceTest {

    @Mock MemberIdentityProfileRepository repository;
    @InjectMocks MemberIdentityProfileService service;

    private static final long MEM_ID = 5L;

    private MemberIdentityProfile profile() {
        return MemberIdentityProfile.builder().memberId(MEM_ID).build();
    }

    // ── getOrCreate ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("perfil existente → retornado directamente sin guardar")
        void existingProfile_returnedWithoutSave() {
            MemberIdentityProfile existing = profile();
            when(repository.findByMemberId(MEM_ID)).thenReturn(Optional.of(existing));

            MemberIdentityProfile result = service.getOrCreate(MEM_ID);

            assertThat(result).isSameAs(existing);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sin perfil → crea y guarda nuevo con memberId")
        void noProfile_createsAndSaves() {
            when(repository.findByMemberId(MEM_ID)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(profile());

            MemberIdentityProfile result = service.getOrCreate(MEM_ID);

            assertThat(result).isNotNull();
            verify(repository).save(argThat(p -> MEM_ID == p.getMemberId()));
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("todos los campos non-null → todos actualizados y guardados")
        void allFieldsProvided_allUpdated() {
            MemberIdentityProfile p = profile();
            when(repository.findByMemberId(MEM_ID)).thenReturn(Optional.of(p));
            when(repository.save(p)).thenReturn(p);

            service.update(MEM_ID, "ANALYTICAL", 4, 5, "LOW", "{}", "[]");

            assertThat(p.getCommunicationStyle()).isEqualTo("ANALYTICAL");
            assertThat(p.getReflexivityLevel()).isEqualTo(4);
            assertThat(p.getEmotionalSensitivity()).isEqualTo(5);
            assertThat(p.getChangeResistance()).isEqualTo("LOW");
            assertThat(p.getEvasionPatterns()).isEqualTo("{}");
            assertThat(p.getMotivators()).isEqualTo("[]");
            verify(repository).save(p);
        }

        @Test
        @DisplayName("todos los campos null → ningún campo modificado")
        void allFieldsNull_nothingChanged() {
            MemberIdentityProfile p = profile();
            String originalStyle = p.getCommunicationStyle();
            when(repository.findByMemberId(MEM_ID)).thenReturn(Optional.of(p));
            when(repository.save(p)).thenReturn(p);

            service.update(MEM_ID, null, null, null, null, null, null);

            assertThat(p.getCommunicationStyle()).isEqualTo(originalStyle);
            verify(repository).save(p); // guardado aunque no haya cambios
        }

        @Test
        @DisplayName("solo communicationStyle non-null → solo ese campo cambia")
        void partialUpdate_onlySpecifiedFieldChanged() {
            MemberIdentityProfile p = profile();
            int originalReflexivity = p.getReflexivityLevel();
            when(repository.findByMemberId(MEM_ID)).thenReturn(Optional.of(p));
            when(repository.save(p)).thenReturn(p);

            service.update(MEM_ID, "EMPATHIC", null, null, null, null, null);

            assertThat(p.getCommunicationStyle()).isEqualTo("EMPATHIC");
            assertThat(p.getReflexivityLevel()).isEqualTo(originalReflexivity);
        }

        @Test
        @DisplayName("perfil no existe → crea primero, luego actualiza")
        void profileNotFound_createdThenUpdated() {
            when(repository.findByMemberId(MEM_ID)).thenReturn(Optional.empty());
            MemberIdentityProfile newProfile = profile();
            when(repository.save(any())).thenReturn(newProfile);

            service.update(MEM_ID, "DIRECT", null, null, null, null, null);

            verify(repository, times(2)).save(any()); // 1 create + 1 update
        }
    }
}
