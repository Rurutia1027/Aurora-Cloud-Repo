# Observability Design for Microservices (Logging, Metrics, Tracing)

---

## 1. Scope & Goals

### Goal

Provide a unified, environment-agnostic observability design for the Spring Boot microservices system (`customer`,
`fraud`, `notification`, `apigw`, `amqp`, clients) such that:

In both **docker-compose mode** and **Kubernetes mode**:

* Traces are visible in Jaeger
* Metrics are exposed via Micrometer → Prometheus
* Logs are centralized and correlated with traces and metrics (via `traceId` / `spanId`)

### Non-Goals

* No database sharding or infrastructure-heavy changes
* No service-mesh dependency (Istio / K8s Gateway optional, not required)
* No strong vendor lock-in (use standards: OTLP, Prometheus, structured JSON logs)

---

## 2. High-Level Observability Architecture

### Per Service (Spring Boot)

Each service implements:

* **Logging**
  Logback → structured JSON logs to stdout
  Includes `traceId` / `spanId`

* **Tracing**
  OpenTelemetry (or Sleuth bridge) → OTLP / Zipkin exporter → Jaeger

* **Metrics**
  Micrometer → Prometheus registry → `/actuator/prometheus`

---

### Infrastructure Layer

#### docker-compose

* Jaeger (all-in-one) for tracing
* Prometheus for metrics scraping
* Logs:

    * stdout logs collected by Promtail / Fluent Bit
    * Forwarded to Loki / ELK

#### Kubernetes

* Jaeger production deployment
* Prometheus Operator or kube-prometheus-stack
* Cluster logging stack (Fluent Bit → Loki/ELK)

---

## 3. Tracing Design (Jaeger)

### 3.1 Spring Boot Tracing Stack

#### Option A (Recommended, Future-Proof) – OpenTelemetry Java Agent

Attach the OpenTelemetry Java Agent to each JVM:

**docker-compose**

```bash
JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar
```

**Kubernetes**

Mount agent and configure via container args or env.

Environment variables:

```bash
OTEL_SERVICE_NAME=<service-name>
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger-collector:4317
OTEL_TRACES_SAMPLER=parentbased_always_on
```

Auto-instrumentation covers:

* Spring MVC / WebFlux
* JDBC / PostgreSQL
* Spring AMQP (where supported)

**Benefit:** minimal code changes, consistent behavior across Spring Boot versions.

---

#### Option B – Spring Cloud Sleuth → Jaeger (Zipkin API)

For Boot 2.7 compatibility:

```properties
spring.zipkin.base-url=http://jaeger-collector:9411
```

This leverages Jaeger’s Zipkin endpoint.
Less future-proof than OpenTelemetry.

**Target design assumes Option A.**

---

### 3.2 Span Model

#### Gateway (`apigw`)

* Root span: `gateway.route.<service>`

#### Backend Services

* Controller span: `http.<resource>.request`
* Service span: `service.<domain>.<operation>`
* Repository span: `repository.<entity>.<operation>`
* JDBC spans: auto-instrumented

#### AMQP Flows

* Producer span: `amqp.publish.<event>`
* Consumer span: `amqp.consume.<event>`

    * Child spans: service → repository → JDBC

---

### 3.3 Context Propagation

Use **W3C TraceContext** (`traceparent`, `tracestate`):

* HTTP: propagated via headers
* Spring Cloud Gateway → backend services
* AMQP: via message headers (auto or interceptor)

---

### 3.4 Jaeger Deployment

#### docker-compose

* Jaeger all-in-one
* OTLP gRPC: 4317
* Zipkin endpoint: 9411
* UI: 16686

Services use:

```bash
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
```

---

#### Kubernetes

* Deploy via Jaeger Operator or Helm
* Collector endpoint:

  ```
  jaeger-collector.observability.svc:4317
  ```
* Expose UI via Ingress or LoadBalancer

---

## 4. Metrics Design (Micrometer + Prometheus)

### 4.1 Micrometer Setup

Dependencies:

* `micrometer-core`
* `micrometer-registry-prometheus`
* `spring-boot-starter-actuator`

Expose:

```properties
management.endpoints.web.exposure.include=prometheus,health,info
```

Endpoint:

```
/actuator/prometheus
```

---

### 4.2 Standard Metrics

Automatically exposed:

* `http_server_requests_seconds_*`
* JVM memory / GC / threads
* Hikari connection pool
* Process/system metrics

---

### 4.3 Custom Metrics

#### Naming Convention

**Request Counter**

```
microservice_request_total{service,layer,status}
```

