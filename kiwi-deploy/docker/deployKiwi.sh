#!/bin/bash

#
# Copyright [2019~2025] [Kason Zhan]
# Licensed under the Apache License, Version 2.0
# See http://www.apache.org/licenses/LICENSE-2.0 for details
#

# Exit immediately if a command exits with a non-zero status
set -e

# Function to handle errors
error_handler() {
  local line_number=$1
  local command="$2"
  echo "=============================================="
  echo "ERROR: Script failed at line $line_number"
  echo "Failed command: $command"
  echo "Exit code: $?"
  echo "=============================================="
  exit 1
}

# Set error trap
trap 'error_handler ${LINENO} "$BASH_COMMAND"' ERR

# Check if script is run with sudo privileges
if [ "$EUID" -ne 0 ]; then
  echo "Error: This script must be run with sudo privileges"
  echo "Please run: sudo $0 $*"
  exit 1
fi

cd ~/microservice-kiwi/ || { echo "CRITICAL ERROR: Failed to change directory to ~/microservice-kiwi/"; exit 1; }

# Function to show help
show_help() {
  echo "Usage: $0 [MODE]"
  echo ""
  echo "Available modes:"
  echo "  -mode=sg      Skip git operations (stash and pull)"
  echo "  -mode=sm      Skip maven build operation"
  echo "  -mode=sbd     Skip Dockerfile building operation (copying Dockerfiles and JARs)"
  echo "  -mode=sa      Skip all operations (git + maven + Dockerfile building)"
  echo "  -help         Show this help message"
  echo ""
  echo "If no mode is specified, all operations will be executed."
  echo ""
  echo "Examples:"
  echo "  sudo $0                # Run all operations"
  echo "  sudo $0 -mode=sg       # Skip only git operations"
  echo "  sudo $0 -mode=sm       # Skip only maven build"
  echo "  sudo $0 -mode=sbd      # Skip only Dockerfile building"
  echo "  sudo $0 -mode=sa       # Skip all operations"
}

# Parse mode parameter
MODE="$1"
SKIP_GIT=false
SKIP_MAVEN=false
SKIP_DOCKER_BUILD=false
case "$MODE" in
  -mode=sg)
    SKIP_GIT=true
    echo "Skipping git stash and pull operations"
    ;;
  -mode=sm)
    SKIP_MAVEN=true
    echo "Skipping maven build operation"
    ;;
  -mode=sbd)
    SKIP_DOCKER_BUILD=true
    echo "Skipping Dockerfile building operation"
    ;;
  -mode=sa)
    SKIP_GIT=true
    SKIP_MAVEN=true
    SKIP_DOCKER_BUILD=true
    echo "Skipping all operations (git, maven, and Dockerfile building)"
    ;;
  -help|--help|-h)
    show_help
    exit 0
    ;;
  *)
    if [ -n "$MODE" ]; then
      echo "Invalid mode: $MODE"
      echo "Use -help to see available modes"
      exit 1
    else
      echo "No mode specified, proceeding with all operations"
    fi
    ;;
esac

# Git operations
if [ "$SKIP_GIT" = false ]; then
  echo "Git pulling..."
  echo "Stashing local changes..."
  git stash
  echo "Pulling latest changes..."
  git pull
else
  echo "Git operations skipped"
fi

# Set execute permissions
echo "Setting execute permissions for scripts..."
chmod 777 ~/microservice-kiwi/kiwi-deploy/docker/*.sh
chmod 777 ~/microservice-kiwi/kiwi-deploy/kiwi-ui/*.sh

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
rm -rf ~/docker/kiwi/ai/logs/*
rm -rf ~/docker/kiwi/ai/tmp/*

# Maven build
if [ "$SKIP_MAVEN" = false ]; then
  echo "Installing VoiceRSS TTS library..."
  cd ~/microservice-kiwi/kiwi-common/kiwi-common-tts/lib
  mvn install:install-file \
      -Dfile=voicerss_tts.jar \
      -DgroupId=voicerss \
      -DartifactId=tts \
      -Dversion=2.0 \
      -Dpackaging=jar
  cd ~/microservice-kiwi/

  echo "Running maven build..."
  mvn clean install -Dmaven.test.skip=true -B
else
  echo "Maven build skipped"
fi

# Move Dockerfiles and JARs efficiently
if [ "$SKIP_DOCKER_BUILD" = false ]; then
  echo "Moving Dockerfiles and JARs..."
  echo "Copying Dockerfiles..."
  cp -f ~/microservice-kiwi/kiwi-eureka/Dockerfile ~/docker/kiwi/eureka/
  cp -f ~/microservice-kiwi/kiwi-config/Dockerfile ~/docker/kiwi/config/
  cp -f ~/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
  cp -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile ~/docker/kiwi/word/biz
  cp -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile ~/docker/kiwi/word/crawler
  cp -f ~/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
  cp -f ~/microservice-kiwi/kiwi-auth/Dockerfile ~/docker/kiwi/auth/
  cp -f ~/microservice-kiwi/kiwi-gateway/Dockerfile ~/docker/kiwi/gate/
  cp -f ~/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/biz/Dockerfile ~/docker/kiwi/ai/biz
  cp -f ~/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/batch/Dockerfile ~/docker/kiwi/ai/batch

  echo "Copying JAR files..."
  cp -f ~/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar ~/docker/kiwi/eureka/
  cp -f ~/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar ~/docker/kiwi/config/
  cp -f ~/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar ~/docker/kiwi/upms/
  cp -f ~/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar ~/docker/kiwi/auth/
  cp -f ~/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar ~/docker/kiwi/gate/
  cp -f ~/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar ~/docker/kiwi/word/
  cp -f ~/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar ~/docker/kiwi/crawler/
  cp -f ~/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar ~/docker/kiwi/ai/biz
  cp -f ~/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar ~/docker/kiwi/ai/batch
else
  echo "Dockerfile building skipped"
fi

echo "Stopping all services..."
~/microservice-kiwi/kiwi-deploy/docker/stopAll.sh "$MODE"

echo "Starting auto deployment..."
~/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh "$MODE"

echo "Sleeping for 200 seconds..."
sleep 200s

# Start autoCheckService if not already running
if ! pgrep -f "autoCheckService.sh" >/dev/null; then
  echo "Starting autoCheckService..."
  nohup ~/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh >~/autoCheck.log 2>&1 &
fi