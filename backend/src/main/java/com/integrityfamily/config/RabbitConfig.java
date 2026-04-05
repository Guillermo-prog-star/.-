package com.integrityfamily.config;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class RabbitConfig {
    @Value("${app.messaging.exchange}") private String exchange;
    @Value("${app.messaging.queues.audit}") private String auditQ;
    @Value("${app.messaging.queues.risk}")  private String riskQ;
    @Value("${app.messaging.queues.plan}")  private String planQ;
    @Bean public TopicExchange integrityExchange() { return new TopicExchange(exchange); }
    @Bean public Queue auditQueue() { return QueueBuilder.durable(auditQ).build(); }
    @Bean public Queue riskQueue()  { return QueueBuilder.durable(riskQ).build(); }
    @Bean public Queue planQueue()  { return QueueBuilder.durable(planQ).build(); }
    @Bean public Binding auditBinding(Queue auditQueue, TopicExchange ie) { return BindingBuilder.bind(auditQueue).to(ie).with("#"); }
    @Bean public Binding riskBinding(Queue riskQueue,  TopicExchange ie) { return BindingBuilder.bind(riskQueue).to(ie).with("evaluation.completed"); }
    @Bean public Binding planBinding(Queue planQueue,  TopicExchange ie) { return BindingBuilder.bind(planQueue).to(ie).with("evaluation.completed"); }
    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        var t = new RabbitTemplate(cf); t.setMessageConverter(messageConverter()); return t;
    }
}
