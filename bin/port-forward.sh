#!/bin/bash
set -euo pipefail

NAMESPACE="aurora"

# -----------------------------
# Start port-forwards in background
# -----------------------------
kubectl port-forward -n $NAMESPACE svc/postgres 5432:5432 &
POSTGRES_PID=$!

kubectl port-forward -n $NAMESPACE svc/rabbitmq 5672:5672 &
RABBIT_PID=$!

kubectl port-forward -n $NAMESPACE svc/rabbitmq 15672:15672 &
RABBIT_UI_PID=$!

# Give port-forwards a few seconds to be ready
sleep 5

#
## -----------------------------
## Kill background port-forwards after build
## -----------------------------
#kill $POSTGRES_PID $RABBIT_PID $RABBIT_UI_PID || true
