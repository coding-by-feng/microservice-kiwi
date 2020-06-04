#!/bin/bash

echo "stop container beginning"

docker stop `docker ps -a| grep kiwi-eureka | awk '{print $1}' `
docker stop `docker ps -a| grep kiwi-config | awk '{print $1}' `
docker stop `docker ps -a| grep kiwi-upms | awk '{print $1}' `
docker stop `docker ps -a| grep kiwi-auth | awk '{print $1}' `
docker stop `docker ps -a| grep kiwi-gate | awk '{print $1}' `
docker stop `docker ps -a| grep kiwi-word | awk '{print $1}' `
docker stop `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "delete container beginning"

docker rm   `docker ps -a| grep kiwi-eureka | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-config | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-upms | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-auth | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-word | awk '{print $1}' `
docker rm   `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "delete image beginning"

docker rmi kiwi-eureka:1.0
docker rmi kiwi-config:1.0
docker rmi kiwi-upms:1.0
docker rmi kiwi-auth:1.0
docker rmi kiwi-gate:1.0
docker rmi kiwi-word:1.0
docker rmi kiwi-crawler:1.0

echo "docker build beginning"

docker build -f /root/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:1.0 /root/docker/kiwi/eureka/
docker build -f /root/docker/kiwi/config/Dockerfile -t kiwi-config:1.0 /root/docker/kiwi/config/
docker build -f /root/docker/kiwi/upms/Dockerfile -t kiwi-upms:1.0 /root/docker/kiwi/upms/
docker build -f /root/docker/kiwi/auth/Dockerfile -t kiwi-auth:1.0 /root/docker/kiwi/auth/
docker build -f /root/docker/kiwi/gate/Dockerfile -t kiwi-gate:1.0 /root/docker/kiwi/gate/
docker build -f /root/docker/kiwi/word/Dockerfile -t kiwi-word:1.0 /root/docker/kiwi/word/
docker build -f /root/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:1.0 /root/docker/kiwi/crawler/

echo "docker-compose base beginning"

docker-compose -f /root/microservice-kiwi/docker-compose-base.yml up -d

echo "success wait 100s"

sleep 100s

echo "docker-compose service beginning"

docker-compose -f /root/microservice-kiwi/docker-compose-service.yml up -d

docker stop `docker ps -a| grep kiwi-crawler | awk '{print $1}' `

echo "all job finish, that is great!"