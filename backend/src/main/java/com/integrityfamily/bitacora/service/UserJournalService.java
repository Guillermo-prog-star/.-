package com.integrityfamily.bitacora.service;

import com.integrityfamily.domain.UserJournal;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserJournalRepository;
import com.integrityfamily.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserJournalService {

    private final UserJournalRepository userJournalRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserJournal> getUserJournals(Long userId) {
        log.info("📖 [USER_JOURNAL] Obteniendo entradas para usuario ID: {}", userId);
        return userJournalRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public UserJournal createJournal(Long userId, UserJournal journal) {
        log.info("📝 [USER_JOURNAL] Creando entrada para usuario ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
        
        journal.setUser(user);
        return userJournalRepository.save(journal);
    }

    @Transactional(readOnly = true)
    public UserJournal getJournalById(Long id) {
        return userJournalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entrada de bitácora no encontrada: " + id));
    }
}
