# ETAPA 1: Construcción (Maven)
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# Descargar dependencias primero para aprovechar cache
RUN mvn package -DskipTests -Dmaven.main.skip || true
COPY src ./src
RUN mvn package -DskipTests

# ETAPA 2: Ejecución (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV LANG C.UTF-8
RUN apk add --no-cache curl bash
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-jar", "app.jar"]