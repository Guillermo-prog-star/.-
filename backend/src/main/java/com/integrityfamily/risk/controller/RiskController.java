package com.integrityfamily.risk.controller;

import com.integrityfamily.risk.domain.RiskSnapshot;
import com.integrityfamily.risk.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping
    public List<RiskSnapshot> getAll() {
        return riskService.findAll();
    }

    @GetMapping("/{id}")
    public RiskSnapshot getById(@PathVariable Long id) {
        return riskService.findById(id);
    }

    @PostMapping
    public RiskSnapshot create(@RequestBody RiskSnapshot snapshot) {
        return riskService.create(snapshot);
    }
}