FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/fraud-1.0-SNAPSHOT.jar fraud.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "fraud.jar"]

# docker build -t aurora-cloud-platform/fraud:latest ./fraud
# docker run -p 8081:8081 aurora-cloud-platform/fraud:latest