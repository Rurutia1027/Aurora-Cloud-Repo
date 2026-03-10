# Observability Infra Design (via OpenTelemetry)

## Overview

**Purpose**
Define an end-to-end observability architecture for our microservices based on **OpenTelemetry(OTel)**, covering
Tracing, Logging, Metrics, and APM without depending on any single commercial product.

**Goals**

- Each request has an end-to-end trace across all services.
- Logs, traces, and metrics are cross-linked via standard OTel identifiers.
- Observability is **vendor-neutral**, pluggable to any OTel-compatible APM/visualization backend (
  Jaeger/Tempo/Grafana/custom stack, etc.).
- Context propagation is **standard** using OTel **Trace Context** and **Baggage**.

## Architecture Overview

### Signals

- Traces: end-to-end request flows (Trace ID + Span ID)
- Logs: structured JSON logs, enriched with trace/span and business context.
- Metrics: system/endpoint/business KPIs.

### High-Levels Components

#### Application layer (per microservices)

- OTel SDK / Auto-Instrumentation (languages-specific)
- OTel Log Appender / bridge (integrates OTel context into logs)
- Metrics via OTel Metrics API or Micrometer-to-OTel bridge

#### Observability Infra

- OpenTelemetry Collector (central pipeline for all signals)
- Tracing backend: Jaeger, Tempo, or similar (OTLP compatible)
- Metrics backend: Prometheus (+ Grafana dashbaords)
- Log pipeline
    - Application -> OTel Logs via OLTP, or
    - Application -> stdout / JSON -> Log agent -> Kafka -> Loki / ES
- APM views: built from traces + metrics + logs using open-source tools (Jaeger / Tempo + Prometheus + Grafana)

## Identity & Correlation Model

### Core Identifiers

Trace ID (OTel standard)

- 16-byte / 32-hex ID
- Represents a single request / transaction across all services
- Used as the primary correlation key for observability

Span ID

- 8-byte / 16-hex ID
- Represents a single unit of work within a trace (e.g., an HTTP call, DB query)
- Used to zoom into specific operations.

