FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copiar archivos Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Descargar dependencias
RUN ./mvnw dependency:go-offline

# Copiar código fuente
COPY src src

# Construir el JAR y verificar resultados
RUN ./mvnw clean package -DskipTests
RUN ls -la target/ || echo "Target directory not found"
RUN find target/ -name "*.jar" -type f || echo "No JAR files found"

# Copiar cualquier JAR generado si existe
RUN if [ -f target/*.jar ]; then cp target/*.jar app.jar; else echo "No JAR found to copy"; fi

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]