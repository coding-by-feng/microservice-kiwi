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
docker build -f ~/docker/kiwi/ai/batch/Dockerfile -t kiwi-ai-batch:2.0 ~/docker/kiwi/ai/batch

# Tag images for Podman
for image in kiwi-{eureka,config,upms,auth,gate,word-biz,crawler,ai-biz}:2.0; do
  docker tag "$image" "localhost/$image"
done

echo "podman-compose base beginning"
podman-compose --project-name kiwi-base -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d --remove-orphans --build 2>/dev/null || { echo "Base compose failed"; exit 1; }
echo "Success, waiting 100s..."
sleep 100s

echo "podman-compose service beginning"
podman-compose --project-name kiwi-service -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-service.yml up -d --force-recreate --remove-orphans --build || { echo "Service compose failed"; exit 1; }

# Stop crawler
crawler_id=$(podman ps -a -q --filter "name=kiwi-crawler")
[ -n "$crawler_id" ] && podman stop "$crawler_id" && echo "Crawler stopped"

echo "Deployment completed successfully!"