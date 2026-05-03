# =========================
# ESTRATEGIA: Build Local para Bypass de Red lenta
# =========================
# Este Dockerfile asume que ya ejecutaste ./mvnw package localmente.
# Esto evita que Docker intente descargar dependencias en una red lenta.

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl bash

WORKDIR /app

# Copia el JAR generado localmente en la carpeta target
COPY target/*.jar app.jar

EXPOSE 8080

# Entrypoint con soporte para espera de DB si fuera necesario
ENTRYPOINT ["java", "-jar", "app.jar"]