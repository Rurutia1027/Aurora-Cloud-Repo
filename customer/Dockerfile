FROM eclipse-temurin:17-jdk
WORKDIR /app

# OpenTelemetry Java Agent (set OTEL_AGENT_URL at build time to pin a version)
ARG OTEL_AGENT_URL=https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
RUN mkdir -p /otel && curl -sSL -o /otel/opentelemetry-javaagent.jar "${OTEL_AGENT_URL}"

COPY target/customer-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Agent path is in image; set OTEL_SERVICE_NAME, OTEL_EXPORTER_OTLP_ENDPOINT at runtime
ENTRYPOINT ["java", "-javaagent:/otel/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
