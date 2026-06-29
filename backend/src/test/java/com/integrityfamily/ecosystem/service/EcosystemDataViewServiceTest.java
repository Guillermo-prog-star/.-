package com.integrityfamily.ecosystem.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.ecosystem.domain.*;
import com.integrityfamily.ecosystem.dto.EcosystemDataView;
import com.integrityfamily.ecosystem.repository.FamilyEcosystemLinkRepository;
import com.integrityfamily.family.dto.FamilyHealthSummaryResponse;
import com.integrityfamily.family.service.FamilyHealthSummaryService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EcosystemDataViewService — Unit Tests")
class EcosystemDataViewServiceTest {

    @Mock FamilyEcosystemLinkRepository linkRepository;
    @Mock FamilyRepository familyRepository;
    @Mock FamilyHealthSummaryService healthSummaryService;
    @Mock EcosystemAuditService auditService;

    @InjectMocks EcosystemDataViewService service;

    private static final Long FAMILY_ID = 1L;
    private static final Long LINK_ID   = 100L;
    private static final String EMAIL   = "actor@test.com";

    private EcosystemParticipant participant(NetworkType type) {
        return EcosystemParticipant.builder()
                .id(20L).name("Colegio San Marcos").networkType(type).active(true).build();
    }

    private FamilyEcosystemLink activeLink(NetworkType type) {
        return FamilyEcosystemLink.builder()
                .id(LINK_ID).familyId(FAMILY_ID)
                .participant(participant(type))
                .networkType(type).accessLevel(type == NetworkType.TERRITORIAL ? 3 : 2)
                .status(EcosystemLinkStatus.ACTIVE)
                .invitedByEmail(EMAIL).invitedAt(LocalDateTime.now())
                .canViewIcfScore(true).canViewRiskLevel(true)
                .canViewSprintProgress(true)
                .build();
    }

    private FamilyHealthSummaryResponse mockHealth() {
        return new FamilyHealthSummaryResponse(
                FAMILY_ID, "Familia Test",
                78.5, 5.2, "Fortaleza", "IMPROVING",
                "BAJO", false,
                "consciente", 0,
                3, 65, "Completar misión",
                true, "IN_PROGRESS", 5L,
                4L, 12L, 3L,
                java.time.LocalDateTime.now()
        );
    }

    private Family family() {
        Family f = new Family();
        f.setId(FAMILY_ID);
        f.setMunicipio("Medellín");
        f.setDepartmentCode("ANT");
        f.setCountryCode("CO");
        return f;
    }

    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("getDataView()")
    class GetDataView {

        @Test @DisplayName("devuelve datos según scope para nivel institucional")
        void devuelve_datos_por_scope() {
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(activeLink(NetworkType.INSTITUTIONAL)));
            when(healthSummaryService.summarize(FAMILY_ID)).thenReturn(mockHealth());

            EcosystemDataView view = service.getDataView(FAMILY_ID, LINK_ID, EMAIL);

            assertThat(view.getFamilyId()).isEqualTo(FAMILY_ID);
            assertThat(view.getIcfScore()).isEqualTo(78.5);
            assertThat(view.getRiskLevel()).isEqualTo("BAJO");
            assertThat(view.getHasActiveSprint()).isTrue();
            assertThat(view.getNetworkType()).isEqualTo(NetworkType.INSTITUTIONAL);
        }

        @Test @DisplayName("campos sin scope permanecen null")
        void campos_sin_scope_son_null() {
            FamilyEcosystemLink link = activeLink(NetworkType.INSTITUTIONAL);
            link.setCanViewPlanSummary(false);
            link.setCanViewCrisisHistory(false);

            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(healthSummaryService.summarize(FAMILY_ID)).thenReturn(mockHealth());

            EcosystemDataView view = service.getDataView(FAMILY_ID, LINK_ID, EMAIL);

            assertThat(view.getPlanSummaryAvailable()).isNull();
            assertThat(view.getCrisisHistoryAvailable()).isNull();
        }

        @Test @DisplayName("TERRITORIAL devuelve solo datos geográficos anónimos sin familyId")
        void territorial_devuelve_datos_anonimos() {
            FamilyEcosystemLink link = activeLink(NetworkType.TERRITORIAL);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));

            EcosystemDataView view = service.getDataView(FAMILY_ID, LINK_ID, EMAIL);

            assertThat(view.getFamilyId()).isNull();
            assertThat(view.getAggregatedOnly()).isTrue();
            assertThat(view.getMunicipio()).isEqualTo("Medellín");
            assertThat(view.getIcfScore()).isNull();
            assertThat(view.getRiskLevel()).isNull();
            verify(healthSummaryService, never()).summarize(any());
        }

        @Test @DisplayName("lanza FORBIDDEN si el vínculo no está ACTIVE")
        void lanza_forbidden_si_no_activo() {
            FamilyEcosystemLink link = activeLink(NetworkType.COMMUNITY);
            link.setStatus(EcosystemLinkStatus.INVITED);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> service.getDataView(FAMILY_ID, LINK_ID, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está activo");
        }

        @Test @DisplayName("lanza FORBIDDEN si el vínculo expiró")
        void lanza_forbidden_si_expirado() {
            FamilyEcosystemLink link = activeLink(NetworkType.INSTITUTIONAL);
            link.setValidUntil(LocalDate.now().minusDays(1));
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> service.getDataView(FAMILY_ID, LINK_ID, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("vigencia");
        }

        @Test @DisplayName("lanza FORBIDDEN si el link no pertenece a la familia")
        void lanza_forbidden_si_familia_distinta() {
            FamilyEcosystemLink link = activeLink(NetworkType.INSTITUTIONAL);
            link.setFamilyId(999L);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> service.getDataView(FAMILY_ID, LINK_ID, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No autorizado");
        }

        @Test @DisplayName("registra auditoría en cada acceso")
        void registra_auditoria() {
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(activeLink(NetworkType.COMMUNITY)));
            when(healthSummaryService.summarize(FAMILY_ID)).thenReturn(mockHealth());

            service.getDataView(FAMILY_ID, LINK_ID, EMAIL);

            verify(auditService).record(any(), eq("DATA_VIEW"), eq(EMAIL), anyString());
        }

        @Test @DisplayName("auditoría territorial usa acción TERRITORIAL_DATA_VIEW")
        void auditoria_territorial_usa_accion_especifica() {
            FamilyEcosystemLink link = activeLink(NetworkType.TERRITORIAL);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));

            service.getDataView(FAMILY_ID, LINK_ID, EMAIL);

            verify(auditService).record(any(), eq("TERRITORIAL_DATA_VIEW"), eq(EMAIL), anyString());
        }
    }
}
