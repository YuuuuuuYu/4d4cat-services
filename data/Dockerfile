# Dockerfile for data-server

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/data.jar app.jar

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
