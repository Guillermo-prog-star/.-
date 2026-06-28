package com.integrityfamily.capital.controller;

import com.integrityfamily.capital.service.ObservatoryService;
import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.ObservatorySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Endpoints del Observatorio del Desarrollo Familiar.
 *
 * Datos completamente anonimizados — accesibles para cualquier usuario autenticado.
 * La generación manual está restringida a ROLE_ADMIN para evitar sobrecarga.
 */
@RestController
@RequestMapping("/api/observatory")
@RequiredArgsConstructor
public class ObservatoryController {

    private final ObservatoryService observatoryService;

    /** Historial de snapshots mensuales (máx. 24 meses = 2 años) */
    @GetMapping("/snapshots")
    public ResponseEntity<ApiResponse<List<ObservatorySnapshot>>> getHistory(
            @RequestParam(defaultValue = "12") int months) {
        int safeMonths = Math.min(Math.max(months, 1), 24);
        return ResponseEntity.ok(ApiResponse.ok(observatoryService.getHistory(safeMonths)));
    }

    /** Snapshot más reciente disponible */
    @GetMapping("/snapshots/latest")
    public ResponseEntity<ApiResponse<ObservatorySnapshot>> getLatest() {
        return observatoryService.getLatest()
                .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
                .orElse(ResponseEntity.ok(ApiResponse.error("No hay snapshots disponibles aún")));
    }

    /** Snapshot para un mes específico (formato: yyyy-MM, ej. 2026-05) */
    @GetMapping("/snapshots/{month}")
    public ResponseEntity<ApiResponse<ObservatorySnapshot>> getByMonth(
            @PathVariable String month) {
        try {
            YearMonth ym = YearMonth.parse(month);
            LocalDate monthStart = ym.atDay(1);
            return observatoryService.getRange(monthStart, monthStart)
                    .stream().findFirst()
                    .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
                    .orElse(ResponseEntity.ok(ApiResponse.error("No hay snapshot para " + month)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Formato de mes inválido. Use yyyy-MM"));
        }
    }

    /** Rango de meses (solo admin — puede generar muchas consultas) */
    @GetMapping("/snapshots/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ObservatorySnapshot>>> getRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(observatoryService.getRange(from, to)));
    }

    /** Generación manual de snapshot para un mes (solo admin — misma operación que el job) */
    @PostMapping("/snapshots/generate/{month}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ObservatorySnapshot>> generateManual(
            @PathVariable String month) {
        try {
            YearMonth ym = YearMonth.parse(month);
            ObservatorySnapshot snapshot = observatoryService.generateForMonth(ym);
            return ResponseEntity.ok(ApiResponse.ok(snapshot));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Error generando snapshot: " + e.getMessage()));
        }
    }
}
