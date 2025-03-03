#!/bin/bash


echo "delete container beginning"

# Stop and remove all pods first to handle dependencies
podman pod stop -a 2>/dev/null || true
podman pod rm -f -a 2>/dev/null || true

# Force remove containers if they exist
for container in $(podman ps -a | grep -E "kiwi-eureka|kiwi-config|kiwi-gate|kiwi-upms|kiwi-auth|kiwi-word-biz|kiwi-crawler" | awk '{print $1}'); do
    [ -n "$container" ] && podman rm -f "$container"
done

# Clean up any Docker containers as well (since you're mixing Docker and Podman)
for container in $(docker ps -a | grep -E "kiwi-eureka|kiwi-config|kiwi-gate|kiwi-upms|kiwi-auth|kiwi-word-biz|kiwi-crawler" | awk '{print $1}'); do
    [ -n "$container" ] && docker rm -f "$container"
done

echo "delete image beginning"

# Remove images with Podman and Docker
podman rmi -f kiwi-eureka:2.0 2>/dev/null || true
podman rmi -f kiwi-config:2.0 2>/dev/null || true
podman rmi -f kiwi-upms:2.0 2>/dev/null || true
podman rmi -f kiwi-auth:2.0 2>/dev/null || true
podman rmi -f kiwi-gate:2.0 2>/dev/null || true
podman rmi -f kiwi-word-biz:2.0 2>/dev/null || true
podman rmi -f kiwi-crawler:2.0 2>/dev/null || true

echo "docker build beginning"

docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:2.0 ~/docker/kiwi/eureka/
docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:2.0 ~/docker/kiwi/config/
docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:2.0 ~/docker/kiwi/upms/
docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:2.0 ~/docker/kiwi/auth/
docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:2.0 ~/docker/kiwi/gate/
docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:2.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:2.0 ~/docker/kiwi/crawler/

echo "podman-compose base beginning"

# Ensure images are available to Podman (tag Docker-built images for Podman)
for image in kiwi-eureka:2.0 kiwi-config:2.0 kiwi-upms:2.0 kiwi-auth:2.0 kiwi-gate:2.0 kiwi-word-biz:2.0 kiwi-crawler:2.0; do
    docker tag "$image" "localhost/$image"
done

podman-compose -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d --force-recreate --remove-orphans
echo "success wait 100s"
sleep 100s
echo "podman-compose service beginning"
podman-compose -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-service.yml up -d --force-recreate --remove-orphans

# Stop crawler using Podman (since we're using podman-compose)
crawler_id=$(podman ps -a | grep kiwi-crawler | awk '{print $1}')
[ -n "$crawler_id" ] && podman stop "$crawler_id"

echo "crawler service was stopped, other service have finished, good job!"