package com.integrityfamily.simulation.controller;

import com.integrityfamily.simulation.service.SentinelSimulationService;
import com.integrityfamily.simulation.service.BetaLauncherService;
import com.integrityfamily.simulation.service.AlphaLaunchService;
import com.integrityfamily.simulation.service.CrisisSimulationService;
import com.integrityfamily.simulation.service.TrendSimulationService;
import com.integrityfamily.auth.service.MasterCredentialService;
import com.integrityfamily.domain.Family;
import com.integrityfamily.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SentinelSimulationService simulationService;
    private final BetaLauncherService betaLauncherService;
    private final AlphaLaunchService alphaLaunchService;
    private final MasterCredentialService masterCredentialService;
    private final CrisisSimulationService crisisSimulationService;
    private final TrendSimulationService trendSimulationService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/provision-master")
    public String provisionMaster() {
        masterCredentialService.provisionMasterAdmin();
        return "Credenciales maestras de William provisionadas.";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/trigger-crisis-test")
    public ApiResponse<String> triggerCrisisTest() {
        return ApiResponse.ok(crisisSimulationService.triggerGlobalCrisisTest());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/trigger-mass-failure")
    public ApiResponse<String> triggerMassFailure() {
        return ApiResponse.ok(trendSimulationService.triggerMassiveMilestoneFailure());
    }

    @PreAuthorize("@familySecurity.check(#familyId)")
    @PostMapping("/burst/{familyId}")
    public String runBurst(@PathVariable Long familyId) {
        return simulationService.runBurstSimulation(familyId);
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/launch-beta")
    public ApiResponse<String> launchBeta(@RequestParam String email) {
        return ApiResponse.ok(betaLauncherService.launch(email));
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/provision-alpha")
    public ApiResponse<String> provisionAlpha(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam String creatorEmail) {
        List<Family> families = alphaLaunchService.provisionAlphaFamilies(count, creatorEmail);
        return ApiResponse.ok("Provisionadas " + families.size() + " familias alfa para " + creatorEmail);
    }
}