(Based on [OTel Trace ID vs Span ID](https://signoz.io/comparisons/opentelemetry-trace-id-vs-span-id/))

### Business Correlation Attributes

Stored as OTel attributes + baggage:

- `user.id`
- `tenant.id`
- `order.id / payment.id` other domain IDs

These appear:

- On spans (span attributes)
- In logs (JSON fields)
- In metrics (only low-cardinality labels e.g, `tenant.id`)

### Context Propagation Design

#### Cross-Service (HTTP/gRPC)

Use W3C Trace Context headers:

- `traceparent` (mandatory): carries `trace_id`, `span_id` and flags
- `tracestate` (optional): carries `trace_id`,`span_id`, and flags

Use **OTel Baggage** for business attributes:

- HTTP `baggage` header (key=value pairs)
    - Example: `baggage: user.id=123,tenant.id=acme`

- Implementation:
    - **Ingress**: OTel SDK extracts context from incoming headers and sets the current span/context.
    - **Egress**: OTel SDK injects current context into outgoing requests automatically

#### Message-Drive (Kafka, MQ)

- Use OTel TextMapPropagator to embedded trace context in:
    - Message header, or
    - A dedicated metadata field in message body
- Customer service:
    - Extracts context and starts a new span as a child of the producing span, preserving Trace ID.

### In-Process & Multi-Thread

- Use OTel Context API as the primary source of truth
- Traditional MDC/ThreadLocal:
    - Derived from OTel Context (for logging only).
    - Always set and cleared in filter/interceptor layers.
- Async / thread pools:
    - Prefer OTel-provided context propagation (e.g., auto integration with Reactor, executors)
    - Where necessary, wrap runnables / callables to copy OTel Context into worker threads

---

## Tracing Design

### Gateway as Root Trace Entry

Gateway is the canonical trace entry point:

- Auto-instrument with OTel
- For each incoming external request:
    - If `tracepaent` exists: continue upstream trace
    - Else: start a **new trace** with a new Trace ID.

Gateway spans:

- Act as root spans
- Attach attributes:
    - `http.method`, `http.route`, `http.status_code`
    - `user.id`, `tenant.id`, `client.ip`, etc.

## Downstream Services

All services:

- Enable OTel SDK /agent for
    - HTTP server & client
    - DB, cache, messaging clients
- Each incoming call:
    - Extracts trace context
    - Starts a service-level span (`service_name.endpoint`)
- Internal operations:
    - Optional manual spans for key business steps (validation, external integration, critical algorithms).
- Span attributes follow OTel semantic conventions (HTTP, DB, messaging)

## Logging Design (with OTel Log Appender)

### Application Logging

Logging is structured JSON, not plain text.
Each log record includes:

- `timestamp`
- `severity`
- `body`(message)
- `trace_id`, `span_id`
- `service.name`
- `deployment.environment`
- Business context: `user.id`, `tenant.id`, `order.id` ...

Implementation options:

- Use OTel Log Appender / log bridge
    - Automatically reads OTel Context:
        - Injects `trace_id`, `span_id`, `service.name`
- MDC can still be used for compatibility but filled from OTel Context

### Log Pipeline

Two main patterns (can be combined):

#### Option A: OTLP Logs via Collector

App -> OTLP Log Exporter -> OTel Collector

- App -> OTLP Log Exporter -> OTel Collector
- Collector:
    - Normalize fields (add/remove/rename attributes)
    - Forwards logs to:
        - Loki
        - Elasticsearch
        - Object storage, etc.

#### Option B: stdout/File -> Agent -> Kafka -> Log Store

- App writes JSON logs to stdout/file
- Promtail/Fluent Biit/Filebeat tails logs and sends to Kafka
- From Kafka:
    - Stream processor or sinks write to Loki/Elasticsearch
- Logs already contain `trace_id` / `span_id`, so log backends can correlate with traces.

### Metrics & APM

#### Metrics Collection

Use OTel Metrics API or Micrometer -> OTel bridge:

- System metrics (CPU, memory, GC, JVM)
- HTTP metrics (request count, latency, error rate)
- Business metrics (e.g., payments per minute / failed orders)

Metrics -> OTLP -> Collector -> Prometheus / long-term store

#### Labelling & Cardinality

Common labels:

- `service.name`
- `http.route`
- `status_code_family` (2xx/4xx/5xx)
- `tenant.id` (if label cardinality is manageable)

DO NOT use `trace_id` or `span_id` as standard labels (high cardinality).
For linking metrics to traces:

- Use exemplars where supported:
    - Metrics samples store a reference to a representative `tracd_id`
    - Grafana or other frontends can link a metric point to a specific trace.

### APM Views (Tool-Agnostic)

Without relying on any specific commercial product:

Build APM-like views by combining:

- Traces (Jaeger / Tempo) for
    - Service map / call graph
    - Per-service latency / error distribution
- Metrics (Prometheus + Grafana) for:
    - **RED** metrics (Rate, Errors, Duration)
    - Resource usage
- Logs (Loki/Elasticsearch + Grafana/Kibana) for:
    - Detailed error context filter by `trace_id`

### End-to-End Troubleshooting Flow

Metrics alert fires (e.g., error rate spike for a REST API)
In Grafana:

- Inspect the affected metrics
- Use exemplars or labels to identity a specific `trace_id`

In tracing UI (Jaeger / Tempo):

- Open the trace by `trace_id`
- See entire call chain and spans with attributes (including business IDs).

From the trace:

- Copy `trace_id` and search logs in Loki/Elasticsearch:
    - Query by `tracd_id=<value>`
- Inspect all logs (across gateway, services, background tasks) for that request.

Combine:

- Metrics show "when/how often"
- Traces show "where in the call chain"
- Logs show "what exactly happened and with which data."

Identify root cause and plan remediation.

## Migration Strategy from Current Design

### Phase-1 - Tracing & Context

- Introduce OTel SDK / agent on gateway + 1-2 core services.
- Enable HTTP server/client auto-instrumentation
- Verify Trace ID propagation end-to-end.

### Phase-2 - Logging

- Replace / augment current logging with OTel log appender
- Make sure each log record carries `trace_id`, `span_id`, `service_name`.
- Integrate with existing Kafka + Loki/ELK pipeline

### Phase-3 - Metrics

- Route metrics via OTel - Collector -> Prometheus
- Standardize metric names and labels; create basic RED dashboards.

### Phase-4 - APM Style Dashboards

- Connect:
    - Prometheus + Grafana for metrics
    - Jaeger/Tempo for traces
    - Loki/ELK + Grafana for logs
- Build:
    - Service map, latency / error views
    - Drill-down from metrics -> trace -> logs

## Summary

This design:

- Use OpenTelemetry as the unified data model and propagation mechanism.
- Aligns fully with OTel's notions of Trace ID / Span ID, Trace Context, and Baggage.
- Builds an open, vendor-neutral observability stack:
    - App (OTel) -> Collector -> (Jaeger/Tempo, Prometheus, Loki/ELK, Grafana)
- Ensures any single request can be followed consistently across **trace**, **logs**, and **metrics** using the same
  Trace ID and business context.
