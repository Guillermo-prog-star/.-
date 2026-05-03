package com.integrityfamily.common.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info(">>>> [SDD] Iniciando sembrado de datos post-Hibernate...");
        try {
            ClassPathResource resource = new ClassPathResource("data.sql");
            if (resource.exists()) {
                byte[] bdata = FileCopyUtils.copyToByteArray(resource.getInputStream());
                String sql = new String(bdata, StandardCharsets.UTF_8);
                
                // Dividir por sentencias (separadas por ;)
                String[] statements = sql.split(";");
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        jdbcTemplate.execute(trimmed);
                    }
                }
                log.info(">>>> [SDD] Sembrado de data.sql finalizado con ÃƒÂ©xito.");
            } else {
                log.info(">>>> [SDD] Archivo data.sql no encontrado, se omite el sembrado.");
            }
        } catch (Exception e) {
            log.error(">>>> [SDD] Error ejecutando data.sql: {}", e.getMessage());
        }
    }
}


