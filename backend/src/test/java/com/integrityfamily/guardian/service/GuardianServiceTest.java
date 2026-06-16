package com.integrityfamily.guardian.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.guardian.domain.FamilyMission;
import com.integrityfamily.guardian.domain.MissionCategory;
import com.integrityfamily.guardian.domain.MissionStatus;
import com.integrityfamily.guardian.dto.ActivateMissionRequest;
import com.integrityfamily.guardian.dto.MissionDto;
import com.integrityfamily.guardian.dto.VoteRequest;
import com.integrityfamily.guardian.repository.FamilyMissionRepository;
import com.integrityfamily.guardian.repository.GuardianVoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GuardianService")
class GuardianServiceTest {

    @Mock private FamilyRepository       familyRepository;
    @Mock private MemberRepository       memberRepository;
    @Mock private GuardianVoteRepository voteRepository;
    @Mock private FamilyMissionRepository missionRepository;

    @InjectMocks
    private GuardianService service;

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private Family   family;
    private FamilyMember memberA;   // id=1
    private FamilyMember memberB;   // id=2
    private FamilyMember memberC;   // id=3

    @BeforeEach
    void setUp() {
        memberA = FamilyMember.builder().id(1L).fullName("Ana").build();
        memberB = FamilyMember.builder().id(2L).fullName("Bruno").build();
        memberC = FamilyMember.builder().id(3L).fullName("Carmen").build();

        family = Family.builder()
                .id(10L)
                .name("Familia Test")
                .members(new ArrayList<>(List.of(memberA, memberB, memberC)))
                .participationScore(0)
                .build();

        // Assign family reference back (bidirectional)
        memberA.setFamily(family);
        memberB.setFamily(family);
        memberC.setFamily(family);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("vote()")
    class Vote {

        @Test
        @DisplayName("primer voto crea un GuardianVote nuevo con el candidato correcto")
        void firstVote_createsNewVote() {
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberA));
            when(memberRepository.findById(2L)).thenReturn(Optional.of(memberB));
            when(voteRepository.findByFamilyIdAndVoterId(10L, 1L)).thenReturn(Optional.empty());
            when(voteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // Stub para resolveGuardianIfMajority
            when(voteRepository.countVotesByFamilyGroupedByNominated(10L)).thenReturn(List.of());
            // Stub para getStatus
            when(voteRepository.countByFamilyId(10L)).thenReturn(0L);
            when(voteRepository.existsByFamilyIdAndVoterId(10L, 1L)).thenReturn(false);
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(missionRepository.countByFamilyIdAndStatus(10L, MissionStatus.COMPLETED)).thenReturn(0L);

            service.vote(10L, new VoteRequest(1L, 2L));

            ArgumentCaptor<com.integrityfamily.guardian.domain.GuardianVote> captor =
                    ArgumentCaptor.forClass(com.integrityfamily.guardian.domain.GuardianVote.class);
            verify(voteRepository).save(captor.capture());
            assertThat(captor.getValue().getNominated()).isSameAs(memberB);
            assertThat(captor.getValue().getVoter()).isSameAs(memberA);
        }

        @Test
        @DisplayName("cambio de voto actualiza el nominated del voto existente")
        void changeVote_updatesExistingVote() {
            var existingVote = com.integrityfamily.guardian.domain.GuardianVote.builder()
                    .id(99L).family(family).voter(memberA).nominated(memberB).build();

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberA));
            when(memberRepository.findById(3L)).thenReturn(Optional.of(memberC));
            when(voteRepository.findByFamilyIdAndVoterId(10L, 1L)).thenReturn(Optional.of(existingVote));
            when(voteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(voteRepository.countVotesByFamilyGroupedByNominated(10L)).thenReturn(List.of());
            when(voteRepository.countByFamilyId(10L)).thenReturn(1L);
            when(voteRepository.existsByFamilyIdAndVoterId(10L, 1L)).thenReturn(true);
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(missionRepository.countByFamilyIdAndStatus(10L, MissionStatus.COMPLETED)).thenReturn(0L);

            service.vote(10L, new VoteRequest(1L, 3L));  // cambia de memberB a memberC

            assertThat(existingVote.getId()).isEqualTo(99L);         // misma fila
            assertThat(existingVote.getNominated()).isSameAs(memberC); // candidato actualizado
        }

        @Test
        @DisplayName("votante no pertenece a la familia → lanza BusinessException")
        void voterNotInFamily_throws() {
            FamilyMember outsider = FamilyMember.builder().id(99L).fullName("Extraño").build();
            Family otherFamily = Family.builder().id(999L).build();
            outsider.setFamily(otherFamily);

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(99L)).thenReturn(Optional.of(outsider));

