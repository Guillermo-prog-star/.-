package com.integrityfamily.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * SDD CONTRACT: Configuración oficial de OpenAPI 3.0 / Swagger UI para Integrity Family.
 * Establece el contrato de API REST y habilita el esquema de seguridad Bearer (JWT).
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "API REST de Integrity Family",
        version = "1.0.0",
        description = "Contrato oficial de servicios y microservicios para la plataforma de transformación y mentoría familiar asistida por IA.",
        contact = @Contact(
            name = "Equipo de Desarrollo Integrity Family",
            email = "soporte@integrityfamily.com",
            url = "https://integrityfamily.com"
        ),
        license = @License(
            name = "Licencia Privada Comercial",
            url = "https://integrityfamily.com/licencia"
        )
    ),
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Introduce tu token JWT Bearer para acceder a los endpoints protegidos."
)
public class OpenApiConfig {
}
