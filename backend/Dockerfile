# Stage 1: build JAR with Maven
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

RUN apk add --no-cache maven

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: run with Playwright Java image (Chromium + deps preinstalled)
# Image version must match Playwright dependency version; see https://playwright.dev/java/docs/docker
FROM mcr.microsoft.com/playwright/java:v1.50.0-noble
WORKDIR /app

COPY --from=builder /build/target/api-automation-platform-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 4000

ENTRYPOINT ["java", "-Xms256m", "-Xmx1536m", "-XX:+UseG1GC", "-jar", "app.jar"]
