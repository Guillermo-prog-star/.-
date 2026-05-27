package com.integrityfamily.admin.service;

import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchdogIntegrityService")
class WatchdogIntegrityServiceTest {

    @Mock FamilyRepository       familyRepository;
    @Mock SecurityWatchdogService watchdogService;
    @Mock AdminAlertRepository   alertRepository;

    @InjectMocks WatchdogIntegrityService service;

    @Test
    @DisplayName("lanza RuntimeException cuando no hay familias con codigo IF-CO-*")
    void testWatchdogActivation_noAlfaFamily_throwsRuntimeException() {
        Family nonAlfa = Family.builder().id(1L).familyCode("ALFA-001").build();
        when(familyRepository.findAll()).thenReturn(List.of(nonAlfa));

        assertThatThrownBy(() -> service.testWatchdogActivation())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Alfa");
    }

    @Test
    @DisplayName("activa sentinel, invoca scan y retorna mensaje de exito cuando encuentra familia IF-CO-*")
    void testWatchdogActivation_alfaFamilyExists_activatesAndReturnsSuccess() {
        Family alfa = Family.builder().id(2L).familyCode("IF-CO-001").sentinelActive(false)
                .currentMilestone("W1").build();
        when(familyRepository.findAll()).thenReturn(List.of(alfa));
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(watchdogService).scanForAnomalies();

        AdminAlert alert = AdminAlert.builder().id(1L).title("CRISIS ACTIVA: IF-CO-001").build();
        when(alertRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(alert));

        String result = service.testWatchdogActivation();

        assertThat(alfa.getSentinelActive()).isTrue();
        verify(watchdogService).scanForAnomalies();
        assertThat(result).contains("IF-CO-001");
    }
}
