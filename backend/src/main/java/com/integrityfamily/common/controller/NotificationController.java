package com.integrityfamily.common.controller;

import com.integrityfamily.common.domain.NotificationLog;
import com.integrityfamily.common.repository.NotificationLogRepository;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping("/family/{familyId}")
    public ApiResponse<List<NotificationLog>> getByFamily(@PathVariable Long familyId) {
        return ApiResponse.ok(notificationLogRepository.findByFamilyIdOrderBySentAtDesc(familyId));
    }
}


