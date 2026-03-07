# Curl commands to verify Jaeger (docker-compose)

Run these from the host (or from another container on the same network). Replace `localhost` with the container name (
e.g. `jaeger`) when calling from inside the compose network.

## 1. Jaeger UI (HTTP 200 = OK)

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:16686
# Expected: 200
```

## 2. Jaeger UI root (fetch page)

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:16686/
# Expected: 200
```

## 3. Jaeger API – list services (after some traces exist)

```bash
curl -s "http://localhost:16686/api/services" | head -c 200
# Expected: JSON array of service names, e.g. ["apigw","customer",...]
```

## 4. Jaeger API – list traces (optional)

```bash
curl -s "http://localhost:16686/api/traces?limit=1" | head -c 300
# Expected: JSON with "data" array (may be empty if no traffic yet)
```

## 5. From inside the compose network (e.g. another service)

```bash
docker compose exec customer curl -s -o /dev/null -w "%{http_code}\n" http://jaeger:16686/
# Expected: 200
```

## One-liner: quick health check

```bash
curl -sf http://localhost:16686/ && echo "Jaeger UI OK" || echo "Jaeger UI FAIL"
```

---

**Note:** OTLP (traces) uses port **4317** (gRPC). The agent uses gRPC to send traces; there is no simple HTTP curl for
4317. The UI and API on **16686** are enough to confirm Jaeger is up and that traces appear after traffic through
apigw/customer/fraud/notification.

---

## One curl through all services (full span trace)

This request goes **apigw → customer → fraud** (and customer publishes to RabbitMQ; notification may appear if trace
context is propagated over AMQP). Run with stack up, then open Jaeger UI → Search → pick service **apigw** or **customer
** and “Find Traces”.

```bash
curl -X POST "http://localhost:8083/api/v1/customers" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: user-1" \
  -H "X-Request-Id: $(uuidgen 2>/dev/null || echo $RANDOM)" \
  -H "X-Tenant-Id: tenant-1" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane@example.com"}'
```

- **Path:** Gateway forwards `/api/v1/customers` to **customer**; customer then calls **fraud** (
  `GET api/v1/fraud/check/{customerId}`) and publishes to RabbitMQ for **notification**.
- **Jaeger:** Open http://localhost:16686 → Service **apigw** or **customer** → Find Traces. You should see one trace
  with spans: apigw (root) → customer → fraud (and optionally notification consumer if AMQP propagation is on).

---

## Troubleshooting: "Failed to resolve 'customer'"

If the gateway returns 500 and logs show `UnknownHostException: Failed to resolve 'customer'`, the apigw container
cannot resolve the backend hostnames. Fixes:

1. **Run everything with the same compose**  
   Start all services with `docker compose up` so apigw and customer/fraud/notification share the same network (
   `aurora-net`) and DNS.

2. **If apigw runs outside Docker** (e.g. `java -jar` on the host), set backend URIs to where the backends are
   reachable:
   ```bash
   export CUSTOMER_URI=http://localhost:8080
   export FRAUD_URI=http://localhost:8081
   export NOTIFICATION_URI=http://localhost:8082
   ```
   Then start the gateway. On Mac/Windows with backends in Docker, use `http://host.docker.internal:8080` etc. if
   needed.

3. **Rebuild/restart**  
   After changing `application.yml` or env vars, rebuild the apigw image (if you changed YAML) and restart:
   `docker compose up -d --force-recreate apigw`.
