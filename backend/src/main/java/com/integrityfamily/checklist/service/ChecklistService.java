package com.integrityfamily.checklist.service;

import com.integrityfamily.checklist.domain.ChecklistItem;
import com.integrityfamily.checklist.repository.ChecklistItemRepository;
import com.integrityfamily.plan.domain.PlanTask;
import com.integrityfamily.plan.repository.PlanTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChecklistService {

    private final ChecklistItemRepository checklistItemRepository;
    private final PlanTaskRepository planTaskRepository;

    public ChecklistService(ChecklistItemRepository checklistItemRepository, 
                            PlanTaskRepository planTaskRepository) {
        this.checklistItemRepository = checklistItemRepository;
        this.planTaskRepository = planTaskRepository;
    }

    public List<ChecklistItem> findAll() {
        return checklistItemRepository.findAll();
    }

    public List<ChecklistItem> findByFamilyId(Long familyId) {
        return checklistItemRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    public ChecklistItem findById(Long id) {
        return checklistItemRepository.findById(id).orElseThrow();
    }

    public ChecklistItem create(ChecklistItem item) {
        return checklistItemRepository.save(item);
    }

    @Transactional
    public ChecklistItem completeItem(Long id, boolean completed) {
        ChecklistItem item = findById(id);
        item.setCompleted(completed);
        
        if (item.getPlanTask() != null) {
            item.getPlanTask().setCompleted(completed);
            planTaskRepository.save(item.getPlanTask());
        }
        
        return checklistItemRepository.save(item);
    }

    @Transactional
    public List<ChecklistItem> generateFromPlan(Long planId) {
        List<PlanTask> tasks = planTaskRepository.findByPlanIdOrderByCreatedAtAsc(planId);
        List<ChecklistItem> generated = new ArrayList<>();

        for (PlanTask task : tasks) {
            ChecklistItem item = new ChecklistItem();
            item.setTitle(task.getTitle());
            item.setCompleted(task.getCompleted());
            item.setPlanTask(task);
            if (task.getPlan() != null) {
                item.setFamily(task.getPlan().getFamily());
            }
            generated.add(checklistItemRepository.save(item));
        }

        return generated;
    }
}