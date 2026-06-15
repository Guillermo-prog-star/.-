package com.integrityfamily.participation.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.ParticipationEventType;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.ParticipationEventRepository;
import com.integrityfamily.participation.dto.ParticipationPulseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParticipationService")
class ParticipationServiceTest {

    @Mock ParticipationEventRepository repo;
    @Mock FamilyRepository             familyRepository;
    @InjectMocks ParticipationService service;

    private static final long FAM_ID      = 1L;
    private static final long GUARDIAN_ID = 10L;

    private FamilyMember member(long id, String name) {
        return FamilyMember.builder().id(id).fullName(name).active(true).build();
    }

    private Family familyWith(List<FamilyMember> members) {
        Family f = Family.builder().id(FAM_ID).name("Test").build();
        f.getMembers().addAll(members);
        return f;
    }

    // ── record ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("record")
    class Record {

        @Test
        @DisplayName("familyId=null → retorna sin guardar evento")
        void nullFamilyId_nothingRecorded() {
            service.record(null, 5L, ParticipationEventType.CHAT_MESSAGE);

            verifyNoInteractions(repo);
        }

        @Test
        @DisplayName("familyId válido → guarda ParticipationEvent con tipo correcto")
        void valid_savesEvent() {
            service.record(FAM_ID, 5L, ParticipationEventType.CHAT_MESSAGE);

            verify(repo).save(argThat(e ->
                    FAM_ID == e.getFamilyId()
                    && Long.valueOf(5L).equals(e.getMemberId())
                    && ParticipationEventType.CHAT_MESSAGE == e.getEventType()));
        }
    }

    // ── getSummary — fatigueSignal ─────────────────────────────────────────

    @Nested
    @DisplayName("getSummary — fatigueSignal")
    class FatigueSignal {

        @Test
        @DisplayName("guardianId=null → NONE")
        void nullGuardian_fatigueNone() {
            List<FamilyMember> members = List.of(member(1L, "A"), member(2L, "B"));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(familyWith(members)));
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of(1L, 2L));
            lenient().when(repo.findLastActivityByMember(eq(FAM_ID), anyLong())).thenReturn(Optional.empty());

            ParticipationService.FamilyParticipationSummary result = service.getSummary(FAM_ID, null);

            assertThat(result.fatigueSignal()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("guardián es el único activo → HIGH")
        void onlyGuardianActive_fatigueHigh() {
            List<FamilyMember> members = List.of(member(GUARDIAN_ID, "Guardián"), member(20L, "Otro"));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(familyWith(members)));
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of(GUARDIAN_ID));
            lenient().when(repo.findLastActivityByMember(eq(FAM_ID), anyLong())).thenReturn(Optional.empty());

            ParticipationService.FamilyParticipationSummary result = service.getSummary(FAM_ID, GUARDIAN_ID);

            assertThat(result.fatigueSignal()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("guardián + 1 otro activo en familia de 4 → MILD")
        void guardianPlusOneOf4_fatigueMild() {
            List<FamilyMember> members = List.of(
                    member(GUARDIAN_ID, "G"), member(20L, "A"), member(21L, "B"), member(22L, "C"));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(familyWith(members)));
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of(GUARDIAN_ID, 20L));
            lenient().when(repo.findLastActivityByMember(eq(FAM_ID), anyLong())).thenReturn(Optional.empty());

            ParticipationService.FamilyParticipationSummary result = service.getSummary(FAM_ID, GUARDIAN_ID);

            assertThat(result.fatigueSignal()).isEqualTo("MILD");
        }

        @Test
        @DisplayName("guardián + 2 otros activos → NONE (sin fatiga)")
        void guardianPlusTwo_fatigueNone() {
            List<FamilyMember> members = List.of(
                    member(GUARDIAN_ID, "G"), member(20L, "A"), member(21L, "B"), member(22L, "C"));
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(familyWith(members)));
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of(GUARDIAN_ID, 20L, 21L));
            lenient().when(repo.findLastActivityByMember(eq(FAM_ID), anyLong())).thenReturn(Optional.empty());

            ParticipationService.FamilyParticipationSummary result = service.getSummary(FAM_ID, GUARDIAN_ID);

            assertThat(result.fatigueSignal()).isEqualTo("NONE");
        }

        @Test
        @DisplayName("familia no encontrada → 0 miembros y lista vacía")
        void familyNotFound_emptyResult() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of());

            ParticipationService.FamilyParticipationSummary result = service.getSummary(FAM_ID, GUARDIAN_ID);

            assertThat(result.totalMembers()).isEqualTo(0);
            assertThat(result.activities()).isEmpty();
        }
    }

    // ── getPulse — tasa y iniciales ───────────────────────────────────────────

    @Nested
    @DisplayName("getPulse")
    class GetPulse {

        @Test
        @DisplayName("familia no encontrada → totalMembers=0, participationRate=0.0")
        void familyNotFound_zeroRate() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of());
            when(repo.findAllOccurredAt(eq(FAM_ID), any())).thenReturn(List.of());

            ParticipationPulseResponse result = service.getPulse(FAM_ID);

            assertThat(result.totalMembers()).isEqualTo(0);
            assertThat(result.participationRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("nombre completo 'Juan López' → iniciales 'JL'")
        void twoWordName_buildsTwoLetterInitials() {
            FamilyMember m = member(1L, "Juan López");
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(familyWith(List.of(m))));
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of(1L));
            when(repo.findLastActivityByMember(eq(FAM_ID), eq(1L))).thenReturn(Optional.of(LocalDateTime.now()));
            when(repo.findAllOccurredAt(eq(FAM_ID), any())).thenReturn(List.of());

            ParticipationPulseResponse result = service.getPulse(FAM_ID);

            assertThat(result.members().get(0).initials()).isEqualTo("JL");
        }

        @Test
        @DisplayName("nombre simple 'María' → inicial 'M'")
        void singleWordName_buildsSingleLetterInitials() {
            FamilyMember m = member(2L, "María");
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(familyWith(List.of(m))));
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of());
            when(repo.findLastActivityByMember(eq(FAM_ID), eq(2L))).thenReturn(Optional.empty());
            when(repo.findAllOccurredAt(eq(FAM_ID), any())).thenReturn(List.of());

            ParticipationPulseResponse result = service.getPulse(FAM_ID);

            assertThat(result.members().get(0).initials()).isEqualTo("M");
        }

        @Test
        @DisplayName("sparkline semanal contiene exactamente 7 días")
        void sparkline_has7Days() {
            when(familyRepository.findById(FAM_ID)).thenReturn(Optional.empty());
            when(repo.findActiveMemberIds(eq(FAM_ID), any())).thenReturn(List.of());
            when(repo.findAllOccurredAt(eq(FAM_ID), any())).thenReturn(List.of());

            ParticipationPulseResponse result = service.getPulse(FAM_ID);

            assertThat(result.weeklyActivity()).hasSize(7);
        }
    }
}
