package com.integrityfamily.common.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SDD: Configuración de Infraestructura de Eventos (Event-Driven Core).
 * Implementa el Exchange Central 'if.events' para el desacoplamiento modular.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "if.events";
    
    // Colas de Dominio
    public static final String BITACORA_QUEUE = "if.queue.bitacora";
    public static final String ANALYTICS_QUEUE = "if.queue.analytics";
    public static final String AI_INSIGHTS_QUEUE = "if.queue.ai.insights";
    public static final String PLAN_QUEUE = "if.queue.plan.generation";

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue bitacoraQueue() {
        return QueueBuilder.durable(BITACORA_QUEUE).build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE).build();
    }

    @Bean
    public Queue aiQueue() {
        return QueueBuilder.durable(AI_INSIGHTS_QUEUE).build();
    }

    @Bean
    public Queue planQueue() {
        return QueueBuilder.durable(PLAN_QUEUE).build();
    }

    // Bindings Estratégicos (Escucha selectiva de eventos)
    @Bean
    public Binding bitacoraBinding(Queue bitacoraQueue, TopicExchange eventExchange) {
        // La bitácora escucha TODO lo que pase en el sistema para capturar aprendizaje
        return BindingBuilder.bind(bitacoraQueue).to(eventExchange).with("#");
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(analyticsQueue).to(eventExchange).with("*.#");
    }

    @Bean
    public Binding aiInsightsBinding(Queue aiQueue, TopicExchange eventExchange) {
        // La IA escucha específicamente cuando termina una evaluación para generar el insight
        return BindingBuilder.bind(aiQueue).to(eventExchange).with("evaluation.completed");
    }

    @Bean
    public Binding planBinding(Queue planQueue, TopicExchange eventExchange) {
        // EL PLAN HÍBRIDO debe generarse siempre que termine una evaluación
        return BindingBuilder.bind(planQueue).to(eventExchange).with("evaluation.completed");
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(producerJackson2MessageConverter());
        return template;
    }
}
