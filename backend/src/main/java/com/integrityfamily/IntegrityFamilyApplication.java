package com.integrityfamily;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de Integrity Family.
 * La inicialización de datos (usuarios, roles) se delega a:
 *   - NodoArmeniaMasterInitializer (auth/service) → usuarios william y admin demo
 * No se duplica lógica aquí para evitar race conditions al arrancar.
 */
@SpringBootApplication
@EnableScheduling
public class IntegrityFamilyApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrityFamilyApplication.class, args);
    }
}