FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY target/fraud-1.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
