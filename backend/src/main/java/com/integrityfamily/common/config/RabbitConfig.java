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
    public static final String DLX_NAME = "if.dlx";
    
    // Colas de Dominio
    public static final String BITACORA_QUEUE = "if.queue.bitacora";
    public static final String ANALYTICS_QUEUE = "if.queue.analytics";
    public static final String AI_INSIGHTS_QUEUE = "if.queue.ai.insights";
    public static final String PLAN_QUEUE = "if.queue.plan.generation";
    public static final String SUGGESTED_TASKS_QUEUE = "if.queue.suggested.tasks";
    public static final String EVIDENCE_ANALYSIS_QUEUE = "if.queue.evidence.analysis";
    public static final String AI_INFERENCE_QUEUE = "q.ai.inference";
    public static final String WEBSOCKET_DASHBOARD_QUEUE = "if.queue.websocket.dashboard";
    public static final String DLQ_NAME = "if.queue.deadletter";

    // Exchanges Adicionales
    public static final String AI_EVENTS_EXCHANGE = "x.ai.events";

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public TopicExchange aiEventsExchange() {
        return new TopicExchange(AI_EVENTS_EXCHANGE);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        // Enrutar cualquier mensaje rechazado (*.dead) hacia la DLQ centralizada
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("#.dead");
    }

    @Bean
    public Queue bitacoraQueue() {
        return QueueBuilder.durable(BITACORA_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "bitacora.dead")
                .build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "analytics.dead")
                .build();
    }

    @Bean
    public Queue aiQueue() {
        return QueueBuilder.durable(AI_INSIGHTS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "ai.insights.dead")
                .build();
    }

    @Bean
    public Queue planQueue() {
        return QueueBuilder.durable(PLAN_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "plan.generation.dead")
                .build();
    }

    @Bean
    public Queue suggestedTasksQueue() {
        return QueueBuilder.durable(SUGGESTED_TASKS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "suggested.tasks.dead")
                .build();
    }

    @Bean
    public Queue evidenceAnalysisQueue() {
        return QueueBuilder.durable(EVIDENCE_ANALYSIS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "evidence.analysis.dead")
                .build();
    }

    @Bean
    public Queue aiInferenceQueue() {
        return QueueBuilder.durable(AI_INFERENCE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "ai.crisis.dead")
                .build();
    }

    @Bean
    public Queue websocketDashboardQueue() {
        return QueueBuilder.durable(WEBSOCKET_DASHBOARD_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "websocket.dashboard.dead")
                .build();
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
    public Binding suggestedTasksBinding(Queue suggestedTasksQueue, TopicExchange eventExchange) {
        // El plan de acción escucha de forma reactiva las recomendaciones del dashboard
        return BindingBuilder.bind(suggestedTasksQueue).to(eventExchange).with("tasks.suggested");
    }

    @Bean
    public Binding evidenceAnalysisBinding(Queue evidenceAnalysisQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(evidenceAnalysisQueue).to(eventExchange).with("evidence.submitted");
    }

    @Bean
    public Binding aiInferenceBinding(Queue aiInferenceQueue, TopicExchange aiEventsExchange) {
        return BindingBuilder.bind(aiInferenceQueue).to(aiEventsExchange).with("crisis.detected");
    }

    @Bean
    public Binding websocketDashboardBinding(Queue websocketDashboardQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(websocketDashboardQueue).to(eventExchange).with("evaluation.completed");
    }

    @Bean
    public Binding websocketDashboardTaskBinding(Queue websocketDashboardQueue, TopicExchange eventExchange) {
        return BindingBuilder.bind(websocketDashboardQueue).to(eventExchange).with("task.completed");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
