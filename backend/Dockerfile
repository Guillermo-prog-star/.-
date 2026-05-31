# ==========================================
# ETAPA 1: Construcción (Maven)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Optimización de caché de dependencias (Pom Layer)
COPY pom.xml .
RUN mvn package -Dmaven.test.skip=true -Dmaven.main.skip || true

# Copia de código fuente y empaquetado del artefacto
COPY src ./src
RUN mvn package -Dmaven.test.skip=true

# ==========================================
# ETAPA 2: Ejecución (JRE)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Corrección del Warning LegacyKeyValueFormat usando sintaxis KEY=VALUE
ENV LANG=C.UTF-8

# Instalación de utilidades esenciales del sistema
RUN apk add --no-cache curl bash

# Transferencia del artefacto desde la etapa de compilación
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Parámetros de ejecución de la JVM optimizados
ENTRYPOINT ["java", "-Xmx512m", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-jar", "app.jar"]