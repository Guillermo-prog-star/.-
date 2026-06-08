package com.integrityfamily.movie.controller;

import com.integrityfamily.movie.dto.FamilyMovieDto;
import com.integrityfamily.movie.service.FamilyMovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/families/{familyId}/movies")
@RequiredArgsConstructor
public class FamilyMovieController {

    private final FamilyMovieService movieService;

    /** Lista todas las películas de la familia. */
    @GetMapping
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<List<FamilyMovieDto>> list(@PathVariable Long familyId) {
        return ResponseEntity.ok(movieService.listMovies(familyId));
    }

    /** Devuelve la película más reciente. */
    @GetMapping("/latest")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<FamilyMovieDto> latest(@PathVariable Long familyId) {
        return movieService.getLatest(familyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Genera la película del trimestre actual. */
    @PostMapping("/generate/quarter")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<FamilyMovieDto> generateQuarter(@PathVariable Long familyId) {
        return ResponseEntity.ok(movieService.generateCurrentQuarter(familyId));
    }

    /** Genera la película para un rango personalizado. */
    @PostMapping("/generate")
    @PreAuthorize("@familySecurity.check(#familyId)")
    public ResponseEntity<FamilyMovieDto> generate(
            @PathVariable Long familyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(movieService.generate(familyId, from, to));
    }
}
