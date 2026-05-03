package com.integrityfamily.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String PLAN_QUEUE = "if.plan.queue";

    /**
     * DeclaraciÃƒÂ³n de la cola.
     * Durable(true) asegura que la cola no se borre si RabbitMQ se reinicia.
     */
    @Bean
    public Queue planQueue() {
        return QueueBuilder.durable(PLAN_QUEUE).build();
    }

    /**
     * Conversor JSON para que los objetos se envÃƒÂ­en como texto estructurado
     * y no como serializaciÃƒÂ³n binaria de Java (evita errores de ClassCast).
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}


