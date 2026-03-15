# Aurora Cloud Platform  | [![CI Pipeline](https://github.com/Rurutia1027/aurora-cloud-platform/actions/workflows/ci.yaml/badge.svg)](https://github.com/Rurutia1027/aurora-cloud-platform/actions/workflows/ci.yaml)

A Cloud-Native Microservices Application (Spring Cloud + Spring Boot + Docker + K8s + Observability).

Aurora Cloud Platform is a modular microservices system built with Spring Boot, designed to demonstrate cloud-native
patterns including service discovery, messaging, observability, and Kubernetes deployment.

This repository contains the **application source code**, **Dockerfiles**, **base Kubernetes metrics**, and **local development tooling**.

Environment-specific deployment manifests are stored separately in the GitOps repository: 
`https://github.com/Rurutia1027/aurora-cloud-gitops`

---

## 1. Architecture Overview 
The application is composed of several Spring Boot microservices that communicate through REST and AMQP.

### Core Components 
| Component | Description |
|----------|-------------|
| `eureka-server` | Service discovery registry for microservices (Spring Cloud Netflix) |
| `apigw` | API Gateway for routing external traffic to backend services |
| `customer` | Customer management microservice |
| `fraud` | Fraud detection microservice |
| `notification` | Notification delivery microservice |
| `amqp` | AMQP module providing RabbitMQ configuration (exchanges, queues, bindings) |
| `clients` | Shared Feign clients and common DTOs |

### Observability Stack (Integrated)

- **Structured JSON Logging** (Logback + OpenTelemetry Logback Appender -> OTLP -> Collector -> Loki)
- **Distributed Tracing (OpenTelemetry)** (Java Agent, tracecontext + baggage, Collector -> Jaeger)
- **Metrics (Micrometer/ Prometheus)**
- **Log/Trace correlation** (trace_id, span_id in logs; Grafana Loki + Jaeger)

---

## Observability Vision & Architecture

### Current build (local)

| Dimension                | What’s in place                                                                                                                                                    |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Full chain (tracing)** | HTTP/gRPC: OTel Agent + Propagator; RabbitMQ: inject/extract in message headers (customer → notification same trace). Collector → Jaeger.                          |
| **Logs**                 | Logback → OTel Appender → OTLP → Collector → Loki; Grafana Loki datasource + provisioned dashboard “Aurora Application Logs”; query by `service_name`, `trace_id`. |
| **Tracing**              | Jaeger UI; consistent sampling (`parentbased_always_on`); MQ context propagation so producer and consumer share one trace.                                         |

### Observability architecture (ASCII)

### Vision: complex scenarios (local + cloud-native)

Observability is built to support **full-chain tracing**, **centralized logs**, and **trace-log correlation** in both *
*local** and **cloud-native** environments. The roadmap aligns with these challenges areas:

**Five Core Challenges (full chain, logs, tracing):**

- Cross-service / cross-boundary propagation
Same trace_id end-to-end; logs carry trace_id; every hop has a span.
> **Local** HTTP/gRPC + RabbitMQ.
> **Cloud-native** Multi-cluster trace_id and sampling consistency; backend aggregation. 

- Async / message queues
Producer and consumer on the same trace; inject/extract in message headers. 
> **Local** RabbitMQ
> **Cloud-native** Sam epattern for Kafka and other MQs; trace-parent/baggage in event envelope. 

- Thread pool / async executor
Child threads keep parent context; logs and span stay on the same trace. 
> **Local**: Rely on OTel instrumented Executor.
> **Cloud-native**: Explicit context propagation or broader Agent coverage; Serverless uses trigger metadata only. 

- Outbound instrumentation
DB, HTTP, gRPC, cache all create child spans and propagate context. 
> **Local**: Agent auto-instrumentation covers most. 
> **Cloud-native**: Fill gaps; align Mesh and app spans.
 
- Sampling consistency
Whole chain sampled or not; no "half-chain". 
> **Local**: parentbased_always_on. 
> **Cloud-native**: Unified sampling and quotas across clusters/tenants. 

**Cloud-native extensions**

- Service Mesh (Istio/Envoy) - Same trace_id across app and sidecar; headers propagated ; app + mesh spans in one trace. 
- Multi-cluster / multi-region / multi-tenant - Unified trace_id; logs and traces aggregated; retention and cardinality control.
- Serverless / FasS - Context from trigger only (HTTP/event metadata); root span and logs per invocation; traceparent/baggage in event headers. 
- Batch / scheduled jobs - Root span at job start; trace_id/saga_id in Baggage for downstream; job + downstream in one trace
- Event-driven / Saga - trace_id + saga_id (Baggage) across DAG; inject / extract at every async boundary; full saga visible in trace and logs. 
- Backend scale - Sampling, retention, indexing and long-trace handling for high cardinality. 

### Containerization
Every service includes:
- A production-ready Dockerfile 
- Container via `docker build` or CI pipeline 
- Base YAML manifests under `/k8s`

## 2. Repository Structure 
```
├── README.md
├── docker-compose.yml # Local RabbitMQ + dependencies
├── diagrams.drawio # System architecture diagrams
├── amqp/
├── apigw/
├── clients/
├── customer/
├── eureka-server/ -> consul 
├── fraud/
├── notification/
├── k8s/
│ └── kind/ # Developer local manifests
└── pom.xml # Parent Maven project
```

## 3. Running Locally 
### 3.1 Start Dependencies (RabbitMQ)
```bash 
docker-compose up -d 
```

### 3.2 Start Eureka Server 
```bash 
cd eureka-server
./mvnw spring-boot:run
```

### 3.3 Start Individual Services 
For example, to start the consumer service: 
```bash 
cd consumer 
./mvnw spring-boot:run 
```

## 4. Build Docker Images 
Each microservice containes its own Dockerfile 
### 4.1 Build all services 
```bash 
mvn clean pakcage -DskipTests 
```

### 4.2 Bild a specific service 
```bash 
docker build -t aurora/customer:latest ./customer
```

## 5. Kubernetes Deployment Base Manifests 
This repository contains based K8s manifests only: 
```
k8s/
└── kind/
```
Environment-specific overlays are maintained in the GitOps repository.
[aurora-cloud-gitops](https://github.com/Rurutia1027/aurora-cloud-gitops)

## 6. GitOps Integration 
The CI pipeline (GitHub Actions) will:
- Build and push Docker images
- Update image tags in `aurora-cloud-gitips`
- Argo CD will detect changes and deploy automatically

## 7. License
[LICENSE](./LICENSE)
