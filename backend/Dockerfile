FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Instalar dependencias necesarias (bash para mvnw)
RUN apk add --no-cache bash

# Copiar configuración de Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Copiar código fuente
COPY src src

# Construir el JAR directamente. 
# Se elimina dependency:go-offline para asegurar resolución de repositorios Spring AI.
RUN ./mvnw clean package -DskipTests

# Localización dinámica del JAR generado
RUN cp target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]