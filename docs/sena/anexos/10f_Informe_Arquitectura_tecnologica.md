# Informe de arquitectura tecnológica

Versiones tomadas de los archivos de construcción el 16/11/2026. Cuando un documento interno contradice al archivo de construcción, manda el archivo de construcción.

# Lenguajes y plataformas

| Pieza | Versión que fija el archivo | Archivo |
| --- | --- | --- |
| Java de compilación | 17 | `backend/pom.xml` (`java.version`) |
| Java de la imagen | 21 (Temurin) | `backend/Dockerfile` |
| Spring Boot | 3.4.3 | `backend/pom.xml` |
| Angular | 17.3 | `if-frontend/package.json` |
| TypeScript | 5.4 | `if-frontend/package.json` |
| MySQL | 8.4 | `docker-compose.yml` |
| RabbitMQ | 3 (imagen `rabbitmq:3-management`) | `docker-compose.yml` |
| JWT | jjwt 0.12.6 | `backend/pom.xml` |
| OpenAPI | springdoc 2.8.5 | `backend/pom.xml` |
| FHIR | HAPI structures R4 8.10.0 | `backend/pom.xml` |
| PDF | iText 8.0.2 | `backend/pom.xml` |
| Excel | Apache POI 5.2.5 | `backend/pom.xml` |

El README dice Java 21. `docs/architecture.md` dice Angular 18. Esas dos frases no coinciden con `pom.xml` y `package.json`.

La arquitectura de capas, los patrones y el despliegue están desarrollados en el PT-MTC-01. El modelo de datos está en el PT-MCBD-01.

[Pendiente: diagrama impreso firmado por el instructor, si el centro lo exige además del Mermaid de los manuales.]
