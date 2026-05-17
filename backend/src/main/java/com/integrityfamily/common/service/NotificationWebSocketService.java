package com.integrityfamily.common.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendToFamily(Long familyId, String destination, Object payload) {
        messagingTemplate.convertAndSend("/topic/family/" + familyId + destination, payload);
    }
    
    public void sendToAll(String destination, Object payload) {
        messagingTemplate.convertAndSend("/topic" + destination, payload);
    }
}
