package com.integrityfamily.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiRabbitConfig {
    @Bean
    public Queue aiQueue() { return new Queue("q.ai.inference", true); }

    @Bean
    public DirectExchange aiExchange() { return new DirectExchange("x.ai.events"); }

    @Bean
    public Binding aiBinding(Queue aiQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(aiQueue).to(aiExchange).with("crisis.detected");
    }
}


