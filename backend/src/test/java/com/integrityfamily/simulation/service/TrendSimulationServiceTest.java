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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendSimulationService")
class TrendSimulationServiceTest {

    @Mock FamilyRepository    familyRepository;
    @Mock AdminAlertRepository alertRepository;

    @InjectMocks TrendSimulationService service;

    private Family family(long id) {
        return Family.builder().id(id).familyCode("FAM-" + id).currentMilestone("W1").build();
    }

    @Test
    @DisplayName("con 0 familias guarda alerta con conteo 0 y severity CRITICAL")
    void triggerMassiveMilestoneFailure_noFamilies_savesAlertWithZeroCount() {
        when(familyRepository.findAll()).thenReturn(List.of());

        String result = service.triggerMassiveMilestoneFailure();

        ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
        verify(alertRepository).save(captor.capture());
        AdminAlert saved = captor.getValue();
        assertThat(saved.getSeverity()).isEqualTo("CRITICAL");
        // mensaje contiene "0 de 0"
        assertThat(saved.getMessage()).contains("0");
        assertThat(result).contains("0");
    }

    @Test
    @DisplayName("con 3 familias afecta 2 (indices 1 y 2) y actualiza su hito a STALLED")
    void triggerMassiveMilestoneFailure_threeFamilies_affectsTwoSkipsIndexZero() {
        Family f0 = family(1L);
        Family f1 = family(2L);
        Family f2 = family(3L);
        when(familyRepository.findAll()).thenReturn(List.of(f0, f1, f2));
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.triggerMassiveMilestoneFailure();

        // f0 (i=0): i%3==0 → no afectado
        assertThat(f0.getCurrentMilestone()).isNotEqualTo("MES_00_STALLED");
        // f1 (i=1), f2 (i=2): afectadas
        assertThat(f1.getCurrentMilestone()).isEqualTo("MES_00_STALLED");
        assertThat(f2.getCurrentMilestone()).isEqualTo("MES_00_STALLED");

        ArgumentCaptor<AdminAlert> captor = ArgumentCaptor.forClass(AdminAlert.class);
        verify(alertRepository).save(captor.capture());
        // 2 de 3 afectadas
        assertThat(captor.getValue().getMessage()).contains("2");
        assertThat(result).contains("2");
    }
}
