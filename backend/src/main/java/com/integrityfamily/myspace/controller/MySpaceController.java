package com.integrityfamily.myspace.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.myspace.domain.PrivateJournalEntry;
import com.integrityfamily.myspace.repository.PrivateJournalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mi Espacio — Diario privado de reflexión emocional.
 * Los registros son exclusivos del usuario autenticado y nunca se comparten con la familia.
 */
@RestController
@RequestMapping("/api/private/journals")
@RequiredArgsConstructor
public class MySpaceController {

    private final PrivateJournalRepository journalRepository;
    private final UserRepository           userRepository;

    @GetMapping
    public ApiResponse<List<PrivateJournalEntry>> getEntries(Principal principal) {
        Long userId = resolveUserId(principal);
        return ApiResponse.ok(journalRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PrivateJournalEntry> createEntry(
            @RequestBody Map<String, String> body,
            Principal principal) {

        Long userId = resolveUserId(principal);

        PrivateJournalEntry entry = PrivateJournalEntry.builder()
                .userId(userId)
                .title(body.getOrDefault("title", "Sin título"))
                .content(body.getOrDefault("content", ""))
                .emotionalState(body.getOrDefault("emotionalState", "NEUTRAL"))
                .category(body.getOrDefault("category", "REFLEXION"))
                .build();

        return ApiResponse.ok(journalRepository.save(entry));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteEntry(@PathVariable Long id, Principal principal) {
        Long userId = resolveUserId(principal);
        journalRepository.findById(id).ifPresent(e -> {
            if (e.getUserId().equals(userId)) {
                journalRepository.delete(e);
            }
        });
        return ApiResponse.ok(null);
    }

    private Long resolveUserId(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));
    }
}
