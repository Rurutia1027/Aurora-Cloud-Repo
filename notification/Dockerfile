FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/notification-1.0-SNAPSHOT.jar notification.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "notification.jar"]

# docker build -t aurora/notification:latest ./notification
