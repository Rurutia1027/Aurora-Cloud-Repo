#!/bin/bash
set -e

# Build local images
for module in customer fraud notification apigw; do
  echo "Building LOCAL Docker image for $module ..."
  docker build --platform linux/arm64 -t aurora-$module:local ./$module
done

echo
echo "Built local images:"
docker images | grep aurora | grep local
