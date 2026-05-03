package com.integrityfamily.admin.controller;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/sentinel")
@RequiredArgsConstructor
public class SentinelAdminController {

    private final FamilyRepository familyRepository;

    @GetMapping("/families")
    public ResponseEntity<List<Family>> getAllFamilies() {
        log.info("[SDD] Consultando estado global de familias");
        return ResponseEntity.ok(familyRepository.findAll());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateSentinel(@PathVariable Long id) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));
        family.setSentinelActive(true);
        familyRepository.save(family);
        return ResponseEntity.ok().build();
    }
}


