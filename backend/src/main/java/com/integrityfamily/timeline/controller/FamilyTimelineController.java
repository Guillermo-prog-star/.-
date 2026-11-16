package com.integrityfamily.timeline.controller;

import com.integrityfamily.common.security.SecurityValidator;
import com.integrityfamily.timeline.dto.TimelineEventDto;
import com.integrityfamily.timeline.service.FamilyTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/families/{familyId}/timeline")
@RequiredArgsConstructor
public class FamilyTimelineController {

    private final FamilyTimelineService timelineService;
    private final SecurityValidator securityValidator;

    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<TimelineEventDto>> getTimeline(@PathVariable Long familyId, Principal principal) {
        Long viewerMemberId = securityValidator.resolveViewerMemberId(familyId, principal);
        return ResponseEntity.ok(timelineService.getTimeline(familyId, viewerMemberId));
    }
}
