package com.integrityfamily;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

/**
 * Punto de entrada de Integrity Family.
 *
 * @EntityScan restringe el escaneo de entidades JPA ÚNICAMENTE a los paquetes
 * canónicos del dominio, eliminando DuplicateMappingException causada por
 * entidades legacy en paquetes modulares.
 *
 * La inicialización de datos (usuarios, roles) se delega a:
 *   - MasterDataInitializer (common/initializer) → usuarios william y admin demo
 * No se duplica lógica aquí para evitar race conditions al arrancar.
 */
@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {
        "com.integrityfamily.domain",
        "com.integrityfamily.lts.domain",
        "com.integrityfamily.risk.domain",
        "com.integrityfamily.plan.domain",
        "com.integrityfamily.assessment.domain",
        "com.integrityfamily.analytics.domain",
        "com.integrityfamily.common.domain",
        "com.integrityfamily.report.domain",
        "com.integrityfamily.adaptive",
        "com.integrityfamily.scanner.domain",
        "com.integrityfamily.myspace.domain"
})
public class IntegrityFamilyApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrityFamilyApplication.class, args);
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            System.out.println("====== AUTO-REPARANDO FLYWAY ANTES DE MIGRAR ======");
            flyway.repair();
            System.out.println("====== AUTO-REPARACION DE FLYWAY COMPLETADA ======");
            flyway.migrate();
        };
    }
}
