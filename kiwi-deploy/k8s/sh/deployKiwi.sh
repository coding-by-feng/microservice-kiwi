#!/bin/bash

rm -rf ~/docker/kiwi/eureka/logs/*
rm -rf ~/docker/kiwi/config/logs/*
rm -rf ~/docker/kiwi/upms/logs/*
rm -rf ~/docker/kiwi/auth/logs/*
rm -rf ~/docker/kiwi/gate/logs/*
rm -rf ~/docker/kiwi/word/logs/*
rm -rf ~/docker/kiwi/word/logs_02/*
rm -rf ~/docker/kiwi/word/crawlerTmp/*
rm -rf ~/docker/kiwi/word/crawlerTmp_02/*
rm -rf ~/docker/kiwi/word/bizTmp/*
rm -rf ~/docker/kiwi/word/bizTmp_02/*
rm -rf ~/docker/kiwi/crawler/logs/*

cd ~/microservice-kiwi/ || exit

echo "git pulling..."

git fetch --all
git reset --hard origin/master
git pull origin master

mvn clean install -Dmaven.test.skip=true

mv -f ~/microservice-kiwi/kiwi-eureka/Dockerfile ~/docker/kiwi/eureka/
mv -f ~/microservice-kiwi/kiwi-config/Dockerfile ~/docker/kiwi/config/
mv -f ~/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile ~/docker/kiwi/word/biz
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile ~/docker/kiwi/word/crawler
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
mv -f ~/microservice-kiwi/kiwi-auth/Dockerfile ~/docker/kiwi/auth/
mv -f ~/microservice-kiwi/kiwi-gateway/Dockerfile ~/docker/kiwi/gate/

mv -f ~/.m2/repository/me/fengorz/kiwi-eureka/1.0/kiwi-eureka-1.0.jar ~/docker/kiwi/eureka/
mv -f ~/.m2/repository/me/fengorz/kiwi-config/1.0/kiwi-config-1.0.jar ~/docker/kiwi/config/
mv -f ~/.m2/repository/me/fengorz/kiwi-upms-biz/1.0/kiwi-upms-biz-1.0.jar ~/docker/kiwi/upms/
mv -f ~/.m2/repository/me/fengorz/kiwi-auth/1.0/kiwi-auth-1.0.jar ~/docker/kiwi/auth/
mv -f ~/.m2/repository/me/fengorz/kiwi-gateway/1.0/kiwi-gateway-1.0.jar ~/docker/kiwi/gate/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-biz/1.0/kiwi-word-biz-1.0.jar ~/docker/kiwi/word/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-crawler/1.0/kiwi-word-crawler-1.0.jar ~/docker/kiwi/crawler/

echo "docker build beginning"

docker build -f ~/docker/kiwi/eureka/Dockerfile -t kiwi-eureka:1.0 ~/docker/kiwi/eureka/
docker build -f ~/docker/kiwi/config/Dockerfile -t kiwi-config:1.0 ~/docker/kiwi/config/
docker build -f ~/docker/kiwi/upms/Dockerfile -t kiwi-upms:1.0 ~/docker/kiwi/upms/
docker build -f ~/docker/kiwi/auth/Dockerfile -t kiwi-auth:1.0 ~/docker/kiwi/auth/
docker build -f ~/docker/kiwi/gate/Dockerfile -t kiwi-gate:1.0 ~/docker/kiwi/gate/
docker build -f ~/docker/kiwi/word/biz/Dockerfile -t kiwi-word-biz:1.0 ~/docker/kiwi/word/
docker build -f ~/docker/kiwi/crawler/Dockerfile -t kiwi-crawler:1.0 ~/docker/kiwi/crawler/

