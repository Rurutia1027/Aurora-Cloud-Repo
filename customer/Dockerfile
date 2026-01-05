FROM eclipse-temurin:17-jdk
WORKDIR app
COPY target/customer-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# docker build -t aurora/customer:latest ./customer
# docker run -p 8080:8080 aurora-cloud-platform/customer:latest