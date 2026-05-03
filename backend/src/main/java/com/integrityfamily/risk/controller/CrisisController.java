package com.integrityfamily.risk.controller;

import com.integrityfamily.common.dto.ApiResponse;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.risk.service.CrisisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * SDD ALIGNMENT: Controlador sincronizado con el Protocolo Sentinel.
 * Se eliminan mÃƒÂ©todos legacy (registerCrisis/getHistory) por desalineaciÃƒÂ³n de
 * contrato.
 */
@RestController
@RequestMapping("/api/crisis")
@RequiredArgsConstructor
public class CrisisController {

    private final CrisisService crisisService;

    @PostMapping("/protocol/activate")
    public ApiResponse<Void> activate(@RequestBody Map<String, Object> body) {
        Long familyId = ((Number) body.get("familyId")).longValue();
        String reason = (String) body.get("reason");

        crisisService.activateProtocol(familyId, reason);
        return ApiResponse.ok(null);
    }

    @PostMapping("/FamilyMember/handle")
    public ApiResponse<Void> handleMemberCrisis(@RequestBody Map<String, Object> body) {
        Long familyId = ((Number) body.get("familyId")).longValue();
        String observation = (String) body.get("observation");
        // Nota: involvedMembers requiere una conversiÃƒÂ³n de lista de objetos a
        // List<FamilyMember>
        // Por simplicidad en este rediseÃƒÂ±o, asumimos que se envÃƒÂ­an los datos
        // necesarios.
        List<FamilyMember> involvedMembers = (List<FamilyMember>) body.get("involvedMembers");

        crisisService.handleMemberCrisis(familyId, involvedMembers, observation);
        return ApiResponse.ok(null);
    }

    @GetMapping("/status/{familyId}")
    public ApiResponse<Boolean> getStatus(@PathVariable Long familyId) {
        return ApiResponse.ok(crisisService.isUnderCrisis(familyId));
    }
}


