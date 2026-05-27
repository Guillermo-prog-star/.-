package com.integrityfamily.report.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.report.domain.VoiceAudit;
import com.integrityfamily.report.repository.VoiceAuditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoiceAnalyticsService")
class VoiceAnalyticsServiceTest {

    @Mock VoiceAuditRepository voiceAuditRepository;
    @Mock FamilyRepository     familyRepository;

    @InjectMocks VoiceAnalyticsService service;

    // ── getSummaryStats ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSummaryStats")
    class SummaryStats {

        @Test
        @DisplayName("successRate es 0 cuando no hay mensajes (division por cero protegida)")
        void noMessages_successRateIsZero() {
            when(voiceAuditRepository.count()).thenReturn(0L);
            when(voiceAuditRepository.countSuccessfulMessages()).thenReturn(0L);
            when(voiceAuditRepository.countDistinctFamilyId()).thenReturn(0L);
            when(voiceAuditRepository.sumDurationSeconds()).thenReturn(0L);

            Map<String, Object> stats = service.getSummaryStats();

            assertThat(stats.get("successRate")).isEqualTo(0.0);
            assertThat(stats.get("totalMessages")).isEqualTo(0L);
        }

        @Test
        @DisplayName("calcula successRate correctamente redondeando a 1 decimal")
        void eightOfTen_successRateIs80dot0() {
            when(voiceAuditRepository.count()).thenReturn(10L);
            when(voiceAuditRepository.countSuccessfulMessages()).thenReturn(8L);
            when(voiceAuditRepository.countDistinctFamilyId()).thenReturn(3L);
            when(voiceAuditRepository.sumDurationSeconds()).thenReturn(120L);

            Map<String, Object> stats = service.getSummaryStats();

            assertThat(stats.get("successRate")).isEqualTo(80.0);
            assertThat(stats.get("activeFamilies")).isEqualTo(3L);
            assertThat(stats.get("totalDuration")).isEqualTo(120L);
        }

        @Test
        @DisplayName("redondea successRate a 1 decimal para valores no enteros")
        void threeOfSeven_successRateRoundedToOneDecimal() {
            when(voiceAuditRepository.count()).thenReturn(7L);
            when(voiceAuditRepository.countSuccessfulMessages()).thenReturn(3L);
            when(voiceAuditRepository.countDistinctFamilyId()).thenReturn(2L);
            when(voiceAuditRepository.sumDurationSeconds()).thenReturn(50L);

            Map<String, Object> stats = service.getSummaryStats();

            // 3/7 * 100 = 42.857... -> Math.round(42.857 * 10) / 10.0 = 428/10.0 = 42.8
            assertThat((Double) stats.get("successRate")).isEqualTo(42.9);
        }
    }

    // ── getRecentInteractions ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getRecentInteractions")
    class RecentInteractions {

        @Test
        @DisplayName("retorna lista vacia cuando no hay auditorias")
        void noAudits_returnsEmptyList() {
            when(voiceAuditRepository.findTop10ByOrderByProcessedAtDesc()).thenReturn(List.of());

            assertThat(service.getRecentInteractions()).isEmpty();
        }

        @Test
        @DisplayName("resuelve nombre de familia cuando la familia existe")
        void familyFound_usesRealName() {
            VoiceAudit audit = VoiceAudit.builder()
                    .id(1L).familyId(10L).municipio("Bogotá").durationSeconds(45)
                    .success(Boolean.TRUE).processedAt(LocalDateTime.now())
                    .build();
            when(voiceAuditRepository.findTop10ByOrderByProcessedAtDesc()).thenReturn(List.of(audit));
            when(familyRepository.findById(10L))
                    .thenReturn(Optional.of(Family.builder().id(10L).name("Los García").build()));

            List<Map<String, Object>> result = service.getRecentInteractions();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).get("family")).isEqualTo("Los García");
            assertThat(result.get(0).get("status")).isEqualTo("SUCCESS");
            assertThat(result.get(0).get("municipio")).isEqualTo("Bogotá");
        }

        @Test
        @DisplayName("usa 'Desconocido' como nombre cuando la familia no existe")
        void familyNotFound_usesDesconocido() {
            VoiceAudit audit = VoiceAudit.builder()
                    .id(2L).familyId(99L).municipio(null).durationSeconds(20)
                    .success(Boolean.FALSE).processedAt(LocalDateTime.now())
                    .build();
            when(voiceAuditRepository.findTop10ByOrderByProcessedAtDesc()).thenReturn(List.of(audit));
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            List<Map<String, Object>> result = service.getRecentInteractions();

            assertThat(result.get(0).get("family")).isEqualTo("Desconocido");
            assertThat(result.get(0).get("municipio")).isEqualTo("N/A");
            assertThat(result.get(0).get("status")).isEqualTo("ERROR");
        }
    }

    // ── getRegionalStats ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getRegionalStats")
    class RegionalStats {

        @Test
        @DisplayName("mapea nombre y count desde la proyeccion del repositorio")
        void rowsWithNames_mappedCorrectly() {
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            rows.add(new Object[]{"Bogotá", 15L});
            rows.add(new Object[]{"Medellín", 8L});
            when(voiceAuditRepository.getRegionalUsage()).thenReturn(rows);

            List<Map<String, Object>> result = service.getRegionalStats();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).get("name")).isEqualTo("Bogotá");
            assertThat(result.get(0).get("count")).isEqualTo(15L);
        }

        @Test
        @DisplayName("usa 'Desconocido' cuando el municipio es null")
        void rowWithNullName_usesDesconocido() {
            java.util.List<Object[]> rows = new java.util.ArrayList<>();
            rows.add(new Object[]{null, 3L});
            when(voiceAuditRepository.getRegionalUsage()).thenReturn(rows);

            List<Map<String, Object>> result = service.getRegionalStats();

            assertThat(result.get(0).get("name")).isEqualTo("Desconocido");
        }
    }
}
