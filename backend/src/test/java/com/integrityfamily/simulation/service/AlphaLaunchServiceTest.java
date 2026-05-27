package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlphaLaunchService")
class AlphaLaunchServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock UserRepository   userRepository;

    @InjectMocks AlphaLaunchService service;

    @Test
    @DisplayName("lanza excepcion cuando el creador no existe")
    void provisionAlphaFamilies_creatorNotFound_throwsRuntimeException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.provisionAlphaFamilies(3, "unknown@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("unknown@test.com");
    }

    @Test
    @DisplayName("crea exactamente N familias alfa y las retorna en lista")
    void provisionAlphaFamilies_validCreator_createsRequestedNumberOfFamilies() {
        User creator = User.builder().id(1L).email("admin@integrity.ia").build();
        when(userRepository.findByEmail("admin@integrity.ia")).thenReturn(Optional.of(creator));
        when(familyRepository.save(any(Family.class))).thenAnswer(inv -> {
            Family f = inv.getArgument(0);
            // simular ID asignado por BD
            return Family.builder().id((long)(Math.random() * 1000))
                    .name(f.getName()).familyCode(f.getFamilyCode())
                    .currentMilestone(f.getCurrentMilestone()).createdBy(f.getCreatedBy())
                    .build();
        });

        List<Family> result = service.provisionAlphaFamilies(3, "admin@integrity.ia");

        assertThat(result).hasSize(3);
        verify(familyRepository, times(3)).save(any(Family.class));

        // Todas deben tener el milestone inicial y el creador asignado
        result.forEach(f -> {
            assertThat(f.getCurrentMilestone()).isEqualTo("MES_00_DIAGNOSTICO");
            assertThat(f.getCreatedBy()).isEqualTo(creator);
        });
    }
}
