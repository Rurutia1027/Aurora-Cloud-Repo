# OpenTelemetry Java Agent: Docker and Script

How the tracing agent is used and why the download script is not committed.

---

## 1. Two Ways to Provide the Agent

| Approach                 | Where                                                                                                                                                          | Use case                                                                                                                                    |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| **Baked into the image** | Dockerfile downloads the agent at build time and `ENTRYPOINT` uses `-javaagent:/otel/opentelemetry-javaagent.jar`.                                             | CI builds (e.g. `build-image.sh`); Kubernetes; any environment where the image is self-contained. No host mount needed.                     |
| **Mounted at runtime**   | Host runs `bin/download-otel-agent.sh` once; docker-compose mounts `./otel-agent/opentelemetry-javaagent.jar` into the container and sets `JAVA_TOOL_OPTIONS`. | Using **pre-built images** that do not contain the agent (e.g. older or third-party images); local runs where you do not rebuild the image. |

---

## 2. What Is Excluded from Committed Code

- **`/otel-agent/`** -- directory where the download script writes the agent JAR. It is in `.gitignore`, so the binary
  is never committed.
- **`bin/download-otel-agent.sh`** -- this script **is** committed; it is a small helper that fetches the agent JAR from
  the official GitHub releases. The **output** of the script (the JAR under `otel-agent/`) is what we exclude.

So: the script is in the repo; the downloaded file is not.

---

## 3. What the Dockerfiles Do (When You Build Images)

Each app Dockerfile (customer, fraud, apigw, and notification) now:

**Downloads the agent during build**

- Uses a `RUN` with `curl` and the default (for build-arg) URL.
- Puts the JAR at `/otel/opentelemetry-javaagent.jar` inside the image.

**Starts the app with the agent**

- `ENTRYPOINT` includes `-javaagent:/otel/opentelemetry-javaagent.jar` before `-jar ../app.jar`.
- No need to set `JAVA_TOOL_OPTIONS` for the agent path when the image is built this way.

**Leaves configuration to runtime**

- `OTEL_SERVICE_NAME`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `OTEL_TRACES_SAMPLER`, etc. are **not** baked in; they are set via
  environment (e.g., docker-compose or Kubernetes) so the same image can be used in different environments.

**Build-arg (optional):**  
You can pin the agent version at build time:

```bash
docker build --build-arg OTEL_AGENT_URL=https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.0.0/opentelemetry-javaagent.jar -t myimage .
```

---

## 4. Docker Compose: With or Without Mount

- **If the image is built from these Dockerfiles**, the agent is already in the image. You only need to set the OTEL env
  vars (e.g. in `docker-compose.yml`); you do **not** need to run `download-otel-agent.sh` or mount the agent volume.

- **If you use an image that does not contain the agent** (e.g. an older or external image), then:
    1. Run once: `./bin/download-otel-agent.sh`
    2. Keep the compose volume: `./otel-agent/opentelemetry-javaagent.jar:/otel/opentelemetry-javaagent.jar:ro`
    3. Keep `JAVA_TOOL_OPTIONS=-javaagent:/otel/opentelemetry-javaagent.jar`

So: **Dockerfile = agent in image, no script/mount required for that image.**  
**Script + mount = for images that don’t ship the agent.**

---

## 5. Summary

| Item                              | Committed?            | Purpose                                                                  |
|-----------------------------------|-----------------------|--------------------------------------------------------------------------|
| `bin/download-otel-agent.sh`      | Yes                   | Fetch agent JAR to `./otel-agent/` for runtime mount.                    |
| `/otel-agent/` (and its contents) | No (gitignored)       | Avoid committing the binary.                                             |
| Dockerfiles                       | Yes                   | Build images that include the agent and start the JVM with `-javaagent`. |
| Runtime config (OTEL_* env)       | Yes (e.g. in compose) | Configure exporter, service name, sampler; not baked into the image.     |

You do **not** need to copy the agent JAR into the repo. Either the Dockerfile downloads it during build, or the script
downloads it on the host and compose mounts it at run time.
