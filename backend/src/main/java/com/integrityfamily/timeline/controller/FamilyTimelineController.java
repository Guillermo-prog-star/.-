package com.integrityfamily.timeline.controller;

import com.integrityfamily.timeline.dto.TimelineEventDto;
import com.integrityfamily.timeline.service.FamilyTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/families/{familyId}/timeline")
@RequiredArgsConstructor
public class FamilyTimelineController {

    private final FamilyTimelineService timelineService;

    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<TimelineEventDto>> getTimeline(@PathVariable Long familyId) {
        return ResponseEntity.ok(timelineService.getTimeline(familyId));
    }
}
