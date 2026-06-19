package com.integrityfamily.myspace.controller;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.myspace.domain.PrivateJournalEntry;
import com.integrityfamily.myspace.repository.PrivateJournalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MySpaceController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("MySpaceController — Diario Privado")
class MySpaceControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PrivateJournalRepository journalRepository;
    @MockitoBean UserRepository           userRepository;
    @MockitoBean com.integrityfamily.security.JwtAuthenticationFilter jwtAuthFilter;
    @MockitoBean com.integrityfamily.security.TenantInterceptor       tenantInterceptor;

    private static final Long   USER_ID = 10L;
    private static final String EMAIL   = "maria@test.com";

    @BeforeEach
    void setUp() throws Exception {
        User user = User.builder().id(USER_ID).email(EMAIL).build();
        Mockito.lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Mockito.lenient().when(tenantInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    // ─── GET /api/private/journals ─────────────────────────────────────────────

    @Nested
    @DisplayName("getEntries()")
    class GetEntries {

        @Test
        @DisplayName("usuario sin entradas → data es array vacío")
        void noEntries_returnsEmptyArray() throws Exception {
            Mockito.when(journalRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/private/journals").principal(() -> EMAIL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("usuario con entradas → lista retornada con id y título")
        void withEntries_returnsList() throws Exception {
            PrivateJournalEntry entry = PrivateJournalEntry.builder()
                    .id(1L).userId(USER_ID)
                    .title("Mi primer diario").content("Hoy fue un buen día")
                    .build();
            Mockito.when(journalRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(entry));

            mockMvc.perform(get("/api/private/journals").principal(() -> EMAIL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("Mi primer diario"));
        }
    }

    // ─── POST /api/private/journals ───────────────────────────────────────────

    @Nested
    @DisplayName("createEntry()")
    class CreateEntry {

        @Test
        @DisplayName("body completo → entrada guardada con userId correcto")
        void fullBody_savedWithCorrectUserId() throws Exception {
            PrivateJournalEntry saved = PrivateJournalEntry.builder()
                    .id(5L).userId(USER_ID)
                    .title("Reflexión").content("Contenido").build();
            Mockito.when(journalRepository.save(any())).thenReturn(saved);

            mockMvc.perform(post("/api/private/journals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"Reflexión\",\"content\":\"Contenido\",\"emotionalState\":\"ALEGRE\"}")
                    .principal(() -> EMAIL))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(5))
                    .andExpect(jsonPath("$.data.userId").value(USER_ID));
        }

        @Test
        @DisplayName("body vacío → usa defaults: 'Sin título', emotionalState=NEUTRAL, category=REFLEXION")
        void emptyBody_usesDefaults() throws Exception {
            PrivateJournalEntry saved = PrivateJournalEntry.builder()
                    .id(6L).userId(USER_ID)
                    .title("Sin título").content("")
                    .build();
            Mockito.when(journalRepository.save(argThat(e ->
                    e.getTitle().equals("Sin título") &&
                    e.getContent().equals("") &&
                    e.getEmotionalState().equals("NEUTRAL") &&
                    e.getCategory().equals("REFLEXION")
            ))).thenReturn(saved);

            mockMvc.perform(post("/api/private/journals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .principal(() -> EMAIL))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Sin título"));
        }
    }

    // ─── DELETE /api/private/journals/{id} ────────────────────────────────────

    @Nested
    @DisplayName("deleteEntry()")
    class DeleteEntry {

        @Test
        @DisplayName("entrada propia → eliminada y 200 ok")
        void ownEntry_deletedSuccessfully() throws Exception {
            PrivateJournalEntry entry = PrivateJournalEntry.builder()
                    .id(3L).userId(USER_ID).title("Borrar esto").build();
            Mockito.when(journalRepository.findById(3L)).thenReturn(Optional.of(entry));

            mockMvc.perform(delete("/api/private/journals/3").principal(() -> EMAIL))
                    .andExpect(status().isOk());

            Mockito.verify(journalRepository).delete(entry);
        }

        @Test
        @DisplayName("entrada ajena → NO eliminada (userId no coincide)")
        void foreignEntry_notDeleted() throws Exception {
            PrivateJournalEntry entry = PrivateJournalEntry.builder()
                    .id(4L).userId(999L).title("Ajena").build();
            Mockito.when(journalRepository.findById(4L)).thenReturn(Optional.of(entry));

            mockMvc.perform(delete("/api/private/journals/4").principal(() -> EMAIL))
                    .andExpect(status().isOk());

            Mockito.verify(journalRepository, Mockito.never()).delete(any());
        }

        @Test
        @DisplayName("entrada inexistente → 200 ok sin efecto (ifPresent no lanza excepción)")
        void nonExistentEntry_noException() throws Exception {
            Mockito.when(journalRepository.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/api/private/journals/99").principal(() -> EMAIL))
                    .andExpect(status().isOk());

            Mockito.verify(journalRepository, Mockito.never()).delete(any());
        }
    }
}
