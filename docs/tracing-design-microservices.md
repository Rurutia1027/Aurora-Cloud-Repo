# Tracing Design for Microservices (Multi-App, Separate Deployment)

This document records the typical design approach for adding distributed tracing to a system of **multiple microservices
apps**, each with its own Dockerfile and deployment (e.g., docker-compose or Kubernetes). It is intended as a reference
for where to instrument, why the gateway is often a good entry point, and why that alone is usually not enough.

---

## 1. Context

- **Setup**: Several independent applications (e.g., API gateway, customer, fraud, notification), each built and run as
  its own container/service.
- **Goal**: One logical user request may cross the gateway and several backends (and possible queues and databases). We
  want a debug, measure latency, and understand failure propagation.

---

## 2. Why the Gateway Is a Natural Entry Point for Tracing

The **API gateway** (or edge proxy) is usually the **single entry point** for external traffic. That makes it a strong
candidate for tracing:

| Aspect                        | Why it helps                                                                                                                                                                                                                                 |
|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Root span**                 | The first span for an incoming request is created at the gateway. That span becomes the root of the trace; all downstream calls are children.                                                                                                |
| **Context origination**       | The gateway sees the request first. It can generate or read `traceparent`/`tracestate` (W3C TraceContext), and optionally set business headers (`X-User-Id`, `X-Request-Id`, `X-Tenant-Id`) from JWT or first-hop headers before forwarding. |
| **Single place to configure** | One component (gateway) can be responsible for starting the trace and propagating context, so you don’t rely on each client to do it.                                                                                                        |
| **Consistent behaviour**      | All traffic that goes through the gateway gets the same tracing and header behaviour.                                                                                                                                                        |

So **yes: the gateway is often the best place to start the trace and to originate (or forward) context.** That
conclusion is sound for the common case where "all user traffic enters via the gateway."

---

## 3. Why the Gateway Alone Is Not Enough

Tracing is **distributed**: each process creates its **own spans**. So

- The **gateway** creates the root span and can set attributes on it.
- Each **backend** (customer, fraud, notification, etc.) is a separate process; when it handles a request, it creates
- **new spans** (children of the same trace if context is propagated).
- **Context propagation** (e.g., `traceparent` / `tracestate` in HTTP headers) ties those spans into one trace. That is
  usually done automatically by the OpenTelemetry agent or SDK when the gateway and backends use the same standard (
  e.g., W3C).
- **Business attributes** (user.id, request.id, tenant.id) are **per-span**. The gateway can set them on the gateway can
  set them on the gateway span and pu the same values in headers; they do **not** automatically appear on the backends'
  spans. So if you want every service's span to carry user/request/tenant, each service that receives a request must
  either:

> read those headers and set them on the **current** span (e.g., with a shared filter), or
> rely on the backend agent to copy from headers (if your stack supports that).

So:

- **Gateway** ideal for **starting the trace**, **propagating trace context**, and **originating business headers** (and
  setting them on the gateway span).
- **Every app that can receive a request**(gateway + all backends): should **participate in propagation** and if you
  want business context on every span, **set span attributes from header** (or equivalent) in a consistent way.

The way you get one trace, one root at the gateway, and every service's span can carry the same business context when
present.

---

## 4. Design Principles (Summary)

1. **Single trace, many spans**
   One logical request -> one trace id; each service (and possible messaging/DB) adds spans. Use **W3C TraceContext** (
   or equivalent) so every hop continuous the same trace.

2. **Gateway as entry**
   Use the gateway to create the root span and to originate or forward trace context and business headers. This gives a
   clear, single entry point for tracing.

3. **Instrument every service**
   Attach the same tracing stack (e.g., OpenTelemetry Java Agent) to **every** app (gateway and backends) so that HTTP,
   DB, and messaging calls are automatically spanned and context is propagated.

4. **Business context everywhere**
   If you want user/request/tenant on every span, use the **same** logic (e.g., a shared filter) in the **gateway and in
   every backend**: read headers, set attributes on the current span. That keeps behavior consistent and avoids
   special-casing one app.

5. **No single point of failure**
   If some traffic can hit a backend without going through the gateway (e.g., internal calls, or gateway not deployed in
   some environments), that backend skill has the same filter and can enrich its span form headers. So "gateway as
   entry" is the preferred path, but backends remain self-sufficient.

---

## 5. Practical Checklist

- **Gateway**
    - Run the OTel agent (or SDK).
    - Start/forward trace context; set or forward `X-User-Id`, `X-Request-Id`, `X-Tenant-Id` (e.g., from JWT)
    - Use the same "tracing context" filter as backends so the gateway span has user/request/tenant.

- **Every backend**
    - Run the OTel agent.
    - Rely on propagation so their spans are part of the same trace.
    - Use the same filter to copy headers onto the current span so business context is consistent.

- **Shared component**
    - One implementation of the "read headers -> set span attributes" logic (e.g. in a shared library or filter) used by
      gateway and all backends avoids drift and keeps the design easy to reason about.

- **Deployment**
    - Same agent and env (e.g. `OTEL_SERVICE_NAME`, `OTEL_EXPORTER_OLTP_ENDPOINT`) in every container; only the service
      name and optional resources attributes differ per app.

---

## 6. When the Gateway Is Not the Only Entry

In some setups , we might have

- **Direct calls to backends** (e.g. from another service or from a job).
- **No gateway** in a given environment (e.g. minimal docker-compose)
- **Multiple gateways or entry points** (e.g. public API vs internal API).

In those cases the same idea holds: **whoever handles the request first** should start or continue the trace and
set/forward context; **every service** that can retrieve a request should use the same instrumentation and the same
logic to attach busiess context to its span. So the gateway is the **typical** and **recommended** entry point when it
exists, but the design does not depend on it being the only one-hence using the same filter in gateway and backends.


---

## 7. References in This repo

- **Observability plan**: `docs/plan.md` (tracing, metrics, logging).
- **APM probe and context**: `docs/amp-probes-tracing-context.md` (what to put on spans, why).
- **Shared filter**: `observability-support` module and its README (use in gateway + all backends).

This document summarises the **design rationale**; implementation details (OTLP, Jaeger, agent version, env vars) are in
the plan and the codebase.

