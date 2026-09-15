package com.integrityfamily.vitality.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.vitality.domain.DailyVitalityLog;
import com.integrityfamily.vitality.dto.VitalityDtos.RegisterLogRequest;
import com.integrityfamily.vitality.repository.DailyVitalityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VitalityService — Unit Tests")
class VitalityServiceTest {

    @Mock DailyVitalityLogRepository repository;
    @Mock MemberRepository memberRepository;

    @InjectMocks
    VitalityService vitalityService;

    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia López").build();
        member = FamilyMember.builder().id(10L).fullName("Ana López").family(family).build();
    }

    @Nested
    @DisplayName("registerLog()")
    class RegisterLog {

        @Test
        @DisplayName("Miembro inexistente → BusinessException")
        void shouldThrow_whenMemberNotFound() {
            when(memberRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vitalityService.registerLog(1L, 10L,
                    new RegisterLogRequest(null, 7.0, 4, null, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no encontrado");
        }

        @Test
        @DisplayName("Miembro de otra familia → BusinessException")
        void shouldThrow_whenMemberBelongsToDifferentFamily() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> vitalityService.registerLog(99L, 10L,
                    new RegisterLogRequest(null, 7.0, 4, null, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no pertenece");
        }

        @Test
        @DisplayName("Sin registro previo → crea uno nuevo con los campos del request")
        void shouldCreateNewLog_whenNoneExists() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(repository.findByFamilyMemberIdAndLogDate(10L, LocalDate.of(2026, 7, 20)))
                    .thenReturn(Optional.empty());
            when(repository.save(any(DailyVitalityLog.class))).thenAnswer(i -> i.getArgument(0));

            DailyVitalityLog result = vitalityService.registerLog(1L, 10L,
                    new RegisterLogRequest(LocalDate.of(2026, 7, 20), 7.5, 4, 30, 4, 20, 2));

            assertThat(result.getFamilyMember()).isEqualTo(member);
            assertThat(result.getLogDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(result.getSleepHours()).isEqualTo(7.5);
            assertThat(result.getSleepQuality()).isEqualTo(4);
            assertThat(result.getExerciseMinutes()).isEqualTo(30);
            assertThat(result.getNutritionQuality()).isEqualTo(4);
            assertThat(result.getScreenTimeBeforeBedMinutes()).isEqualTo(20);
            assertThat(result.getFatigueLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Registro previo + request parcial → solo sobreescribe los campos presentes")
        void shouldOnlyOverwritePresentFields_whenLogAlreadyExists() {
            DailyVitalityLog existing = DailyVitalityLog.builder()
                    .id(5L).familyMember(member).logDate(LocalDate.of(2026, 7, 20))
                    .sleepHours(6.0).sleepQuality(3).exerciseMinutes(15)
                    .build();
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(repository.findByFamilyMemberIdAndLogDate(10L, LocalDate.of(2026, 7, 20)))
                    .thenReturn(Optional.of(existing));
            when(repository.save(any(DailyVitalityLog.class))).thenAnswer(i -> i.getArgument(0));

            // Solo llega fatigueLevel -- el resto debe conservar lo ya guardado.
            DailyVitalityLog result = vitalityService.registerLog(1L, 10L,
                    new RegisterLogRequest(LocalDate.of(2026, 7, 20), null, null, null, null, null, 5));

            assertThat(result.getSleepHours()).isEqualTo(6.0);
            assertThat(result.getSleepQuality()).isEqualTo(3);
            assertThat(result.getExerciseMinutes()).isEqualTo(15);
            assertThat(result.getFatigueLevel()).isEqualTo(5);
        }

        @Test
        @DisplayName("logDate ausente en el request → usa la fecha de hoy")
        void shouldDefaultToToday_whenLogDateAbsent() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            when(repository.findByFamilyMemberIdAndLogDate(any(), any())).thenReturn(Optional.empty());
            when(repository.save(any(DailyVitalityLog.class))).thenAnswer(i -> i.getArgument(0));

            DailyVitalityLog result = vitalityService.registerLog(1L, 10L,
                    new RegisterLogRequest(null, 8.0, null, null, null, null, null));

            assertThat(result.getLogDate()).isEqualTo(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("listByMember()")
    class ListByMember {

        @Test
        @DisplayName("Miembro de otra familia → BusinessException, no consulta el repositorio")
        void shouldThrow_whenMemberBelongsToDifferentFamily() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            assertThatThrownBy(() -> vitalityService.listByMember(99L, 10L,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20)))
                    .isInstanceOf(BusinessException.class);

            verify(repository, never()).findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(any(), any(), any());
        }

        @Test
        @DisplayName("Miembro válido → delega en el repositorio")
        void shouldDelegateToRepository() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));
            DailyVitalityLog log = DailyVitalityLog.builder().id(1L).familyMember(member)
                    .logDate(LocalDate.of(2026, 7, 20)).build();
            when(repository.findByFamilyMemberIdAndLogDateBetweenOrderByLogDateAsc(
                    10L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20)))
                    .thenReturn(List.of(log));

            List<DailyVitalityLog> result = vitalityService.listByMember(1L, 10L,
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20));

            assertThat(result).hasSize(1).containsExactly(log);
        }
    }
}
