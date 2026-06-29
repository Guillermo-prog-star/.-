package com.integrityfamily.ecosystem.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.ecosystem.domain.*;
import com.integrityfamily.ecosystem.dto.EcosystemDtos.*;
import com.integrityfamily.ecosystem.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EcosystemService — Unit Tests")
class EcosystemServiceTest {

    @Mock FamilyRepository familyRepository;
    @Mock EcosystemParticipantRepository participantRepository;
    @Mock FamilyEcosystemLinkRepository linkRepository;
    @Mock EcosystemAuditService auditService;

    @InjectMocks EcosystemService service;

    // ── Fixtures ──────────────────────────────────────────────────────────

    private static final Long FAMILY_ID      = 1L;
    private static final Long PARTICIPANT_ID = 20L;
    private static final Long LINK_ID        = 200L;
    private static final String EMAIL        = "familia@test.com";

    private Family family() {
        Family f = new Family();
        f.setId(FAMILY_ID);
        return f;
    }

    private EcosystemParticipant participant(NetworkType type) {
        return EcosystemParticipant.builder()
                .id(PARTICIPANT_ID)
                .name("Colegio San Marcos")
                .networkType(type)
                .active(true)
                .build();
    }

    private EcosystemParticipant inactiveParticipant() {
        return EcosystemParticipant.builder()
                .id(PARTICIPANT_ID).name("ONG Inactiva")
                .networkType(NetworkType.COMMUNITY).active(false).build();
    }

    private FamilyEcosystemLink invitedLink(EcosystemParticipant p) {
        return FamilyEcosystemLink.builder()
                .id(LINK_ID).familyId(FAMILY_ID).participant(p)
                .networkType(p.getNetworkType())
                .accessLevel(2)
                .status(EcosystemLinkStatus.INVITED)
                .invitedByEmail(EMAIL)
                .invitedAt(LocalDateTime.now())
                .build();
    }

    private FamilyEcosystemLink activeLink(EcosystemParticipant p) {
        FamilyEcosystemLink l = invitedLink(p);
        l.setStatus(EcosystemLinkStatus.ACTIVE);
        l.setConsentedByEmail(EMAIL);
        l.setConsentedAt(LocalDateTime.now());
        return l;
    }

    // ─────────────────────────────────────────────────────────────────────
    // registerParticipant()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("registerParticipant()")
    class RegisterParticipant {

        @Test @DisplayName("registra un nuevo participante institucional correctamente")
        void registra_participante_nuevo() {
            RegisterParticipantRequest req = new RegisterParticipantRequest();
            req.setName("Colegio San Marcos");
            req.setNetworkType(NetworkType.INSTITUTIONAL);
            req.setContactEmail("rector@sanmarcos.edu");

            when(participantRepository.existsByNameAndNetworkType("Colegio San Marcos", NetworkType.INSTITUTIONAL))
                    .thenReturn(false);
            when(participantRepository.save(any())).thenAnswer(inv -> {
                EcosystemParticipant p = inv.getArgument(0);
                p.setId(PARTICIPANT_ID);
                return p;
            });

            ParticipantResponse resp = service.registerParticipant(req);

            assertThat(resp.getName()).isEqualTo("Colegio San Marcos");
            assertThat(resp.getNetworkType()).isEqualTo(NetworkType.INSTITUTIONAL);
            verify(participantRepository).save(any());
        }

