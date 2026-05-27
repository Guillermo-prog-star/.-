package com.integrityfamily.bitacora.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.UserJournal;
import com.integrityfamily.domain.repository.UserJournalRepository;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link UserJournalService}.
 *
 * No levanta contexto Spring — usa Mockito strict stubs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserJournalService — Unit Tests")
class UserJournalServiceTest {

    @Mock UserJournalRepository journalRepository;
    @Mock UserRepository userRepository;

    @InjectMocks UserJournalService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@if.com")
                .fullName("William Test")
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getUserJournals()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getUserJournals() — listar entradas por usuario")
    class GetUserJournals {

        @Test
        @DisplayName("retorna lista en orden descendente de creación")
        void getUserJournals_returnsList() {
            LocalDateTime earlier = LocalDateTime.now().minusDays(2);
            LocalDateTime later   = LocalDateTime.now().minusDays(1);

            UserJournal j1 = UserJournal.builder().id(1L).user(user).title("Día difícil")
                    .content("Reflexión").createdAt(earlier).build();
            UserJournal j2 = UserJournal.builder().id(2L).user(user).title("Día feliz")
                    .content("Logro").createdAt(later).build();

            // Repositorio ya devuelve en orden desc
            when(journalRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(j2, j1));

            List<UserJournal> result = service.getUserJournals(1L);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(1).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("lista vacía cuando el usuario no tiene entradas")
        void getUserJournals_emptyList() {
            when(journalRepository.findByUserIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of());

            assertThat(service.getUserJournals(1L)).isEmpty();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  createJournal()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createJournal() — crear entrada en bitácora personal")
    class CreateJournal {

        @Test
        @DisplayName("éxito: asigna el usuario al journal y persiste")
        void createJournal_success_assignsUserAndSaves() {
            UserJournal journal = UserJournal.builder()
                    .title("Mi reflexión")
                    .content("Hoy aprendí algo importante")
                    .emotionalState("Tranquilo")
                    .build();

            UserJournal saved = UserJournal.builder()
                    .id(10L).user(user).title("Mi reflexión")
                    .content("Hoy aprendí algo importante").build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(journalRepository.save(any(UserJournal.class))).thenReturn(saved);

            UserJournal result = service.createJournal(1L, journal);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getUser()).isEqualTo(user);
            verify(journalRepository).save(any(UserJournal.class));
        }

        @Test
        @DisplayName("la instancia guardada recibe el usuario correcto antes del save")
        void createJournal_setsUserOnJournalBeforeSave() {
            UserJournal journal = UserJournal.builder()
                    .title("Reflexión").content("Contenido").build();

            ArgumentCaptor<UserJournal> captor = ArgumentCaptor.forClass(UserJournal.class);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(journalRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.createJournal(1L, journal);

            assertThat(captor.getValue().getUser()).isSameAs(user);
        }

        @Test
        @DisplayName("usuario no existe → BusinessException USER_NOT_FOUND 404")
        void createJournal_userNotFound_throwsBusinessException() {
            UserJournal journal = UserJournal.builder()
                    .title("Reflexión").content("Contenido").build();

            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createJournal(99L, journal))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("USER_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(journalRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  getJournalById()
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getJournalById() — obtener entrada por ID")
    class GetJournalById {

        @Test
        @DisplayName("éxito: retorna la entrada cuando existe")
        void getJournalById_found() {
            UserJournal journal = UserJournal.builder()
                    .id(5L).user(user).title("Mi día").content("Fue bueno").build();

            when(journalRepository.findById(5L)).thenReturn(Optional.of(journal));

            UserJournal result = service.getJournalById(5L);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getTitle()).isEqualTo("Mi día");
        }

        @Test
        @DisplayName("ID no existe → BusinessException JOURNAL_NOT_FOUND 404")
        void getJournalById_notFound_throwsBusinessException() {
            when(journalRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getJournalById(999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getCode()).isEqualTo("JOURNAL_NOT_FOUND");
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }
}
