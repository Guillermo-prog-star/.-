package com.integrityfamily.common.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * RestTemplateConfig
 *
 * Problema resuelto: StringHttpMessageConverter usa ISO-8859-1 por defecto.
 * Cuando Claude/OpenAI devuelven texto con caracteres UTF-8 (?, ?, ?, etc.),
 * el converter los decodifica como Latin-1, produciendo triple-encoding visible
 * como "Aún" en lugar de "A?n".
 *
 * Soluci?n: reemplazar el converter por uno configurado expl?citamente con UTF-8.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(60))
            .build();

        // Reemplazar StringHttpMessageConverter con uno UTF-8
        restTemplate.getMessageConverters()
            .stream()
            .filter(c -> c instanceof StringHttpMessageConverter)
            .map(c -> (StringHttpMessageConverter) c)
            .forEach(c -> c.setDefaultCharset(StandardCharsets.UTF_8));

        return restTemplate;
    }
}
