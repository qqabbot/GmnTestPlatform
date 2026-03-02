# Build stage
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy pom and download dependencies (cached unless pom changes)
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN adduser -D -u 1000 appuser

# Copy jar from builder (artifact name from pom)
COPY --from=builder /app/target/api-automation-platform-*.jar app.jar

# Directories for uploads and reports
RUN mkdir -p /app/data /app/videos /app/screenshots /app/allure-results && \
    chown -R appuser:appuser /app

USER appuser

EXPOSE 4000

# Use docker profile so application-docker.yml is loaded
ENV SPRING_PROFILES_ACTIVE=docker
ENTRYPOINT ["java", "-Xms256m", "-Xmx768m", "-XX:+UseG1GC", "-jar", "app.jar"]
