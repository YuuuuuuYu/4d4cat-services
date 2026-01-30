# Dockerfile for api-server

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/api.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
