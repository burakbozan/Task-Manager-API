# ---- Build Stage ----
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for faster builds)
RUN mvn -B -DskipTests package

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Add curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy built JAR from build stage
COPY --from=build --chown=spring:spring /workspace/target/taskmanager-*.jar app.jar

USER spring
EXPOSE 8080

# Healthcheck for Kubernetes/OpenShift
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
