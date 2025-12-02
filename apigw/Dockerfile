FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/apigw-1.0-SNAPSHOT.jar apigw.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "apigw.jar"]

# docker build -t aurora/customer:latest ./customer
# docker run -e SPRING_PROFILES_ACTIVE=prod -p 8080:8080 aurora/customer:latest