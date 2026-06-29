package com.integrityfamily.family.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyJourneySnapshot;
import com.integrityfamily.domain.repository.FamilyJourneySnapshotRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.family.dto.JourneyHistoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JourneyHistoryService — Unit Tests")
class JourneyHistoryServiceTest {

    @Mock FamilyJourneySnapshotRepository snapshotRepository;
    @Mock FamilyRepository                familyRepository;

    @InjectMocks JourneyHistoryService service;

    private static final Long FAM_ID = 1L;
    private Family family;

    @BeforeEach
    void setUp() {
        family = new Family();
        family.setId(FAM_ID);
        family.setName("Familia López");
    }

    private void stubFamily() {
        when(familyRepository.findById(FAM_ID)).thenReturn(Optional.of(family));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private FamilyJourneySnapshot snap(LocalDate date, int level, int progress,
                                        boolean levelUp, Integer prev) {
        FamilyJourneySnapshot s = new FamilyJourneySnapshot();
        s.setFamilyId(FAM_ID);
        s.setSnapshotDate(date);
        s.setJourneyLevel(level);
        s.setJourneyProgress(progress);
        s.setLevelUp(levelUp);
        s.setPreviousLevel(prev);
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory() — familia no encontrada")
    class FamilyNotFound {

        @Test
        @DisplayName("lanza IllegalArgumentException cuando familia no existe")
        void throwsWhenNotFound() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getHistory(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory() — sin snapshots")
    class NoSnapshots {

        @Test
        @DisplayName("devuelve lista vacía cuando no hay snapshots")
        void returnsEmptyPoints() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of());

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            assertThat(r.points()).isEmpty();
            assertThat(r.totalLevelUps()).isEqualTo(0);
            assertThat(r.firstSnapshotDate()).isNull();
            assertThat(r.lastSnapshotDate()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHistory() — con snapshots")
    class WithSnapshots {

        private final LocalDate D1 = LocalDate.of(2026, 6, 1);
        private final LocalDate D2 = LocalDate.of(2026, 6, 5);
        private final LocalDate D3 = LocalDate.of(2026, 6, 10);

        @Test
        @DisplayName("devuelve los puntos en orden cronológico")
        void returnsPointsInOrder() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(
                            snap(D1, 2, 21, false, null),
                            snap(D2, 3, 29, true,  2),
                            snap(D3, 3, 29, false, null)
                    ));

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            assertThat(r.points()).hasSize(3);
            assertThat(r.points().get(0).date()).isEqualTo(D1);
            assertThat(r.points().get(1).date()).isEqualTo(D2);
            assertThat(r.points().get(2).date()).isEqualTo(D3);
        }

        @Test
        @DisplayName("cuenta correctamente los level-ups")
        void countsTotalLevelUps() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(
                            snap(D1, 2, 21, false, null),
                            snap(D2, 3, 29, true,  2),
                            snap(D3, 4, 36, true,  3)
                    ));

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            assertThat(r.totalLevelUps()).isEqualTo(2);
        }

        @Test
        @DisplayName("firstSnapshotDate y lastSnapshotDate apuntan al primer y último punto")
        void datesAreCorrect() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(
                            snap(D1, 1, 14, false, null),
                            snap(D3, 2, 21, true,  1)
                    ));

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            assertThat(r.firstSnapshotDate()).isEqualTo(D1);
            assertThat(r.lastSnapshotDate()).isEqualTo(D3);
        }

        @Test
        @DisplayName("los campos de cada SnapshotPoint se mapean correctamente")
        void pointFieldsAreMapped() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(snap(D2, 5, 43, true, 4)));

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            var pt = r.points().get(0);
            assertThat(pt.date()).isEqualTo(D2);
            assertThat(pt.level()).isEqualTo(5);
            assertThat(pt.progress()).isEqualTo(43);
            assertThat(pt.levelUp()).isTrue();
            assertThat(pt.previousLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("familyId y familyName se copian de la entidad familia")
        void metadataIsCorrect() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(snap(D1, 2, 21, false, null)));

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            assertThat(r.familyId()).isEqualTo(FAM_ID);
            assertThat(r.familyName()).isEqualTo("Familia López");
        }

        @Test
        @DisplayName("snapshot sin level-up tiene previousLevel null")
        void nonLevelUpHasNullPreviousLevel() {
            stubFamily(); when(snapshotRepository.findByFamilyIdOrderBySnapshotDateAsc(FAM_ID))
                    .thenReturn(List.of(snap(D1, 3, 29, false, null)));

            JourneyHistoryResponse r = service.getHistory(FAM_ID);

            assertThat(r.points().get(0).previousLevel()).isNull();
        }
    }
}
