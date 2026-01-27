# Dockerfile

# Stage 1: Create a minimal runtime image
FROM eclipse-temurin:21-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the executable jar built from the CI workflow
COPY app.jar app.jar

# Expose the port the application runs on
EXPOSE 8080

# Set the entrypoint to run the application
# Java options can be added here for performance tuning
ENTRYPOINT ["java", "-jar", "app.jar"]

