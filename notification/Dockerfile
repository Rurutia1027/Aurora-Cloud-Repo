FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/notification-1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# docker build -t aurora/notification:latest ./notification
