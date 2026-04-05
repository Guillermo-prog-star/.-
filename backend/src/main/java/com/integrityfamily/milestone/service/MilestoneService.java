package com.integrityfamily.milestone.service;

import com.integrityfamily.family.domain.Family;
import com.integrityfamily.family.repository.FamilyRepository;
import com.integrityfamily.milestone.domain.Milestone;
import com.integrityfamily.milestone.repository.MilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneRepository milestoneRepository;
    private final FamilyRepository familyRepository;

    public List<Milestone> findAll() {
        return milestoneRepository.findAll();
    }

    public Milestone findById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Milestone no encontrado"));
    }

    public Milestone create(Milestone milestone) {
        return milestoneRepository.save(milestone);
    }

    public Milestone update(Long id, Milestone request) {
        Milestone existing = milestoneRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Milestone no encontrado"));

        return milestoneRepository.save(existing);
    }

    public void delete(Long id) {
        if (!milestoneRepository.existsById(id)) {
            throw new RuntimeException("Milestone no encontrado");
        }
        milestoneRepository.deleteById(id);
    }

    public String getCurrentMilestoneLabel(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));

        return "inicio";
    }

    public String advanceMilestone(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new RuntimeException("Familia no encontrada"));

        return "inicio";
    }
}