package com.integrityfamily.bitacora.controller;

import com.integrityfamily.domain.UserJournal;
import com.integrityfamily.bitacora.service.UserJournalService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/private/journals")
@RequiredArgsConstructor
@Tag(name = "Bitácora Privada", description = "Endpoints para la gestión de la bitácora personal (Capa Privada).")
public class UserJournalController {

    private final UserJournalService userJournalService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Obtener entradas de bitácora del usuario", description = "Devuelve las entradas de la bitácora privada del usuario autenticado.")
    public ApiResponse<List<UserJournal>> getMyJournals() {
        Long userId = getCurrentUserId();
        return ApiResponse.ok(userJournalService.getUserJournals(userId));
    }

    @PostMapping
    @Operation(summary = "Crear entrada de bitácora privada", description = "Registra una nueva reflexión o estado emocional privado.")
    public ApiResponse<UserJournal> createJournal(@RequestBody UserJournal userJournal) {
        Long userId = getCurrentUserId();
        return ApiResponse.ok(userJournalService.createJournal(userId, userJournal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una entrada específica", description = "Devuelve los detalles de una entrada privada.")
    public ApiResponse<UserJournal> getJournalById(@PathVariable Long id) {
        return ApiResponse.ok(userJournalService.getJournalById(id));
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Usuario no autenticado");
        }
        String email = auth.getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado para email: " + email));
        return user.getId();
    }
}
