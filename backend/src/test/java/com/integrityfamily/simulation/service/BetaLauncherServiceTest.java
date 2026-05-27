package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BetaLauncherService")
class BetaLauncherServiceTest {

    @Mock MemberService    memberService;
    @Mock FamilyRepository familyRepository;

    @InjectMocks BetaLauncherService service;

    private Family testFamily() {
        return Family.builder().id(10L).name("Familia Beta").familyCode("ALFA-XYZ")
                .sentinelActive(false).build();
    }

    // ── launch(email) ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("launch: lanza excepcion si no hay familia asociada al email")
    void launch_familyNotFound_throwsRuntimeException() {
        when(familyRepository.findByCreatedBy_Email("no@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.launch("no@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no@test.com");
    }

    @Test
    @DisplayName("launch: activa sentinel, crea miembro y retorna string con nombre y codigo")
    void launch_familyFound_activatesSentinelAndReturnsSummary() {
        Family family = testFamily();
        when(familyRepository.findByCreatedBy_Email("user@test.com")).thenReturn(Optional.of(family));
        // launchSimulation() llama a findById con el mismo ID
        when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.launch("user@test.com");

        verify(memberService).createMember(any());
        assertThat(family.getSentinelActive()).isTrue();
        assertThat(result).contains("Familia Beta").contains("ALFA-XYZ");
    }

    // ── launchSimulation(familyId) ────────────────────────────────────────────

    @Test
    @DisplayName("launchSimulation: lanza excepcion si la familia no existe por ID")
    void launchSimulation_familyNotFound_throwsRuntimeException() {
        when(familyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.launchSimulation(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Familia no encontrada");
    }
}
