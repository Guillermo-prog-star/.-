package com.integrityfamily.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventPublisher — Unit Tests")
class EventPublisherTest {

    @Mock ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks EventPublisher eventPublisher;

    @Nested
    @DisplayName("publish()")
    class Publish {

        @Test
        @DisplayName("evento no nulo → delega en ApplicationEventPublisher")
        void shouldDelegateToApplicationPublisher_whenEventIsNotNull() {
            Object event = new Object();

            eventPublisher.publish(event);

            verify(applicationEventPublisher).publishEvent(event);
        }

        @Test
        @DisplayName("evento null → no invoca ApplicationEventPublisher")
        void shouldNotPublish_whenEventIsNull() {
            eventPublisher.publish(null);

            verifyNoInteractions(applicationEventPublisher);
        }

        @Test
        @DisplayName("publica el objeto exacto recibido, sin transformación")
        void shouldPublishExactObject() {
            String event = "family.crisis.triggered";

            eventPublisher.publish(event);

            verify(applicationEventPublisher).publishEvent(event);
            verifyNoMoreInteractions(applicationEventPublisher);
        }
    }
}
