package com.integrityfamily.sistemas.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.sistemas.catalog.SistemasCatalog;
import com.integrityfamily.sistemas.dto.SistemasDtos.CatalogResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.FourWeekCheckResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.ReviewResponse;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveCheckRequest;
import com.integrityfamily.sistemas.dto.SistemasDtos.SaveReviewRequest;
import com.integrityfamily.sistemas.service.SistemasService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SistemasController {

    private final SistemasService sistemasService;

    @GetMapping("/sistemas/catalog")
    public ApiResponse<CatalogResponse> catalog() {
        return ApiResponse.ok(SistemasCatalog.catalog());
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/families/{familyId}/sistemas/reviews")
    public ApiResponse<List<ReviewResponse>> history(@PathVariable Long familyId) {
        return ApiResponse.ok(sistemasService.history(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/families/{familyId}/sistemas/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> getReview(@PathVariable Long familyId, @PathVariable Long reviewId) {
        return ApiResponse.ok(sistemasService.getReview(familyId, reviewId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PostMapping("/families/{familyId}/sistemas/reviews")
    public ApiResponse<ReviewResponse> saveReview(@PathVariable Long familyId,
                                                  @RequestBody SaveReviewRequest request,
                                                  Principal principal) {
        return ApiResponse.ok(sistemasService.saveReview(familyId, principalName(principal), request),
                "Revisión guardada");
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @GetMapping("/families/{familyId}/sistemas/check")
    public ApiResponse<FourWeekCheckResponse> check(@PathVariable Long familyId) {
        return ApiResponse.ok(sistemasService.fourWeekCheck(familyId));
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PostMapping("/families/{familyId}/sistemas/check")
    public ApiResponse<FourWeekCheckResponse> saveCheck(@PathVariable Long familyId,
                                                        @RequestBody SaveCheckRequest request,
                                                        Principal principal) {
        return ApiResponse.ok(sistemasService.saveCheck(familyId, principalName(principal), request),
                "Prueba guardada");
    }

    private static String principalName(Principal principal) {
        return principal != null ? principal.getName() : null;
    }
}
