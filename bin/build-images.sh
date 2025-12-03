#!/bin/bash
set -e

# Docker build
for module in customer fraud notification apigw; do
  echo "Building Docker image for $module ..."
  docker build -t nanachi1027/aurora-$module:latest ./$module
done

# List images
docker images | grep aurora | grep nanachi1027

# Push images
for module in customer fraud notification apigw; do
  docker push nanachi1027/aurora-$module:latest
done

