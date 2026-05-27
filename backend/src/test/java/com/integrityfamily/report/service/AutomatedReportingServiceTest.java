package com.integrityfamily.report.service;

import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutomatedReportingService")
class AutomatedReportingServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock WhatsAppService  whatsappService;

    @InjectMocks AutomatedReportingService service;

    // ── isDueForReport — via processScheduledReports ──────────────────────────

    @Test
    @DisplayName("no envia nada cuando no hay familias")
    void processScheduledReports_noFamilies_nothingSent() {
        when(familyRepository.findAll()).thenReturn(List.of());

        service.processScheduledReports();

        verify(whatsappService, never()).sendToFamily(any(), anyString());
        verify(familyRepository, never()).save(any());
    }

    @Test
    @DisplayName("envia reporte a familia sin reporte previo creada hace mas de 6 meses")
    void processScheduledReports_neverReported_createdOver6MonthsAgo_sendsReport() {
        Family family = Family.builder()
                .id(1L).name("Familia Test").whatsapp("+573001234567")
                .lastReportSentAt(null)
                .createdAt(LocalDateTime.now().minusMonths(7))
                .build();
        when(familyRepository.findAll()).thenReturn(List.of(family));
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processScheduledReports();

        verify(whatsappService).sendToFamily(any(Family.class), anyString());
        assertThat(family.getLastReportSentAt()).isNotNull();
    }

    @Test
    @DisplayName("no envia reporte a familia sin reporte previo creada hace menos de 6 meses")
    void processScheduledReports_neverReported_createdRecently_doesNotSend() {
        Family family = Family.builder()
                .id(2L).name("Familia Reciente")
                .lastReportSentAt(null)
                .createdAt(LocalDateTime.now().minusMonths(3))
                .build();
        when(familyRepository.findAll()).thenReturn(List.of(family));

        service.processScheduledReports();

        verify(whatsappService, never()).sendToFamily(any(), anyString());
    }

    @Test
    @DisplayName("envia reporte a familia cuyo ultimo reporte fue hace mas de 6 meses")
    void processScheduledReports_lastReportOver6MonthsAgo_sendsReport() {
        Family family = Family.builder()
                .id(3L).name("Familia Antigua")
                .lastReportSentAt(LocalDateTime.now().minusMonths(8))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();
        when(familyRepository.findAll()).thenReturn(List.of(family));
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processScheduledReports();

        verify(whatsappService).sendToFamily(any(Family.class), anyString());
        assertThat(family.getLastReportSentAt()).isAfter(LocalDateTime.now().minusSeconds(5));
    }

    @Test
    @DisplayName("no envia reporte a familia que ya recibio reporte hace menos de 6 meses")
    void processScheduledReports_lastReportRecent_doesNotSend() {
        Family family = Family.builder()
                .id(4L).name("Familia Reciente")
                .lastReportSentAt(LocalDateTime.now().minusMonths(2))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();
        when(familyRepository.findAll()).thenReturn(List.of(family));

        service.processScheduledReports();

        verify(whatsappService, never()).sendToFamily(any(), anyString());
    }

    @Test
    @DisplayName("sigue procesando otras familias si WhatsApp falla para una")
    void processScheduledReports_whatsappFails_exceptionSwallowed_otherFamiliesProcessed() {
        Family f1 = Family.builder()
                .id(5L).name("Familia Error")
                .lastReportSentAt(null)
                .createdAt(LocalDateTime.now().minusMonths(10))
                .build();
        Family f2 = Family.builder()
                .id(6L).name("Familia OK")
                .lastReportSentAt(null)
                .createdAt(LocalDateTime.now().minusMonths(8))
                .build();
        when(familyRepository.findAll()).thenReturn(List.of(f1, f2));
        // sendToFamily es void: primera llamada lanza; segunda no hace nada
        org.mockito.Mockito.doThrow(new RuntimeException("WhatsApp no disponible"))
                .doNothing()
                .when(whatsappService).sendToFamily(any(Family.class), anyString());
        when(familyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // No debe propagar la excepcion — se espera que retorne sin error
        service.processScheduledReports();

        // Intenta enviar a ambas familias
        verify(whatsappService, org.mockito.Mockito.times(2)).sendToFamily(any(), anyString());
        // f2 (sin error) tiene su timestamp actualizado; f1 (error) lo conserva nulo
        assertThat(f1.getLastReportSentAt()).isNull();
        assertThat(f2.getLastReportSentAt()).isNotNull();
    }
}
