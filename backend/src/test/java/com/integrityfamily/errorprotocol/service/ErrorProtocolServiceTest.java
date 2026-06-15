package com.integrityfamily.errorprotocol.service;

import com.integrityfamily.errorprotocol.domain.FamilyErrorProtocol;
import com.integrityfamily.errorprotocol.domain.FamilyErrorProtocol.ProtocolStep;
import com.integrityfamily.errorprotocol.repository.ErrorProtocolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErrorProtocolService")
class ErrorProtocolServiceTest {

    @Mock ErrorProtocolRepository repo;
    @InjectMocks ErrorProtocolService service;

    private static final long FAM_ID  = 1L;
    private static final long PROT_ID = 99L;

    private FamilyErrorProtocol protocol() {
        return FamilyErrorProtocol.builder()
                .id(PROT_ID).familyId(FAM_ID).missionFailed("Misión X").build();
    }

    // ── getAll / getOpen ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAll / getOpen")
    class Queries {

        @Test
        @DisplayName("getAll → delega a repo ordenado por createdAt desc")
        void getAll_delegatesToRepo() {
            List<FamilyErrorProtocol> list = List.of(protocol());
            when(repo.findByFamilyIdOrderByCreatedAtDesc(FAM_ID)).thenReturn(list);

            assertThat(service.getAll(FAM_ID)).isSameAs(list);
        }

        @Test
        @DisplayName("getOpen → delega a repo filtrando closed=false")
        void getOpen_delegatesToRepo() {
            List<FamilyErrorProtocol> open = List.of(protocol());
            when(repo.findByFamilyIdAndClosedFalse(FAM_ID)).thenReturn(open);

            assertThat(service.getOpen(FAM_ID)).isSameAs(open);
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("crea y guarda protocolo con familyId y missionFailed")
        void saveWithCorrectFields() {
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyErrorProtocol result = service.create(FAM_ID, "Misión fallida");

            assertThat(result.getFamilyId()).isEqualTo(FAM_ID);
            assertThat(result.getMissionFailed()).isEqualTo("Misión fallida");
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("no encontrado → RuntimeException con id en mensaje")
        void notFound_throwsRuntimeException() {
            when(repo.findById(PROT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(PROT_ID, Map.of()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(String.valueOf(PROT_ID));
        }

        @Test
        @DisplayName("campos presentes en map → actualizados en entidad")
        void presentFields_updated() {
            FamilyErrorProtocol p = protocol();
            when(repo.findById(PROT_ID)).thenReturn(Optional.of(p));
            when(repo.save(p)).thenReturn(p);

            service.update(PROT_ID, Map.of(
                    "feelings", "Frustración",
                    "whatHappened", "Se perdió el hilo",
                    "correctiveAction", "Hablar con calma",
                    "whoHelps", "Papá",
                    "agreement", "Reunión semanal",
                    "learning", "Escucha activa"));

            assertThat(p.getFeelings()).isEqualTo("Frustración");
            assertThat(p.getWhatHappened()).isEqualTo("Se perdió el hilo");
            assertThat(p.getCorrectiveAction()).isEqualTo("Hablar con calma");
            assertThat(p.getWhoHelps()).isEqualTo("Papá");
            assertThat(p.getAgreement()).isEqualTo("Reunión semanal");
            assertThat(p.getLearning()).isEqualTo("Escucha activa");
        }

        @Test
        @DisplayName("currentStep=FOLLOWUP → enum parseado y asignado")
        void currentStep_parsedToEnum() {
            FamilyErrorProtocol p = protocol();
            when(repo.findById(PROT_ID)).thenReturn(Optional.of(p));
            when(repo.save(p)).thenReturn(p);

            service.update(PROT_ID, Map.of("currentStep", "FOLLOWUP"));

            assertThat(p.getCurrentStep()).isEqualTo(ProtocolStep.FOLLOWUP);
        }

        @Test
        @DisplayName("map vacío → ningún campo cambia, pero igual guarda")
        void emptyMap_noChanges_butSaves() {
            FamilyErrorProtocol p = protocol();
            ProtocolStep originalStep = p.getCurrentStep();
            when(repo.findById(PROT_ID)).thenReturn(Optional.of(p));
            when(repo.save(p)).thenReturn(p);

            service.update(PROT_ID, Map.of());

            assertThat(p.getCurrentStep()).isEqualTo(originalStep);
            verify(repo).save(p);
        }
    }

    // ── close ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("close")
    class Close {

        @Test
        @DisplayName("no encontrado → RuntimeException")
        void notFound_throwsRuntimeException() {
            when(repo.findById(PROT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.close(PROT_ID, "Aprendí X"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(String.valueOf(PROT_ID));
        }

        @Test
        @DisplayName("encontrado → closed=true, closedAt asignado, step=LEARNING, learning guardado")
        void found_closedCorrectly() {
            FamilyErrorProtocol p = protocol();
            when(repo.findById(PROT_ID)).thenReturn(Optional.of(p));
            when(repo.save(p)).thenReturn(p);

            service.close(PROT_ID, "Paciencia ante todo");

            assertThat(p.isClosed()).isTrue();
            assertThat(p.getClosedAt()).isNotNull();
            assertThat(p.getCurrentStep()).isEqualTo(ProtocolStep.LEARNING);
            assertThat(p.getLearning()).isEqualTo("Paciencia ante todo");
            verify(repo).save(p);
        }
    }
}
