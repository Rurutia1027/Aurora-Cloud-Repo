#!/usr/bin/env bash
# Download OpenTelemetry Java Agent for use with docker-compose (see docs/plan.md, docs/amp-probes-tracing-context.md).
# Run once before: docker-compose up

set -euo pipefail

OTEL_AGENT_DIR="${OTEL_AGENT_DIR:-$(cd "$(dirname "$0")/.." && pwd)/otel-agent}"
DOWNLOAD_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
JAR_NAME="opentelemetry-javaagent.jar"
OUTPUT_PATH="${OTEL_AGENT_DIR}/${JAR_NAME}"

mkdir -p "$OTEL_AGENT_DIR"
echo "Downloading OpenTelemetry Java Agent to ${OUTPUT_PATH} ..."
if command -v curl &>/dev/null; then
  curl -sSL -o "$OUTPUT_PATH" "$DOWNLOAD_URL"
elif command -v wget &>/dev/null; then
  wget -q -O "$OUTPUT_PATH" "$DOWNLOAD_URL"
else
  echo "Need curl or wget to download the agent." >&2
  exit 1
fi
echo "Done. Use mount: ${OTEL_AGENT_DIR}/${JAR_NAME} -> /otel/opentelemetry-javaagent.jar"
