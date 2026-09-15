package com.integrityfamily.familyhome.adapter;

import com.integrityfamily.bitacora.dto.SprintDtos.CreateSprintRequest;
import com.integrityfamily.bitacora.dto.SprintDtos.SprintResponse;
import com.integrityfamily.bitacora.service.SprintService;
import com.integrityfamily.dto.home.AcceptFirstSprintRequest;
import com.integrityfamily.familyhome.application.exception.FamilyHomeDataIncompleteException;
import com.integrityfamily.familyhome.port.SprintSummarySnapshot;
import com.integrityfamily.familyhome.security.FamilyIdentifierBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyInterventionCommandPortAdapter — Unit Tests")
class FamilyInterventionCommandPortAdapterTest {

    @Mock SprintService sprintService;
    @Mock FamilyIdentifierBridge idBridge;

    FamilyInterventionCommandPortAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FamilyInterventionCommandPortAdapter(sprintService, idBridge);
    }

    @Test
    @DisplayName("familia sin mapeo a Long → FamilyHomeDataIncompleteException")
    void unknownFamily_throws() {
        UUID familyId = UUID.randomUUID();
        when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.acceptFirstSprint(familyId, new AcceptFirstSprintRequest(null, null, null, null)))
                .isInstanceOf(FamilyHomeDataIncompleteException.class);
    }

    @Test
    @DisplayName("objective vacío → aplica el objetivo por defecto antes de delegar en SprintService")
    void blankObjective_getsDefault() {
        UUID familyId = UUID.randomUUID();
        when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
        when(sprintService.createSprint(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SprintResponse(10L, 5L, "Primer sprint de transformación familiar",
                        "comunicacion", 7, LocalDate.now(), LocalDate.now().plusDays(7),
                        "ACTIVE", List.of(), List.of(), null, null, null));

        SprintSummarySnapshot result = adapter.acceptFirstSprint(familyId, new AcceptFirstSprintRequest("  ", null, null, null));

        ArgumentCaptor<CreateSprintRequest> captor = ArgumentCaptor.forClass(CreateSprintRequest.class);
        org.mockito.Mockito.verify(sprintService).createSprint(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertThat(captor.getValue().objective()).isEqualTo("Primer sprint de transformación familiar");
        assertThat(result.title()).isEqualTo("Primer sprint de transformación familiar");
        assertThat(result.totalMissions()).isZero();
        assertThat(result.completedMissions()).isZero();
    }

    @Test
    @DisplayName("respeta el objective explícito del cliente cuando viene informado")
    void explicitObjective_isPreserved() {
        UUID familyId = UUID.randomUUID();
        when(idBridge.resolveFamilyId(familyId)).thenReturn(Optional.of(5L));
        when(sprintService.createSprint(org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SprintResponse(11L, 5L, "Mejorar la comunicación en la cena",
                        "comunicacion", 7, LocalDate.now(), LocalDate.now().plusDays(7),
                        "ACTIVE", List.of(), List.of(), null, null, null));

        adapter.acceptFirstSprint(familyId, new AcceptFirstSprintRequest("Mejorar la comunicación en la cena", "comunicacion", 7, List.of("Cenar sin celulares")));

        ArgumentCaptor<CreateSprintRequest> captor = ArgumentCaptor.forClass(CreateSprintRequest.class);
        org.mockito.Mockito.verify(sprintService).createSprint(org.mockito.ArgumentMatchers.eq(5L), captor.capture());
        assertThat(captor.getValue().objective()).isEqualTo("Mejorar la comunicación en la cena");
        assertThat(captor.getValue().missions()).containsExactly("Cenar sin celulares");
    }
}
