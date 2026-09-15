package com.integrityfamily.familyhome.security;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyIdentifierBridge — Unit Tests")
class FamilyIdentifierBridgeTest {

    private static final String SECRET_A = "test-secret-a";
    private static final String SECRET_B = "test-secret-b";

    @Mock FamilyRepository familyRepository;
    @Mock UserRepository userRepository;

    private FamilyIdentifierBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new FamilyIdentifierBridge(familyRepository, userRepository, SECRET_A);
    }

    @Nested
    @DisplayName("toFamilyUuid() / toUserUuid()")
    class Derivation {

        @Test
        @DisplayName("es determinista: el mismo id siempre produce el mismo UUID")
        void deterministic() {
            assertThat(bridge.toFamilyUuid(42L)).isEqualTo(bridge.toFamilyUuid(42L));
            assertThat(bridge.toUserUuid(42L)).isEqualTo(bridge.toUserUuid(42L));
        }

        @Test
        @DisplayName("ids distintos producen UUID distintos")
        void distinctIdsProduceDistinctUuids() {
            assertThat(bridge.toFamilyUuid(1L)).isNotEqualTo(bridge.toFamilyUuid(2L));
        }

        @Test
        @DisplayName("un mismo id numérico produce UUID distinto para familia y para usuario (namespaces separados)")
        void familyAndUserNamespacesDoNotCollide() {
            assertThat(bridge.toFamilyUuid(7L)).isNotEqualTo(bridge.toUserUuid(7L));
        }

        @Test
        @DisplayName("secretos distintos producen UUID distintos para el mismo id (no es un salt público)")
        void differentSecretsProduceDifferentUuids() {
            FamilyIdentifierBridge other = new FamilyIdentifierBridge(familyRepository, userRepository, SECRET_B);
            assertThat(bridge.toFamilyUuid(42L)).isNotEqualTo(other.toFamilyUuid(42L));
        }

        @Test
        @DisplayName("el UUID resultante tiene formato RFC 4122 versión 5")
        void uuidHasVersion5Format() {
            UUID uuid = bridge.toFamilyUuid(42L);
            assertThat(uuid.version()).isEqualTo(5);
            assertThat(uuid.variant()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("resolveFamilyId() / resolveUserId()")
    class Resolution {

        @Test
        @DisplayName("resuelve el Long original a partir del UUID derivado, cargando el índice bajo demanda")
        void resolvesFamilyIdViaLazyIndex() {
            Family family = Family.builder().id(42L).build();
            when(familyRepository.findAll()).thenReturn(List.of(family));

            UUID publicId = bridge.toFamilyUuid(42L);
            Optional<Long> resolved = bridge.resolveFamilyId(publicId);

            assertThat(resolved).contains(42L);
        }

        @Test
        @DisplayName("UUID desconocido (no corresponde a ninguna familia) → Optional vacío")
        void unknownUuidResolvesToEmpty() {
            when(familyRepository.findAll()).thenReturn(List.of());

            Optional<Long> resolved = bridge.resolveFamilyId(UUID.randomUUID());

            assertThat(resolved).isEmpty();
        }

        @Test
        @DisplayName("resuelve el userId original a partir del UUID derivado")
        void resolvesUserIdViaLazyIndex() {
            User user = User.builder().id(9L).build();
            when(userRepository.findAll()).thenReturn(List.of(user));

            UUID publicId = bridge.toUserUuid(9L);
            Optional<Long> resolved = bridge.resolveUserId(publicId);

            assertThat(resolved).contains(9L);
        }
    }
}
