#!/bin/bash

echo "delete container beginning"

# Force remove containers if they exist
for container_id in $(podman ps -a | grep -E "kiwi-crawler|kiwi-word-biz|kiwi-upms|kiwi-auth|kiwi-gate|kiwi-config|kiwi-eureka" | awk '{print $1}'); do
    if [ -n "$container_id" ]; then
        # Get the container name
        container_name=$(podman inspect --format '{{.Name}}' "$container_id" 2>/dev/null)

        # Print container ID and name
        echo "Removing container ID: $container_id, Name: $container_name"

        # Remove the container
        podman rm -f "$container_id"

        echo "Removed"
    fi
done

echo "docker build beginning"

docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:2.0 ~/docker/kiwi/eureka/
docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:2.0 ~/docker/kiwi/config/
docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:2.0 ~/docker/kiwi/upms/
docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:2.0 ~/docker/kiwi/auth/
docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:2.0 ~/docker/kiwi/gate/
docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:2.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:2.0 ~/docker/kiwi/crawler/

# Tag images for Podman
for image in kiwi-{eureka,config,upms,auth,gate,word-biz,crawler}:2.0; do
  docker tag "$image" "localhost/$image"
done

echo "podman-compose base beginning"
podman-compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d --remove-orphans --build || { echo "Base compose failed"; exit 1; }
podman-compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d --remove-orphans --build 2>/dev/null || { echo "Base compose failed"; exit 1; }
echo "Success, waiting 100s..."
sleep 100s

echo "podman-compose service beginning"
podman-compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-service.yml up -d --force-recreate --remove-orphans --build || { echo "Service compose failed"; exit 1; }

# Stop crawler
crawler_id=$(podman ps -a -q --filter "name=kiwi-crawler")
[ -n "$crawler_id" ] && podman stop "$crawler_id" && echo "Crawler stopped"

echo "Deployment completed successfully!"