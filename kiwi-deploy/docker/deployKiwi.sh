#!/bin/bash

#
# Copyright [2019~2025] [codingByFeng]
# Licensed under the Apache License, Version 2.0
# See http://www.apache.org/licenses/LICENSE-2.0 for details
#

cd ~/microservice-kiwi/ || { echo "Failed to cd to ~/microservice-kiwi"; exit 1; }

# Parse mode parameter
MODE="$1"
SKIP_GIT=false
SKIP_MAVEN=false
case "$MODE" in
  -mode=sg)
    SKIP_GIT=true
    echo "Skipping git stash and pull operations"
    ;;
  -mode=sm)
    SKIP_MAVEN=true
    echo "Skipping maven build operation"
    ;;
  -mode=sb)
    SKIP_GIT=true
    SKIP_MAVEN=true
    echo "Skipping both git and maven operations"
    ;;
  *)
    echo "No mode specified or invalid mode, proceeding with all operations"
    ;;
esac

# Git operations
if [ "$SKIP_GIT" = false ]; then
  echo "Git pulling..."
  git stash && git pull || { echo "Git operations failed"; exit 1; }
else
  echo "Git operations skipped due to -mode=sg"
fi

chmod 777 ~/microservice-kiwi/kiwi-deploy/docker/*.sh

# Clean log directories efficiently
echo "Cleaning log directories..."
rm -rf ~/docker/kiwi/eureka/logs/*
rm -rf ~/docker/kiwi/config/logs/*
rm -rf ~/docker/kiwi/upms/logs/*
rm -rf ~/docker/kiwi/auth/logs/*
rm -rf ~/docker/kiwi/gate/logs/*
rm -rf ~/docker/kiwi/word/logs/*
rm -rf ~/docker/kiwi/word/crawlerTmp/*
rm -rf ~/docker/kiwi/word/bizTmp/*
rm -rf ~/docker/kiwi/crawler/logs/*

# Maven build
if [ "$SKIP_MAVEN" = false ]; then
  echo "Running maven build..."
  mvn clean install -Dmaven.test.skip=true -B || { echo "Maven build failed"; exit 1; }
else
  echo "Maven build skipped due to -mode=sm"
fi

# Move Dockerfiles and JARs efficiently
echo "Moving Dockerfiles and JARs..."
mv -f ~/microservice-kiwi/kiwi-eureka/Dockerfile ~/docker/kiwi/eureka/
mv -f ~/microservice-kiwi/kiwi-config/Dockerfile ~/docker/kiwi/config/
mv -f ~/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile ~/docker/kiwi/word/biz
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile ~/docker/kiwi/word/crawler
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
mv -f ~/microservice-kiwi/kiwi-auth/Dockerfile ~/docker/kiwi/auth/
mv -f ~/microservice-kiwi/kiwi-gateway/Dockerfile ~/docker/kiwi/gate/

mv -f ~/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar ~/docker/kiwi/eureka/
mv -f ~/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar ~/docker/kiwi/config/
mv -f ~/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar ~/docker/kiwi/upms/
mv -f ~/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar ~/docker/kiwi/auth/
mv -f ~/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar ~/docker/kiwi/gate/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar ~/docker/kiwi/word/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar ~/docker/kiwi/crawler/

# Run autoDeploy with parameter
~/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh "$MODE"

echo "Sleeping for 200 seconds..."
sleep 200s

# Start autoCheckService if not already running
if ! pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Starting autoCheckService..."
  nohup ~/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh >~/autoCheck.log 2>&1 &
fi