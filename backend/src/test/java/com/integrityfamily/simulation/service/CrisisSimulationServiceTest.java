package com.integrityfamily.simulation.service;

import com.integrityfamily.domain.AdminAlert;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.AdminAlertRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrisisSimulationService")
class CrisisSimulationServiceTest {

    @Mock FamilyRepository    familyRepository;
    @Mock AdminAlertRepository alertRepository;

    @InjectMocks CrisisSimulationService service;

    @Test
    @DisplayName("activa sentinelActive y crea alerta CRITICAL cuando existe una familia")
    void triggerGlobalCrisisTest_familyExists_activatesAndCreatesAlert() {
        Family family = Family.builder().id(1L).familyCode("ALFA-001").sentinelActive(false).build();
        when(familyRepository.findAll()).thenReturn(List.of(family));
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.triggerGlobalCrisisTest();

        assertThat(family.getSentinelActive()).isTrue();

        ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
        verify(alertRepository).save(captor.capture());
        AdminAlert saved = captor.getValue();
        assertThat(saved.getTitle()).contains("ALFA-001");
        assertThat(saved.getSeverity()).isEqualTo("CRITICAL");
        assertThat(result).contains("activada");
    }

    @Test
    @DisplayName("lanza excepcion cuando no hay familias registradas")
    void triggerGlobalCrisisTest_noFamilies_throwsException() {
        when(familyRepository.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.triggerGlobalCrisisTest())
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
