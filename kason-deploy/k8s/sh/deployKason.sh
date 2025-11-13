#!/bin/bash

rm -rf ~/docker/kason/eureka/logs/*
rm -rf ~/docker/kason/config/logs/*
rm -rf ~/docker/kason/upms/logs/*
rm -rf ~/docker/kason/auth/logs/*
rm -rf ~/docker/kason/gate/logs/*
rm -rf ~/docker/kason/word/logs/*
rm -rf ~/docker/kason/word/logs_02/*
rm -rf ~/docker/kason/word/crawlerTmp/*
rm -rf ~/docker/kason/word/crawlerTmp_02/*
rm -rf ~/docker/kason/word/bizTmp/*
rm -rf ~/docker/kason/word/bizTmp_02/*
rm -rf ~/docker/kason/crawler/logs/*

cd ~/microservice-kason/ || exit

echo "git pulling..."

git fetch --all
git reset --hard origin/master
git pull origin master

mvn clean install -Dmaven.test.skip=true

mv -f ~/microservice-kason/kason-eureka/Dockerfile ~/docker/kason/eureka/
mv -f ~/microservice-kason/kason-config/Dockerfile ~/docker/kason/config/
mv -f ~/microservice-kason/kason-upms/kason-upms-biz/Dockerfile ~/docker/kason/upms/
mv -f ~/microservice-kason/kason-word/kason-word-biz/docker/biz/Dockerfile ~/docker/kason/word/biz
mv -f ~/microservice-kason/kason-word/kason-word-biz/docker/crawler/Dockerfile ~/docker/kason/word/crawler
mv -f ~/microservice-kason/kason-word/kason-word-crawler/Dockerfile ~/docker/kason/crawler/
mv -f ~/microservice-kason/kason-auth/Dockerfile ~/docker/kason/auth/
mv -f ~/microservice-kason/kason-gateway/Dockerfile ~/docker/kason/gate/

mv -f ~/.m2/repository/me/fengorz/kason-eureka/2.0/kason-eureka-2.0.jar ~/docker/kason/eureka/
mv -f ~/.m2/repository/me/fengorz/kason-config/2.0/kason-config-2.0.jar ~/docker/kason/config/
mv -f ~/.m2/repository/me/fengorz/kason-upms-biz/2.0/kason-upms-biz-2.0.jar ~/docker/kason/upms/
mv -f ~/.m2/repository/me/fengorz/kason-auth/2.0/kason-auth-2.0.jar ~/docker/kason/auth/
mv -f ~/.m2/repository/me/fengorz/kason-gateway/2.0/kason-gateway-2.0.jar ~/docker/kason/gate/
mv -f ~/.m2/repository/me/fengorz/kason-word-biz/2.0/kason-word-biz-2.0.jar ~/docker/kason/word/
mv -f ~/.m2/repository/me/fengorz/kason-word-crawler/2.0/kason-word-crawler-2.0.jar ~/docker/kason/crawler/

echo "docker build beginning"

docker build -f ~/docker/kason/eureka/Dockerfile -t kason-eureka:2.0 ~/docker/kason/eureka/
docker build -f ~/docker/kason/config/Dockerfile -t kason-config:2.0 ~/docker/kason/config/
docker build -f ~/docker/kason/upms/Dockerfile -t kason-upms:2.0 ~/docker/kason/upms/
docker build -f ~/docker/kason/auth/Dockerfile -t kason-auth:2.0 ~/docker/kason/auth/
docker build -f ~/docker/kason/gate/Dockerfile -t kason-gate:2.0 ~/docker/kason/gate/
docker build -f ~/docker/kason/word/biz/Dockerfile -t kason-word-biz:2.0 ~/docker/kason/word/
docker build -f ~/docker/kason/crawler/Dockerfile -t kason-crawler:2.0 ~/docker/kason/crawler/

