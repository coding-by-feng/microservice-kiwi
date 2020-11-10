#!/bin/bash

echo "delete container beginning"

docker rm   `docker ps -a| grep kiwi-eureka | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-config | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-upms | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-auth | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-word-biz | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-word-biz-crawler | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "delete image beginning"

docker rmi kiwi-eureka:1.0
docker rmi kiwi-config:1.0
docker rmi kiwi-upms:1.0
docker rmi kiwi-auth:1.0
docker rmi kiwi-gate:1.0
docker rmi kiwi-word-biz:1.0
docker rmi kiwi-word-biz-crawler:1.0
docker rmi kiwi-crawler:1.0

echo "docker build beginning"

docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:1.0 ~/docker/kiwi/eureka/
docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:1.0 ~/docker/kiwi/config/
docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:1.0 ~/docker/kiwi/upms/
docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:1.0 ~/docker/kiwi/auth/
docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:1.0 ~/docker/kiwi/gate/
docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:1.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/word/crawler/Dockerfile -t kiwi-word-biz-crawler:1.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:1.0 ~/docker/kiwi/crawler/

echo "docker-compose base beginning"

docker-compose -f ~/microservice-kiwi/docker-compose-base.yml up -d

echo "success wait 100s"

sleep 100s

echo "docker-compose service beginning"

docker-compose -f ~/microservice-kiwi/docker-compose-service.yml up -d

docker stop `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "all job finish, that is great!"