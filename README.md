# Aurora Cloud Platform  | [![CI Pipeline](https://github.com/Rurutia1027/aurora-cloud-platform/actions/workflows/ci.yaml/badge.svg)](https://github.com/Rurutia1027/aurora-cloud-platform/actions/workflows/ci.yaml)
A Cloud-Native Microservices Applicatin (Spring Cloud + Spring Boot + Docker + K8s + Observability).

Aurora Cloud Platform is a modular microservices system built with Spring Boot, designed to demostrate cloud-native application patterns including service discovery, messaging, observability, containerization, and Kubernetes deployment. 

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
- **Structured JSON Logging**
- **Distributed Tracing (OpenTelementry)**
- **Metrics (Micrometer)**
- **Log/Trace correlation fields (traceId, spanId)**

### Contarinerization 
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
