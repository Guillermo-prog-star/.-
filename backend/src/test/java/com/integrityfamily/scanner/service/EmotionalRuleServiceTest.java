package com.integrityfamily.scanner.service;

import com.integrityfamily.scanner.domain.EmotionalRule;
import com.integrityfamily.scanner.dto.EmotionalRuleDto;
import com.integrityfamily.scanner.dto.EmotionalRuleRequest;
import com.integrityfamily.scanner.repository.EmotionalRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmotionalRuleService — IF-EEDSL Admin")
class EmotionalRuleServiceTest {

    @Mock
    private EmotionalRuleRepository repo;

    @InjectMocks
    private EmotionalRuleService service;

    // ── Helper ────────────────────────────────────────────────────────────────

    private EmotionalRule rule(Long id, String ruleKey, int version, boolean active) {
        return EmotionalRule.builder()
                .id(id)
                .ruleKey(ruleKey)
                .version(version)
                .active(active)
                .milestoneScope("*")
                .memberRole("*")
                .requiredSignals(List.of("high_risk"))
                .temporalWindowDays(14)
                .confidenceBase(0.70)
                .createdBy("ADMIN")
                .createdAt(Instant.now())
                .build();
    }

    // ── findAll / findActive ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll / findActive")
    class FindMethods {
        @Test
        void findAllReturnsMappedDtos() {
            when(repo.findAllWithSignals()).thenReturn(
                    List.of(rule(1L, "rule_a", 1, true), rule(2L, "rule_b", 1, false)));

            List<EmotionalRuleDto> result = service.findAll();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(EmotionalRuleDto::ruleKey)
                    .containsExactly("rule_a", "rule_b");
        }

        @Test
        void findActiveReturnsMappedDtos() {
            when(repo.findByActiveTrue()).thenReturn(List.of(rule(1L, "rule_a", 1, true)));

            List<EmotionalRuleDto> result = service.findActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).active()).isTrue();
        }
    }

    // ── toggleActive ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toggleActive")
    class ToggleActive {
        @Test
        void flipsActiveFromTrueToFalse() {
            EmotionalRule r = rule(1L, "stress_rule", 1, true);
            when(repo.findById(1L)).thenReturn(Optional.of(r));
            when(repo.save(r)).thenReturn(r);

            EmotionalRuleDto result = service.toggleActive(1L);

            assertThat(result.active()).isFalse();
        }

        @Test
        void flipsActiveFromFalseToTrue() {
            EmotionalRule r = rule(1L, "stress_rule", 1, false);
            when(repo.findById(1L)).thenReturn(Optional.of(r));
            when(repo.save(r)).thenReturn(r);

            EmotionalRuleDto result = service.toggleActive(1L);

            assertThat(result.active()).isTrue();
        }

        @Test
        void throwsWhenRuleNotFound() {
            when(repo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.toggleActive(999L))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("999");
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        void createsFirstVersionWhenRuleKeyIsNew() {
            EmotionalRuleRequest req = new EmotionalRuleRequest(
                    "new_rule", "*", "*", List.of("high_risk"), 14, null, 0.75, null);
            when(repo.findByRuleKeyOrderByVersionDesc("new_rule")).thenReturn(List.of());
            EmotionalRule saved = rule(10L, "new_rule", 1, true);
            when(repo.save(any(EmotionalRule.class))).thenReturn(saved);

            EmotionalRuleDto result = service.create(req);

            assertThat(result.version()).isEqualTo(1);
            assertThat(result.ruleKey()).isEqualTo("new_rule");
        }

        @Test
        void autoIncrementsVersionWhenRuleKeyExists() {
            EmotionalRuleRequest req = new EmotionalRuleRequest(
                    "existing_rule", "*", "*", List.of("high_risk"), 14, null, 0.75, null);
            when(repo.findByRuleKeyOrderByVersionDesc("existing_rule"))
                    .thenReturn(List.of(rule(5L, "existing_rule", 3, false)));
            EmotionalRule saved = rule(11L, "existing_rule", 4, true);
            when(repo.save(any(EmotionalRule.class))).thenReturn(saved);

            EmotionalRuleDto result = service.create(req);

            assertThat(result.version()).isEqualTo(4);
        }

        @Test
        void usesDefaultsForNullOptionalFields() {
            EmotionalRuleRequest req = new EmotionalRuleRequest(
                    "minimal_rule", null, null, null, null, null, null, null);
            when(repo.findByRuleKeyOrderByVersionDesc("minimal_rule")).thenReturn(List.of());

            ArgumentCaptor<EmotionalRule> captor = ArgumentCaptor.forClass(EmotionalRule.class);
            when(repo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.create(req);

            EmotionalRule persisted = captor.getValue();
            assertThat(persisted.getMilestoneScope()).isEqualTo("*");
            assertThat(persisted.getMemberRole()).isEqualTo("*");
            assertThat(persisted.getTemporalWindowDays()).isEqualTo(14);
            assertThat(persisted.getConfidenceBase()).isEqualTo(0.70);
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {
        @Test
        void updatesOnlyNonNullFields() {
            EmotionalRule existing = rule(1L, "stress_rule", 2, true);
            existing.setMilestoneScope("M6");
            when(repo.findById(1L)).thenReturn(Optional.of(existing));
            when(repo.save(existing)).thenReturn(existing);

            EmotionalRuleRequest req = new EmotionalRuleRequest(
                    null, "M12", null, null, null, null, null, null);

            EmotionalRuleDto result = service.update(1L, req);

            assertThat(result.milestoneScope()).isEqualTo("M12");
            assertThat(result.memberRole()).isEqualTo("*");
        }

        @Test
        void throwsWhenRuleNotFound() {
            when(repo.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(404L,
                    new EmotionalRuleRequest(null, null, null, null, null, null, null, null)))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("404");
        }
    }
}
