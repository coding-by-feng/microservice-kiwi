#!/bin/bash


echo "delete container beginning"

docker rm -f  `docker ps -a| grep kiwi-eureka | awk '{print $1}' `
docker rm -f  `docker ps -a| grep kiwi-config | awk '{print $1}' `
docker rm -f  `docker ps -a| grep kiwi-gate | awk '{print $1}' `
docker rm -f  `docker ps -a| grep kiwi-upms | awk '{print $1}' `
docker rm -f  `docker ps -a| grep kiwi-auth | awk '{print $1}' `
docker rm -f  `docker ps -a| grep kiwi-word-biz | awk '{print $1}' `
docker rm -f  `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "delete image beginning"

docker rmi kiwi-eureka:2.0
docker rmi kiwi-config:2.0
docker rmi kiwi-upms:2.0
docker rmi kiwi-auth:2.0
docker rmi kiwi-gate:2.0
docker rmi kiwi-word-biz:2.0
docker rmi kiwi-crawler:2.0

echo "docker build beginning"

docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:2.0 ~/docker/kiwi/eureka/
docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:2.0 ~/docker/kiwi/config/
docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:2.0 ~/docker/kiwi/upms/
docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:2.0 ~/docker/kiwi/auth/
docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:2.0 ~/docker/kiwi/gate/
docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:2.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:2.0 ~/docker/kiwi/crawler/

echo "podman-compose base beginning"

podman-compose -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-base.yml up -d
echo "success wait 100s"
sleep 100s
echo "podman-compose service beginning"
podman-compose -f ~/microservice-kiwi/kiwi-deploy/docker/podman-compose-service.yml up -d

docker stop `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "crawler service was stopped, other service have finished, good job!"