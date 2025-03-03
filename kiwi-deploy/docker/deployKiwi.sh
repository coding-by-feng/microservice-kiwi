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
mv -f ~/microservice-kiwi/kiwi-{eureka,config,auth,gateway}/Dockerfile ~/docker/kiwi/{eureka,config,auth,gate}/
mv -f ~/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/{biz,crawler}/Dockerfile ~/docker/kiwi/word/{biz,crawler}/
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
mv -f ~/.m2/repository/me/fengorz/kiwi-{eureka,config,upms-biz,auth,gateway,word-biz,word-crawler}/2.0/kiwi-*.jar ~/docker/kiwi/{eureka,config,upms,auth,gate,word,crawler}/

# Run autoDeploy with parameter
~/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh "$MODE"

echo "Sleeping for 200 seconds..."
sleep 200s

# Start autoCheckService if not already running
if ! pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Starting autoCheckService..."
  nohup ~/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh >~/autoCheck.log 2>&1 &
fi