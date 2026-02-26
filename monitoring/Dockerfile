# Dockerfile for monitoring service

# Stage 1: Build the application (using a multi-stage build)
FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

# Copy the Gradle build files
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .
COPY monitoring monitoring

# Grant execution rights to the Gradle wrapper
RUN chmod +x gradlew

# Build the monitoring service
RUN ./gradlew :monitoring:bootJar --no-daemon

# Stage 2: Create a minimal runtime image
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /app/monitoring/build/libs/monitoring.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Set the entrypoint to run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
