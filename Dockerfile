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
# JAVA_OPTS can be passed via environment variable for JVM tuning
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

