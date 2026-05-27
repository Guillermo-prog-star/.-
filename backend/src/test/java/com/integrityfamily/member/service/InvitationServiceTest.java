package com.integrityfamily.member.service;

import com.integrityfamily.auth.service.EmailService;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.WhatsAppService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link InvitationService}.
 *
 * No levanta contexto Spring — usa Mockito strict stubs.
 * Documenta la lógica de envío por WhatsApp y email con sus fallbacks.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InvitationService — Unit Tests")
class InvitationServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock WhatsAppService whatsAppService;
    @Mock EmailService emailService;

    @InjectMocks InvitationService service;

    private Family family;
    private FamilyMember member;

    @BeforeEach
    void setUp() {
        family = Family.builder()
                .id(1L)
                .name("Los García")
                .familyCode("IF-CO-BOG-2026-0001")
                .whatsapp("+573001234567")
                .build();

        member = FamilyMember.builder()
                .id(10L)
                .firstName("William")
                .fullName("William García")
                .email("william@example.com")
                .phone("+573009876543")
                .family(family)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  miembro no encontrado
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("miembro no encontrado → BusinessException MEMBER_NOT_FOUND 404")
    void sendInvitation_memberNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendInvitation(99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("MEMBER_NOT_FOUND");
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                });

        verifyNoInteractions(whatsAppService, emailService);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  envío por WhatsApp
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendInvitation() — canal WhatsApp")
    class WhatsAppChannel {

        @Test
        @DisplayName("miembro con teléfono propio → usa el teléfono del miembro")
        void usesOwnPhone() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            service.sendInvitation(10L);

            verify(whatsAppService).sendInvitation(
                    "+573009876543",       // teléfono del miembro
                    "William",             // firstName
                    "Los García",          // familyName
                    "IF-CO-BOG-2026-0001"  // familyCode
            );
        }

        @Test
        @DisplayName("miembro sin teléfono → usa el WhatsApp de la familia como fallback")
        void fallsBackToFamilyWhatsApp() {
            member = FamilyMember.builder()
                    .id(10L).firstName("William").fullName("William García")
                    .email("william@example.com").phone(null)
                    .family(family).build();

            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            service.sendInvitation(10L);

            verify(whatsAppService).sendInvitation(
                    "+573001234567",        // WhatsApp de la familia
                    "William",
                    "Los García",
                    "IF-CO-BOG-2026-0001"
            );
        }

        @Test
        @DisplayName("ni miembro ni familia tienen WhatsApp → no se envía WhatsApp")
        void noWhatsAppSent_whenBothNull() {
            Family noWaFamily = Family.builder()
                    .id(2L).name("Sin WA").familyCode("CODE-001").whatsapp(null).build();

            FamilyMember noWaMember = FamilyMember.builder()
                    .id(11L).firstName("Juan").fullName("Juan Sin WA")
                    .email("juan@example.com").phone(null)
                    .family(noWaFamily).build();

            when(memberRepository.findById(11L)).thenReturn(Optional.of(noWaMember));

            service.sendInvitation(11L);

            verifyNoInteractions(whatsAppService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  envío por Email
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendInvitation() — canal Email")
    class EmailChannel {

        @Test
        @DisplayName("miembro con email → se envía invitación al email del miembro")
        void sendsEmailToMember() {
            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            service.sendInvitation(10L);

            verify(emailService).sendInvitation(
                    "william@example.com",
                    "William",
                    "Los García",
                    "IF-CO-BOG-2026-0001"
            );
        }

        @Test
        @DisplayName("miembro sin email → no se envía email")
        void noEmailSent_whenNoEmail() {
            member = FamilyMember.builder()
                    .id(10L).firstName("William").fullName("William García")
                    .email(null).phone("+573009876543")
                    .family(family).build();

            when(memberRepository.findById(10L)).thenReturn(Optional.of(member));

            service.sendInvitation(10L);

            verifyNoInteractions(emailService);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  código de familia generado si está vacío
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("familia sin familyCode → genera 'FAM-<id>' como código de respaldo")
    void generatesCodeWhenMissing() {
        Family noCodeFamily = Family.builder()
                .id(5L).name("Sin Código").familyCode(null).build();

        FamilyMember noCodeMember = FamilyMember.builder()
                .id(20L).firstName("Ana").fullName("Ana Sin Código")
                .email("ana@example.com").phone(null)
                .family(noCodeFamily).build();

        when(memberRepository.findById(20L)).thenReturn(Optional.of(noCodeMember));

        service.sendInvitation(20L);

        // Sin WhatsApp (phone=null y family.whatsapp=null) → no se llama whatsApp
        // Con email → se pasa el código generado "FAM-5"
        verify(emailService).sendInvitation(
                "ana@example.com",
                "Ana",
                "Sin Código",
                "FAM-5"
        );
    }
}
