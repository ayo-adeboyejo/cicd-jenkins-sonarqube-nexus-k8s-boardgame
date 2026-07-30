# ===== Build Stage =====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first to leverage Docker layer caching
COPY pom.xml .

# Pre-download dependencies (faster rebuilds)
RUN mvn -B -q -DskipTests dependency:go-offline

# Copy project source
COPY src ./src

# Build the JAR (skip tests for faster build)
RUN mvn clean package -DskipTests

# ===== Runtime Stage =====
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root user and group — fixes SonarQube security warning
# Running as root in a container is a security risk
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Transfer ownership to non-root user
RUN chown appuser:appgroup app.jar

# Switch to non-root user — container will no longer run as root
USER appuser

# Expose app port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
