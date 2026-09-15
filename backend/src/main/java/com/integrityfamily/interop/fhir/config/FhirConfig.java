package com.integrityfamily.interop.fhir.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code FhirContext.forR4()} es costoso de crear (escanea el modelo R4
 * completo) — debe existir un único bean singleton, nunca instanciarse por
 * request.
 */
@Configuration
public class FhirConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
