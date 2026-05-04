package com.integrityfamily.ai.config;

import org.springframework.context.annotation.Configuration;

/**
 * SDD: Configuración neutralizada para evitar colisión con RabbitConfig centralizado.
 * Toda la configuración de RabbitMQ para el módulo AI ahora reside en com.integrityfamily.common.config.RabbitConfig
 */
@Configuration
public class AiRabbitConfig {
    // Configuración movida a RabbitConfig para centralización arquitectónica.
}
