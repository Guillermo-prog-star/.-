package com.integrityfamily.checklist.service;

import com.integrityfamily.checklist.domain.ChecklistItem;
import com.integrityfamily.checklist.repository.ChecklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistRepository checklistRepository;

    public List<ChecklistItem> findAll() {
        return checklistRepository.findAll();
    }

    public ChecklistItem findById(Long id) {
        return checklistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item no encontrado"));
    }

    public ChecklistItem create(ChecklistItem item) {
        return checklistRepository.save(item);
    }

    public ChecklistItem update(Long id, ChecklistItem request) {
        ChecklistItem existing = checklistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Checklist item no encontrado"));

        return checklistRepository.save(existing);
    }

    public void delete(Long id) {
        if (!checklistRepository.existsById(id)) {
            throw new RuntimeException("Checklist item no encontrado");
        }
        checklistRepository.deleteById(id);
    }
}