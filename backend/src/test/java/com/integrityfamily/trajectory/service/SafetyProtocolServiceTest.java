package com.integrityfamily.trajectory.service;

import com.integrityfamily.common.service.UserNotificationService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.FamilyRiskTrajectory;
import com.integrityfamily.domain.RiskMacrodomain;
import com.integrityfamily.domain.RiskTrajectory;
import com.integrityfamily.domain.SafetyProtocolActivation;
import com.integrityfamily.domain.repository.FamilyRiskTrajectoryRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.SafetyProtocolActivationRepository;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.ActivateSafetyProtocolRequest;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.CloseSafetyProtocolRequest;
import com.integrityfamily.trajectory.dto.TrajectoryDtos.SafetyProtocolDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafetyProtocolService — Unit Tests")
class SafetyProtocolServiceTest {

    @Mock SafetyProtocolActivationRepository repo;
    @Mock FamilyRiskTrajectoryRepository familyTrajectoryRepo;
    @Mock MemberRepository memberRepo;
    @Mock UserNotificationService notificationService;

    @InjectMocks SafetyProtocolService service;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Family aFamily(Long id) {
        Family f = new Family();
        f.setId(id);
        f.setName("Familia Test");
        return f;
    }

    private RiskTrajectory aTrajectory() {
        return RiskTrajectory.builder()
            .id(1L).code("IDEACION_SUICIDA").name("Ideación suicida")
            .macrodomain(RiskMacrodomain.SALUD_MENTAL).severityDefault("CRITICAL")
            .requiresSafetyProtocol(true).active(true)
            .build();
    }

    private FamilyRiskTrajectory aFamilyTrajectory(Family family) {
        return FamilyRiskTrajectory.builder()
            .id(100L).family(family).trajectory(aTrajectory())
            .build();
    }

    private FamilyMember aResponsible(Long id, Family family) {
        return FamilyMember.builder().id(id).fullName("María Pérez").family(family).build();
    }

