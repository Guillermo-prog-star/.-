FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN groupadd -r appgroup \
    && useradd -r -g appgroup appuser \
    && mkdir -p /app/logs \
    && chown -R appuser:appgroup /app

COPY --from=builder /app/target/*.jar app.jar

RUN chown appuser:appgroup /app/app.jar

ENV SPRING_PROFILES_ACTIVE=prod
ENV LOGGING_FILE_NAME=/app/logs/backend-prod.log

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]