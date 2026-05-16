package com.integrityfamily.domain.repository;

import com.integrityfamily.domain.UserJournal;
import com.integrityfamily.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserJournalRepository extends JpaRepository<UserJournal, Long> {
    
    List<UserJournal> findByUserOrderByCreatedAtDesc(User user);
    
    List<UserJournal> findByUserIdOrderByCreatedAtDesc(Long userId);
}
