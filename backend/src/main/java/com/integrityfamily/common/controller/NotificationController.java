package com.integrityfamily.common.controller;

import com.integrityfamily.common.domain.NotificationLog;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;
    private final UserRepository userRepository;

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<NotificationLog>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(notificationLogRepository.findByFamilyIdOrderBySentAtDesc(familyId));
    }

    /** Returns the 20 most recent notifications for the authenticated user's family. */
    @GetMapping("/mine")
    @Transactional(readOnly = true)
    public ApiResponse<List<NotificationLog>> getMine(Principal principal) {
        Long familyId = resolveFamilyId(principal);
        if (familyId == null) return ApiResponse.ok(List.of());
        return ApiResponse.ok(
            notificationLogRepository.findByFamilyIdOrderBySentAtDesc(familyId, PageRequest.of(0, 20))
        );
    }

    /** Returns the count of unread notifications for the authenticated user's family. */
    @GetMapping("/mine/unread-count")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Long>> getUnreadCount(Principal principal) {
        Long familyId = resolveFamilyId(principal);
        long count = familyId != null ? notificationLogRepository.countByFamilyIdAndViewedFalse(familyId) : 0L;
        return ApiResponse.ok(Map.of("count", count));
    }

    /** Marks all unread notifications as viewed for the authenticated user's family. */
    @PutMapping("/mine/mark-all-read")
    @Transactional
    public ApiResponse<Void> markAllRead(Principal principal) {
        Long familyId = resolveFamilyId(principal);
        if (familyId != null) notificationLogRepository.markAllViewedByFamilyId(familyId);
        return ApiResponse.ok(null);
    }

    private Long resolveFamilyId(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName())
                .map(u -> u.getFamily() != null ? u.getFamily().getId() : null)
                .orElse(null);
    }
}


