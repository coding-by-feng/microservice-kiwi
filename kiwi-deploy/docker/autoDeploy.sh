#!/bin/bash

echo "docker build beginning"

docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:2.0 ~/docker/kiwi/eureka/
docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:2.0 ~/docker/kiwi/config/
docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:2.0 ~/docker/kiwi/upms/
docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:2.0 ~/docker/kiwi/auth/
docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:2.0 ~/docker/kiwi/gate/
docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:2.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:2.0 ~/docker/kiwi/crawler/
docker build -f ~/docker/kiwi/ai/biz/Dockerfile -t kiwi-ai-biz:2.0 ~/docker/kiwi/ai/biz
docker build -f ~/docker/kiwi/ai/batch/Dockerfile -t kiwi-ai-biz-batch:2.0 ~/docker/kiwi/ai/batch

echo "podman-compose base beginning"
podman-compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d --remove-orphans --build
base_exit_code=$?
if [ $base_exit_code -ne 0 ]; then
    echo "Base compose failed with exit code: $base_exit_code"
    exit 1
fi
echo "Base compose completed successfully, waiting 100s..."
sleep 100s

echo "podman-compose service beginning"
podman-compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-service.yml up -d --force-recreate --remove-orphans --build
service_exit_code=$?
if [ $service_exit_code -ne 0 ]; then
    echo "Service compose failed with exit code: $service_exit_code"
    exit 1
fi
echo "Service compose completed successfully"

# Stop crawler
crawler_id=$(podman ps -a -q --filter "name=kiwi-crawler")
[ -n "$crawler_id" ] && podman stop "$crawler_id" && echo "Crawler stopped"

echo "Deployment completed successfully!"