        @Test @DisplayName("lanza CONFLICT si el nombre ya existe en ese tipo de red")
        void lanza_conflict_si_nombre_duplicado() {
            RegisterParticipantRequest req = new RegisterParticipantRequest();
            req.setName("Duplicado");
            req.setNetworkType(NetworkType.COMMUNITY);

            when(participantRepository.existsByNameAndNetworkType("Duplicado", NetworkType.COMMUNITY))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.registerParticipant(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya existe un participante");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // listParticipants()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("listParticipants()")
    class ListParticipants {

        @Test @DisplayName("sin filtro devuelve todos los activos")
        void sin_filtro_devuelve_todos_activos() {
            when(participantRepository.findByActiveTrue())
                    .thenReturn(List.of(participant(NetworkType.INSTITUTIONAL), participant(NetworkType.COMMUNITY)));

            assertThat(service.listParticipants(null)).hasSize(2);
        }

        @Test @DisplayName("con filtro llama al método por networkType")
        void con_filtro_llama_metodo_especifico() {
            when(participantRepository.findByNetworkTypeAndActiveTrue(NetworkType.TERRITORIAL))
                    .thenReturn(List.of());

            service.listParticipants(NetworkType.TERRITORIAL);

            verify(participantRepository).findByNetworkTypeAndActiveTrue(NetworkType.TERRITORIAL);
            verify(participantRepository, never()).findByActiveTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // link()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("link()")
    class Link {

        @Test @DisplayName("la familia vincula un participante institucional correctamente")
        void vincula_participante_institucional() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(PARTICIPANT_ID);
            req.setObjective("Monitorear asistencia escolar");

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(PARTICIPANT_ID))
                    .thenReturn(Optional.of(participant(NetworkType.INSTITUTIONAL)));
            when(linkRepository.existsByFamilyIdAndParticipantIdAndStatusNot(
                    FAMILY_ID, PARTICIPANT_ID, EcosystemLinkStatus.REVOKED)).thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                FamilyEcosystemLink l = inv.getArgument(0);
                l.setId(LINK_ID);
                return l;
            });

            LinkResponse resp = service.link(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(EcosystemLinkStatus.INVITED);
            assertThat(resp.getNetworkType()).isEqualTo(NetworkType.INSTITUTIONAL);
            assertThat(resp.getAccessLevel()).isEqualTo(2);
            assertThat(resp.getObjective()).isEqualTo("Monitorear asistencia escolar");
        }

        @Test @DisplayName("acceso territorial fuerza todos los flags en false — mínimo privilegio")
        void territorial_fuerza_scope_en_false() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(PARTICIPANT_ID);
            EcosystemAccessScopeDto scope = new EcosystemAccessScopeDto();
            scope.setCanViewIcfScore(true);
            scope.setCanViewCrisisHistory(true);
            req.setAccessScope(scope);

            EcosystemParticipant territorial = participant(NetworkType.TERRITORIAL);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(territorial));
            when(linkRepository.existsByFamilyIdAndParticipantIdAndStatusNot(any(), any(), any())).thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                FamilyEcosystemLink l = inv.getArgument(0);
                l.setId(LINK_ID);
                return l;
            });

            LinkResponse resp = service.link(FAMILY_ID, req, EMAIL);

