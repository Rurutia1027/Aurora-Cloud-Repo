#!/bin/bash
set -e

# Usage: ./build-images-dockerhub.sh <tag>
TAG=$1
LATEST_TAG="latest"

modules=(customer fraud notification apigw)

# Build and tag images
for module in "${modules[@]}"; do
  echo "Building Docker image for $module ..."

  # Build with unique tag
  docker build --platform  linux/amd64,linux/arm64 -t nanachi1027/aurora-$module:$TAG ./$module

  # Tag also as latest
  docker tag nanachi1027/aurora-$module:$TAG nanachi1027/aurora-$module:$LATEST_TAG
done

# List images
docker images | grep aurora | grep nanachi1027

# Push both tags
for module in "${modules[@]}"; do
  docker push nanachi1027/aurora-$module:$TAG
  docker push nanachi1027/aurora-$module:$LATEST_TAG
done