**Latency Histogram**

```
microservice_request_duration_seconds{service,layer}
```

**Error Counter**

```
microservice_request_error_total{service,layer}
```

**AMQP Metrics**

```
microservice_amqp_messages_total{service,role,status}
```

---

### Allowed Labels

* `service`: apigw, customer, fraud, notification
* `layer`: gateway, controller, service, repository, messaging
* `status`: success, error
* `role`: producer, consumer

Avoid:

* SQL strings
* User IDs
* Request parameters
* High-cardinality labels

---

### 4.4 Prometheus Integration

#### docker-compose

Prometheus scrapes:

```
http://<service>:<port>/actuator/prometheus
```

Static job per service or grouped target list.

---

#### Kubernetes

Use Prometheus Operator.

Annotate services:

```yaml
prometheus.io/scrape: "true"
prometheus.io/path: "/actuator/prometheus"
prometheus.io/port: "8080"
```

Or define `ServiceMonitor` resources.

---

## 5. Logging Design (Structured, Correlated)

### 5.1 Log Format

Use Logback + JSON encoder (e.g., logstash-logback-encoder).

Standard fields:

* timestamp
* level
* logger
* thread
* message
* service
* traceId
* spanId
* environment

Trace identifiers populated via MDC from OpenTelemetry/Sleuth.

All logs go to **stdout**.

---

### 5.2 Log Shipping

#### docker-compose

* Promtail / Fluent Bit container
* Reads Docker stdout logs
* Pushes to Loki or Elasticsearch

Queryable by:

* service
* traceId
* level

---

#### Kubernetes

* Fluent Bit / Fluentd DaemonSet
* Tails `/var/log/containers`
* Forwards to Loki/ELK

Correlation via `traceId`.

---

## 6. Environment-Specific Design

### 6.1 docker-compose Mode

#### Services

* apigw
* customer
* fraud
* notification
* amqp
* DBs
* RabbitMQ

#### Observability Stack

* jaeger (all-in-one)
* prometheus
* loki (optional but recommended)
* promtail / fluent-bit

#### Configuration

**Tracing**

```bash
OTEL_SERVICE_NAME=<service>
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
```

**Metrics**

Prometheus static targets:

* apigw:8083
* customer:8080
* fraud:8081
* notification:8082

**Logging**

All services log JSON → stdout → promtail → loki.

---

### 6.2 Kubernetes Mode

#### Namespaces

* `microservices`
* `observability`

#### Deployments

* App Deployments + Services
* Jaeger Operator
* Prometheus Operator
* Logging stack

#### Tracing Config

```bash
OTEL_SERVICE_NAME=<service>
OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger-collector.observability.svc:4317
```

#### Metrics

Use `ServiceMonitor` or annotations.

#### Logging

Fluent Bit DaemonSet → Loki/ELK.

---

## 7. Configuration Strategy

### application.yml (common)

* Enable actuator
* Enable Micrometer
* JSON logging
* No environment-specific endpoints

---

### application-docker.yml

* Jaeger endpoint: `jaeger`
* Local OTLP configuration

---

### application-kube.yml

* Jaeger collector:

  ```
  jaeger-collector.observability.svc
  ```

Environment determines:

* How OTEL agent is attached
* Prometheus deployment
* Logging backend

---

## 8. Rollout Plan

### Phase 1 – Baseline

* Add Micrometer Prometheus registry
* Enable `/actuator/prometheus`
* Enable JSON logging with traceId/spanId

---

### Phase 2 – Tracing

* Attach OpenTelemetry Java Agent (docker-compose first)
* Verify traces in Jaeger
* Validate service names and span hierarchy

---

### Phase 3 – docker-compose Observability

* Add Jaeger, Prometheus, logging stack
* Validate:

    * Traces in Jaeger
    * Metrics in Prometheus
    * Logs correlated by traceId

---

### Phase 4 – Kubernetes Observability

* Deploy Jaeger & Prometheus in `observability`
* Configure Deployments with OTEL
* Configure ServiceMonitor
* Validate end-to-end observability

---

### Phase 5 – Hardening

* Tune sampling strategies
* Define retention policies
* Rate-limit logs/metrics if needed
* Add Prometheus alerts

---

## Final Outcome

After completion:

* Distributed traces visible in Jaeger
* Metrics available in Prometheus
* Logs correlated across services via traceId
* Works consistently in docker-compose and Kubernetes
* No sharding, no service mesh dependency, no vendor lock-in

This establishes a production-ready, standards-based observability foundation for the microservices system.
