#!/bin/bash

echo "delete container beginning"

echo "delete container beginning"

# Stop and remove all pods first to handle dependencies
podman pod stop -a 2>/dev/null || true
podman pod rm -f -a 2>/dev/null || true
podman rm -f -a 2>/dev/null || true  # Remove all Podman containers

# Force remove containers if they exist
for container in $(podman ps -a | grep -E "kiwi-eureka|kiwi-config|kiwi-gate|kiwi-upms|kiwi-auth|kiwi-word-biz|kiwi-crawler" | awk '{print $1}'); do
    [ -n "$container" ] && podman rm -f "$container"
done

# Clean up any Docker containers as well (since you're mixing Docker and Podman)
for container in $(docker ps -a | grep -E "kiwi-eureka|kiwi-config|kiwi-gate|kiwi-upms|kiwi-auth|kiwi-word-biz|kiwi-crawler" | awk '{print $1}'); do
    [ -n "$container" ] && docker rm -f "$container"
done

echo "docker build beginning"

# Build all images in one go
for service in eureka config upms auth gate "word/biz" crawler; do
  echo "Building $service..."
  docker build -f ~/docker/kiwi/${service/Dockerfile/Dockerfile} -t kiwi-${service//\//-}:2.0 ~/docker/kiwi/${service%/*}/ || { echo "Build failed for $service"; exit 1; }
done

# Tag images for Podman
for image in kiwi-{eureka,config,upms,auth,gate,word-biz,crawler}:2.0; do
  docker tag "$image" "localhost/$image"
done

echo "podman-compose base beginning"
podman-compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d --force-recreate --remove-orphans --build || { echo "Base compose failed"; exit 1; }
echo "Success, waiting 100s..."
sleep 100s

echo "podman-compose service beginning"
podman-compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-service.yml up -d --force-recreate --remove-orphans --build || { echo "Service compose failed"; exit 1; }

# Stop crawler
crawler_id=$(podman ps -a -q --filter "name=kiwi-crawler")
[ -n "$crawler_id" ] && podman stop "$crawler_id" && echo "Crawler stopped"

echo "Deployment completed successfully!"