    // ─── activate() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("activa el protocolo y notifica a la familia")
        void activatesAndNotifies() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, family);
            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(
                5L, "Contactar línea de crisis", LocalDate.now().plusDays(1));

            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));
            when(memberRepo.findById(5L)).thenReturn(Optional.of(responsible));
            when(repo.save(any(SafetyProtocolActivation.class))).thenAnswer(inv -> {
                SafetyProtocolActivation a = inv.getArgument(0);
                a.setId(1L);
                a.setCreatedAt(LocalDateTime.now());
                return a;
            });

            SafetyProtocolDto result = service.activate(100L, req, "guardian@test.com");

            assertThat(result.responsibleId()).isEqualTo(5L);
            assertThat(result.responsibleName()).isEqualTo("María Pérez");
            assertThat(result.initialAction()).isEqualTo("Contactar línea de crisis");
            assertThat(result.closed()).isFalse();
            verify(notificationService).push(any(Family.class), isNull(), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("lanza excepción si la trayectoria familiar no existe")
        void throwsIfFamilyTrajectoryNotFound() {
            when(familyTrajectoryRepo.findById(999L)).thenReturn(Optional.empty());
            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(5L, "Acción", LocalDate.now());

            assertThatThrownBy(() -> service.activate(999L, req, "guardian@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trayectoria familiar no encontrada");
        }

        @Test
        @DisplayName("lanza excepción si no se indica responsable")
        void throwsIfResponsibleMissing() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));

            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(null, "Acción", LocalDate.now());

            assertThatThrownBy(() -> service.activate(100L, req, "guardian@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("responsable");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si la acción inicial está en blanco")
        void throwsIfInitialActionBlank() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));

            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(5L, "  ", LocalDate.now());

            assertThatThrownBy(() -> service.activate(100L, req, "guardian@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("acción inicial");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si falta la fecha de seguimiento")
        void throwsIfFollowUpDateMissing() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));

            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(5L, "Acción", null);

            assertThatThrownBy(() -> service.activate(100L, req, "guardian@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seguimiento");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el responsable no existe")
        void throwsIfResponsibleNotFound() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));
            when(memberRepo.findById(5L)).thenReturn(Optional.empty());

            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(5L, "Acción", LocalDate.now());

            assertThatThrownBy(() -> service.activate(100L, req, "guardian@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Miembro responsable no encontrado");
        }

        @Test
        @DisplayName("lanza excepción si el responsable pertenece a otra familia")
        void throwsIfResponsibleFromDifferentFamily() {
            Family family = aFamily(10L);
            Family otherFamily = aFamily(20L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, otherFamily);

            when(familyTrajectoryRepo.findById(100L)).thenReturn(Optional.of(frt));
            when(memberRepo.findById(5L)).thenReturn(Optional.of(responsible));

            ActivateSafetyProtocolRequest req = new ActivateSafetyProtocolRequest(5L, "Acción", LocalDate.now());

            assertThatThrownBy(() -> service.activate(100L, req, "guardian@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a esta familia");
            verify(repo, never()).save(any());
        }
    }

    // ─── getByFamilyTrajectory() ────────────────────────────────────────────────

    @Nested
    @DisplayName("getByFamilyTrajectory()")
    class GetByFamilyTrajectory {

        @Test
        @DisplayName("retorna las activaciones mapeadas a DTO")
        void returnsMappedActivations() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, family);
            SafetyProtocolActivation activation = SafetyProtocolActivation.builder()
                .id(1L).familyTrajectory(frt).responsible(responsible)
                .initialAction("Acción").followUpDate(LocalDate.now())
                .closed(false).createdAt(LocalDateTime.now()).build();

            when(repo.findByFamilyTrajectoryIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(activation));

            List<SafetyProtocolDto> result = service.getByFamilyTrajectory(100L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).responsibleName()).isEqualTo("María Pérez");
        }
    }

    // ─── close() ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("close()")
    class Close {

        @Test
        @DisplayName("cierra la activación y registra notas de resolución")
        void closesActivation() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, family);
            SafetyProtocolActivation activation = SafetyProtocolActivation.builder()
                .id(1L).familyTrajectory(frt).responsible(responsible)
                .initialAction("Acción").followUpDate(LocalDate.now())
                .closed(false).createdAt(LocalDateTime.now()).build();

            when(repo.findById(1L)).thenReturn(Optional.of(activation));
            when(repo.save(any(SafetyProtocolActivation.class))).thenAnswer(inv -> inv.getArgument(0));

            CloseSafetyProtocolRequest req = new CloseSafetyProtocolRequest("Situación estabilizada");
            SafetyProtocolDto result = service.close(100L, 1L, req);

            assertThat(result.closed()).isTrue();
            assertThat(result.resolutionNotes()).isEqualTo("Situación estabilizada");
        }

        @Test
        @DisplayName("lanza excepción si la activación no existe")
        void throwsIfActivationNotFound() {
            when(repo.findById(99L)).thenReturn(Optional.empty());
            CloseSafetyProtocolRequest req = new CloseSafetyProtocolRequest(null);

            assertThatThrownBy(() -> service.close(100L, 99L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Activación no encontrada");
        }

        @Test
        @DisplayName("lanza excepción si la activación no pertenece a la trayectoria indicada")
        void throwsIfActivationBelongsToDifferentTrajectory() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, family);
            SafetyProtocolActivation activation = SafetyProtocolActivation.builder()
                .id(1L).familyTrajectory(frt).responsible(responsible)
                .initialAction("Acción").followUpDate(LocalDate.now())
                .closed(false).createdAt(LocalDateTime.now()).build();

            when(repo.findById(1L)).thenReturn(Optional.of(activation));
            CloseSafetyProtocolRequest req = new CloseSafetyProtocolRequest(null);

            assertThatThrownBy(() -> service.close(999L, 1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece a esta trayectoria");
        }

        @Test
        @DisplayName("lanza excepción si no se indican notas de resolución (null)")
        void throwsIfResolutionNotesMissing() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, family);
            SafetyProtocolActivation activation = SafetyProtocolActivation.builder()
                .id(1L).familyTrajectory(frt).responsible(responsible)
                .initialAction("Acción").followUpDate(LocalDate.now())
                .closed(false).createdAt(LocalDateTime.now()).build();

            when(repo.findById(1L)).thenReturn(Optional.of(activation));
            CloseSafetyProtocolRequest req = new CloseSafetyProtocolRequest(null);

            assertThatThrownBy(() -> service.close(100L, 1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notas de resolución");
            verify(repo, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si las notas de resolución están en blanco")
        void throwsIfResolutionNotesBlank() {
            Family family = aFamily(10L);
            FamilyRiskTrajectory frt = aFamilyTrajectory(family);
            FamilyMember responsible = aResponsible(5L, family);
            SafetyProtocolActivation activation = SafetyProtocolActivation.builder()
                .id(1L).familyTrajectory(frt).responsible(responsible)
                .initialAction("Acción").followUpDate(LocalDate.now())
                .closed(false).createdAt(LocalDateTime.now()).build();

            when(repo.findById(1L)).thenReturn(Optional.of(activation));
            CloseSafetyProtocolRequest req = new CloseSafetyProtocolRequest("   ");

            assertThatThrownBy(() -> service.close(100L, 1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("notas de resolución");
            verify(repo, never()).save(any());
        }
    }
}