            assertThat(resp.getAccessScope().isCanViewIcfScore()).isFalse();
            assertThat(resp.getAccessScope().isCanViewCrisisHistory()).isFalse();
            assertThat(resp.getAccessLevel()).isEqualTo(3);
        }

        @Test @DisplayName("Red Familiar recibe nivel de acceso 1")
        void familiar_recibe_access_level_1() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(PARTICIPANT_ID);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(PARTICIPANT_ID))
                    .thenReturn(Optional.of(participant(NetworkType.FAMILIAR)));
            when(linkRepository.existsByFamilyIdAndParticipantIdAndStatusNot(any(), any(), any())).thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                FamilyEcosystemLink l = inv.getArgument(0);
                l.setId(LINK_ID);
                return l;
            });

            LinkResponse resp = service.link(FAMILY_ID, req, EMAIL);

            assertThat(resp.getAccessLevel()).isEqualTo(1);
        }

        @Test @DisplayName("lanza NOT_FOUND si la familia no existe")
        void lanza_not_found_familia() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(PARTICIPANT_ID);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.link(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test @DisplayName("lanza NOT_FOUND si el participante no existe")
        void lanza_not_found_participante() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(99L);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.link(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Participante no encontrado");
        }

        @Test @DisplayName("lanza UNPROCESSABLE si el participante está inactivo")
        void lanza_error_participante_inactivo() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(PARTICIPANT_ID);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(inactiveParticipant()));

            assertThatThrownBy(() -> service.link(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está disponible");
        }

        @Test @DisplayName("lanza CONFLICT si ya existe vínculo activo o pendiente")
        void lanza_conflict_vinculo_duplicado() {
            LinkRequest req = new LinkRequest();
            req.setParticipantId(PARTICIPANT_ID);
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(PARTICIPANT_ID))
                    .thenReturn(Optional.of(participant(NetworkType.COMMUNITY)));
            when(linkRepository.existsByFamilyIdAndParticipantIdAndStatusNot(
                    FAMILY_ID, PARTICIPANT_ID, EcosystemLinkStatus.REVOKED)).thenReturn(true);

            assertThatThrownBy(() -> service.link(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya está vinculado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // giveConsent()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("giveConsent()")
    class GiveConsent {

        @Test @DisplayName("activa un vínculo INVITED correctamente")
        void activa_vinculo_invited() {
            ConsentRequest req = new ConsentRequest();
            req.setLinkId(LINK_ID);

            FamilyEcosystemLink link = invitedLink(participant(NetworkType.INSTITUTIONAL));
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LinkResponse resp = service.giveConsent(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(EcosystemLinkStatus.ACTIVE);
            assertThat(resp.getConsentedByEmail()).isEqualTo(EMAIL);
        }

        @Test @DisplayName("nivel territorial no puede recibir acceso a datos nominales al consentir")
        void territorial_no_recibe_acceso_nominal_al_consentir() {
            ConsentRequest req = new ConsentRequest();
            req.setLinkId(LINK_ID);
            EcosystemAccessScopeDto scope = new EcosystemAccessScopeDto();
            scope.setCanViewIcfScore(true);
            scope.setCanViewRiskLevel(true);
            req.setAccessScope(scope);

            FamilyEcosystemLink link = invitedLink(participant(NetworkType.TERRITORIAL));
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.giveConsent(FAMILY_ID, req, EMAIL);

            assertThat(link.isCanViewIcfScore()).isFalse();
            assertThat(link.isCanViewRiskLevel()).isFalse();
        }

        @Test @DisplayName("lanza error si el vínculo no está en estado INVITED")
        void lanza_error_si_no_es_invited() {
            ConsentRequest req = new ConsentRequest();
            req.setLinkId(LINK_ID);

            FamilyEcosystemLink link = activeLink(participant(NetworkType.COMMUNITY));
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> service.giveConsent(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo se puede consentir");
        }

        @Test @DisplayName("lanza FORBIDDEN si el link no pertenece a la familia")
        void lanza_forbidden_si_familia_distinta() {
            ConsentRequest req = new ConsentRequest();
            req.setLinkId(LINK_ID);

            FamilyEcosystemLink link = invitedLink(participant(NetworkType.INSTITUTIONAL));
            link.setFamilyId(999L);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> service.giveConsent(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("No autorizado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // revoke()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("revoke()")
    class Revoke {

        @Test @DisplayName("la familia revoca un vínculo activo")
        void revoca_vinculo_activo() {
            RevokeRequest req = new RevokeRequest();
            req.setLinkId(LINK_ID);
            req.setReason("Decisión de la familia");

            FamilyEcosystemLink link = activeLink(participant(NetworkType.INSTITUTIONAL));
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LinkResponse resp = service.revoke(FAMILY_ID, req, EMAIL);

            assertThat(resp.getStatus()).isEqualTo(EcosystemLinkStatus.REVOKED);
            assertThat(link.getRevocationReason()).isEqualTo("Decisión de la familia");
        }

        @Test @DisplayName("la revocación no requiere motivo")
        void revocacion_sin_motivo_es_valida() {
            RevokeRequest req = new RevokeRequest();
            req.setLinkId(LINK_ID);
            req.setReason(null);

            FamilyEcosystemLink link = activeLink(participant(NetworkType.COMMUNITY));
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatCode(() -> service.revoke(FAMILY_ID, req, EMAIL)).doesNotThrowAnyException();
        }

        @Test @DisplayName("lanza error si ya fue revocado")
        void lanza_error_si_ya_revocado() {
            RevokeRequest req = new RevokeRequest();
            req.setLinkId(LINK_ID);

            FamilyEcosystemLink link = activeLink(participant(NetworkType.INSTITUTIONAL));
            link.setStatus(EcosystemLinkStatus.REVOKED);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            assertThatThrownBy(() -> service.revoke(FAMILY_ID, req, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya fue revocado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // getSummary()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("getSummary()")
    class GetSummary {

        @Test @DisplayName("agrupa vínculos por tipo de red en el resumen")
        void agrupa_por_tipo_de_red() {
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(linkRepository.findByFamilyId(FAMILY_ID)).thenReturn(List.of(
                    activeLink(participant(NetworkType.FAMILIAR)),
                    activeLink(participant(NetworkType.INSTITUTIONAL)),
                    invitedLink(participant(NetworkType.COMMUNITY)),
                    activeLink(participant(NetworkType.TERRITORIAL))
            ));

            FamilyEcosystemSummary summary = service.getSummary(FAMILY_ID);

            assertThat(summary.getTotalLinks()).isEqualTo(4);
            assertThat(summary.getActiveLinks()).isEqualTo(3);
            assertThat(summary.getFamiliar()).hasSize(1);
            assertThat(summary.getInstitutional()).hasSize(1);
            assertThat(summary.getCommunity()).hasSize(1);
            assertThat(summary.getTerritorial()).hasSize(1);
        }

        @Test @DisplayName("lanza NOT_FOUND si la familia no existe")
        void lanza_not_found_familia() {
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSummary(FAMILY_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // getActiveLinks() / getLinksByNetwork()
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("Consultas de vínculos")
    class ConsultasVinculos {

        @Test @DisplayName("getActiveLinks devuelve solo vínculos ACTIVE")
        void get_active_links() {
            when(linkRepository.findByFamilyIdAndStatus(FAMILY_ID, EcosystemLinkStatus.ACTIVE))
                    .thenReturn(List.of(activeLink(participant(NetworkType.INSTITUTIONAL))));

            List<LinkResponse> list = service.getActiveLinks(FAMILY_ID);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStatus()).isEqualTo(EcosystemLinkStatus.ACTIVE);
        }

        @Test @DisplayName("getLinksByNetwork filtra por tipo de red")
        void get_links_by_network() {
            when(linkRepository.findByFamilyIdAndNetworkType(FAMILY_ID, NetworkType.COMMUNITY))
                    .thenReturn(List.of(activeLink(participant(NetworkType.COMMUNITY))));

            List<LinkResponse> list = service.getLinksByNetwork(FAMILY_ID, NetworkType.COMMUNITY);

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getNetworkType()).isEqualTo(NetworkType.COMMUNITY);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Flujo completo
    // ─────────────────────────────────────────────────────────────────────

    @Nested @DisplayName("Flujo completo: link → consent → revoke")
    class FlujoCicloDeVida {

        @Test @DisplayName("transiciona correctamente INVITED → ACTIVE → REVOKED")
        void flujo_completo() {
            EcosystemParticipant p = participant(NetworkType.INSTITUTIONAL);
            FamilyEcosystemLink link = invitedLink(p);

            // link
            LinkRequest linkReq = new LinkRequest();
            linkReq.setParticipantId(PARTICIPANT_ID);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family()));
            when(participantRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(p));
            when(linkRepository.existsByFamilyIdAndParticipantIdAndStatusNot(any(), any(), any())).thenReturn(false);
            when(linkRepository.save(any())).thenAnswer(inv -> {
                FamilyEcosystemLink l = inv.getArgument(0);
                if (l.getId() == null) l.setId(LINK_ID);
                return l;
            });

            LinkResponse linked = service.link(FAMILY_ID, linkReq, EMAIL);
            assertThat(linked.getStatus()).isEqualTo(EcosystemLinkStatus.INVITED);

            // consent
            ConsentRequest conReq = new ConsentRequest();
            conReq.setLinkId(LINK_ID);
            when(linkRepository.findById(LINK_ID)).thenReturn(Optional.of(link));

            LinkResponse consented = service.giveConsent(FAMILY_ID, conReq, EMAIL);
            assertThat(consented.getStatus()).isEqualTo(EcosystemLinkStatus.ACTIVE);

            // revoke
            RevokeRequest revReq = new RevokeRequest();
            revReq.setLinkId(LINK_ID);

            LinkResponse revoked = service.revoke(FAMILY_ID, revReq, EMAIL);
            assertThat(revoked.getStatus()).isEqualTo(EcosystemLinkStatus.REVOKED);
        }
    }
}
