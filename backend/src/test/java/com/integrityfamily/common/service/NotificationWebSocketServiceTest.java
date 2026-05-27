package com.integrityfamily.common.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationWebSocketService")
class NotificationWebSocketServiceTest {

    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks NotificationWebSocketService service;

    @Test
    @DisplayName("sendToFamily construye el topic '/topic/family/{id}{destination}' correcto")
    void sendToFamily_sendsToCorrectTopic() {
        service.sendToFamily(42L, "/alerts", "payload");

        verify(messagingTemplate).convertAndSend("/topic/family/42/alerts", "payload");
    }

    @Test
    @DisplayName("sendToAll construye el topic '/topic{destination}' correcto")
    void sendToAll_sendsToCorrectTopic() {
        service.sendToAll("/global-update", 99);

        verify(messagingTemplate).convertAndSend("/topic/global-update", (Object) 99);
    }
}