            assertThatThrownBy(() -> service.vote(10L, new VoteRequest(99L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no pertenece a esta familia");
        }

        @Test
        @DisplayName("familia no encontrada → lanza BusinessException NOT_FOUND")
        void familyNotFound_throws() {
            when(familyRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.vote(999L, new VoteRequest(1L, 2L)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resolveGuardianIfMajority() — elección automática")
    class ResolveGuardian {

        @Test
        @DisplayName("mayoría simple alcanzada → familia actualizada con nuevo guardián")
        void majorityReached_setsGuardian() {
            // 2 de 3 votos para memberB (> 3/2 = 1.5)
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberA));
            when(memberRepository.findById(2L)).thenReturn(Optional.of(memberB));
            when(voteRepository.findByFamilyIdAndVoterId(10L, 1L)).thenReturn(Optional.empty());
            when(voteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Simular conteo: memberB tiene 2 votos de 3 → mayoría simple
            when(voteRepository.countVotesByFamilyGroupedByNominated(10L))
                    .thenReturn(Collections.singletonList(new Object[]{2L, 2L}));  // nominatedId=2, votes=2
            when(memberRepository.countByFamilyId(10L)).thenReturn(3L);  // 2 de 3 → mayoría
            when(voteRepository.countByFamilyId(10L)).thenReturn(2L);
            when(voteRepository.existsByFamilyIdAndVoterId(10L, 1L)).thenReturn(false);
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(missionRepository.countByFamilyIdAndStatus(10L, MissionStatus.COMPLETED)).thenReturn(0L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(memberB)); // para guardianName lookup

            service.vote(10L, new VoteRequest(1L, 2L));

            // familyRepository.save() llamado (al menos una vez) con guardianMemberId=2
            ArgumentCaptor<Family> familyCaptor = ArgumentCaptor.forClass(Family.class);
            verify(familyRepository, atLeastOnce()).save(familyCaptor.capture());
            Family saved = familyCaptor.getAllValues().stream()
                    .filter(f -> f.getGuardianMemberId() != null).findFirst().orElseThrow();
            assertThat(saved.getGuardianMemberId()).isEqualTo(2L);
            assertThat(saved.getGuardianSince()).isNotNull();
        }

        @Test
        @DisplayName("sin mayoría simple → guardianMemberId no cambia")
        void noMajority_guardianUnchanged() {
            // 1 de 3 votos: no supera 3/2 = 1.5
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberA));
            when(memberRepository.findById(2L)).thenReturn(Optional.of(memberB));
            when(voteRepository.findByFamilyIdAndVoterId(10L, 1L)).thenReturn(Optional.empty());
            when(voteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(voteRepository.countVotesByFamilyGroupedByNominated(10L))
                    .thenReturn(Collections.singletonList(new Object[]{2L, 1L}));  // 1 solo voto
            when(memberRepository.countByFamilyId(10L)).thenReturn(3L);  // 1 de 3 → no mayoría
            when(voteRepository.countByFamilyId(10L)).thenReturn(1L);
            when(voteRepository.existsByFamilyIdAndVoterId(10L, 1L)).thenReturn(false);
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(missionRepository.countByFamilyIdAndStatus(10L, MissionStatus.COMPLETED)).thenReturn(0L);

            service.vote(10L, new VoteRequest(1L, 2L));

            assertThat(family.getGuardianMemberId()).isNull();  // sin cambio
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("confirmGuardian()")
    class ConfirmGuardian {

        @Test
        @DisplayName("confirma guardián manualmente y persiste guardianSince")
        void confirmsGuardianAndPersists() {
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(2L)).thenReturn(Optional.of(memberB));
            when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // stubs para getStatus
            when(voteRepository.countVotesByFamilyGroupedByNominated(10L)).thenReturn(List.of());
            when(voteRepository.countByFamilyId(10L)).thenReturn(0L);
            when(voteRepository.existsByFamilyIdAndVoterId(10L, 2L)).thenReturn(false);
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(missionRepository.countByFamilyIdAndStatus(10L, MissionStatus.COMPLETED)).thenReturn(0L);
            when(memberRepository.findById(2L)).thenReturn(Optional.of(memberB));

            var response = service.confirmGuardian(10L, 2L);

            assertThat(response.guardianMemberId()).isEqualTo(2L);
            assertThat(response.guardianFullName()).isEqualTo("Bruno");
            assertThat(family.getGuardianSince()).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("activateMission()")
    class ActivateMission {

        @Test
        @DisplayName("guardián activa misión → se persiste con status ACTIVE")
        void guardianActivatesMission() {
            family.setGuardianMemberId(1L);  // memberA es guardián
            FamilyMission saved = FamilyMission.builder()
                    .id(1L).family(family).title("Cena sin pantallas")
                    .category(MissionCategory.CONEXION).durationMinutes(60)
                    .status(MissionStatus.ACTIVE).createdBy(memberA)
                    .build();

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberA));
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(missionRepository.save(any(FamilyMission.class))).thenReturn(saved);

            var req = new ActivateMissionRequest("Cena sin pantallas", "Sin celulares",
                    MissionCategory.CONEXION, 60, 1L);
            MissionDto dto = service.activateMission(10L, req);

            assertThat(dto.status()).isEqualTo(MissionStatus.ACTIVE);
            assertThat(dto.title()).isEqualTo("Cena sin pantallas");
        }

        @Test
        @DisplayName("no-guardián intenta activar → lanza BusinessException FORBIDDEN")
        void nonGuardian_throwsForbidden() {
            family.setGuardianMemberId(1L);  // memberA es guardián, pero memberB intenta activar

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            var req = new ActivateMissionRequest("Test", null, MissionCategory.CONEXION, 60, 2L);
            assertThatThrownBy(() -> service.activateMission(10L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo el Guardián Familiar");
        }

        @Test
        @DisplayName("familia sin guardián → lanza BusinessException FORBIDDEN")
        void noGuardian_throwsForbidden() {
            // family.guardianMemberId is null → no guardian
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));

            var req = new ActivateMissionRequest("Test", null, MissionCategory.CONEXION, 60, 1L);
            assertThatThrownBy(() -> service.activateMission(10L, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo el Guardián Familiar");
        }

        @Test
        @DisplayName("misión activa anterior es cancelada antes de activar la nueva")
        void previousActiveMission_isCancelled() {
            family.setGuardianMemberId(1L);
            FamilyMission existing = FamilyMission.builder()
                    .id(5L).status(MissionStatus.ACTIVE).family(family).build();
            FamilyMission newMission = FamilyMission.builder()
                    .id(6L).family(family).title("Nueva")
                    .category(MissionCategory.COMUNICACION).durationMinutes(30)
                    .status(MissionStatus.ACTIVE).createdBy(memberA).build();

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(memberRepository.findById(1L)).thenReturn(Optional.of(memberA));
            when(missionRepository.findTopByFamilyIdAndStatusOrderByActivatedAtDesc(10L, MissionStatus.ACTIVE))
                    .thenReturn(Optional.of(existing));
            when(missionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(missionRepository.save(argThat(m -> m.getId() == null))).thenReturn(newMission);

            service.activateMission(10L,
                    new ActivateMissionRequest("Nueva", null, MissionCategory.COMUNICACION, 30, 1L));

            assertThat(existing.getStatus()).isEqualTo(MissionStatus.CANCELLED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("completeMission()")
    class CompleteMission {

        @Test
        @DisplayName("misión completada → status COMPLETED, participationScore += 10")
        void completeMission_updatesStatusAndScore() {
            family.setParticipationScore(20);
            FamilyMission mission = FamilyMission.builder()
                    .id(7L).family(family).status(MissionStatus.ACTIVE)
                    .title("Caminar juntos").build();

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(missionRepository.findById(7L)).thenReturn(Optional.of(mission));
            when(missionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.completeMission(10L, 7L, 1L);

            assertThat(mission.getStatus()).isEqualTo(MissionStatus.COMPLETED);
            assertThat(mission.getCompletedAt()).isNotNull();
            assertThat(family.getParticipationScore()).isEqualTo(30);  // 20 + 10
        }

        @Test
        @DisplayName("misión no encontrada → lanza BusinessException MISSION_NOT_FOUND")
        void missionNotFound_throws() {
            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(missionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.completeMission(10L, 99L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Misión no encontrada");
        }

        @Test
        @DisplayName("misión de otra familia → lanza BusinessException MISSION_MISMATCH")
        void missionFamilyMismatch_throws() {
            Family otherFamily = Family.builder().id(999L).build();
            FamilyMission missionOther = FamilyMission.builder()
                    .id(8L).family(otherFamily).status(MissionStatus.ACTIVE).build();

            when(familyRepository.findById(10L)).thenReturn(Optional.of(family));
            when(missionRepository.findById(8L)).thenReturn(Optional.of(missionOther));

            assertThatThrownBy(() -> service.completeMission(10L, 8L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no pertenece a esta familia");
        }
    }
}
