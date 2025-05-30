#!/bin/bash

# Method 1: Check if running with sudo -E by examining environment variables
check_sudo_e() {
    echo "=============================================="
    echo "CHECKING SUDO -E STATUS:"
    echo "=============================================="

    # Check if running as root
    if [ "$EUID" -ne 0 ]; then
        echo "❌ ERROR: Not running as root/sudo"
        return 1
    fi

    # Check if SUDO_USER is set (indicates sudo was used)
    if [ -z "$SUDO_USER" ]; then
        echo "❌ WARNING: SUDO_USER not set - may not be running via sudo"
        return 1
    fi

    # Check if user environment variables are preserved
    # These are typically set in user's .bashrc and should be preserved with -E
    local env_preserved=true
    local missing_vars=()

    # Check for your specific environment variables
    if [ -z "$KIWI_ENC_PASSWORD" ]; then
        missing_vars+=("KIWI_ENC_PASSWORD")
        env_preserved=false
    fi

    if [ -z "$DB_IP" ]; then
        missing_vars+=("DB_IP")
        env_preserved=false
    fi

    if [ -z "$GROK_API_KEY" ]; then
        missing_vars+=("GROK_API_KEY")
        env_preserved=false
    fi

    # Display results
    echo "Current user: $(whoami)"
    echo "Original user: $SUDO_USER"
    echo "EUID: $EUID"
    echo "UID: $UID"

    if [ "$env_preserved" = true ]; then
        echo "✅ Environment variables preserved - likely running with sudo -E"
        echo "✅ All required environment variables are set"
    else
        echo "❌ WARNING: Some environment variables missing"
        echo "Missing variables: ${missing_vars[*]}"
        echo "💡 Recommendation: Run with 'sudo -E' to preserve environment variables"
        return 1
    fi

    echo "=============================================="
    return 0
}

# Method 2: Alternative check using HOME variable
check_sudo_e_alternative() {
    echo "ALTERNATIVE SUDO -E CHECK:"
    echo "=============================================="

    if [ "$EUID" -eq 0 ] && [ -n "$SUDO_USER" ]; then
        # Check if HOME points to original user's home
        expected_home="/home/$SUDO_USER"
        if [ "$HOME" = "$expected_home" ] || [ "$HOME" = "/root" ]; then
            echo "✅ Running with sudo"
            # Check if user's environment is preserved
            if [ -n "$KIWI_ENC_PASSWORD" ] || [ -n "$DB_IP" ]; then
                echo "✅ User environment preserved - likely sudo -E"
            else
                echo "❌ User environment not preserved - use sudo -E"
            fi
        fi
    fi
    echo "=============================================="
}

# Method 3: Enhanced error message for your existing check
enhanced_sudo_check() {
    if [ "$EUID" -ne 0 ]; then
        echo "=============================================="
        echo "❌ ERROR: This script must be run with sudo privileges"
        echo ""
        echo "To preserve your environment variables, please run:"
        echo "   sudo -E $0 $*"
        echo ""
        echo "The -E flag preserves your user environment variables"
        echo "which are required for this script to function properly."
        echo "=============================================="
        exit 1
    fi
}

# Method 4: Comprehensive check with recommendations
comprehensive_sudo_check() {
    echo "=============================================="
    echo "COMPREHENSIVE SUDO CHECK:"
    echo "=============================================="

    # Basic sudo check
    if [ "$EUID" -ne 0 ]; then
        echo "❌ Not running as root"
        echo "Please run: sudo -E $0 $*"
        exit 1
    fi

    # Check if it's actually sudo (not just root login)
    if [ -z "$SUDO_USER" ]; then
        echo "⚠️  WARNING: Running as root but SUDO_USER not set"
        echo "This might be a direct root login rather than sudo"
    else
        echo "✅ Running via sudo (original user: $SUDO_USER)"
    fi

    # Check environment preservation
    local critical_vars=("KIWI_ENC_PASSWORD" "DB_IP" "GROK_API_KEY" "MYSQL_ROOT_PASSWORD")
    local missing_count=0

    echo ""
    echo "Checking critical environment variables:"
    for var in "${critical_vars[@]}"; do
        if [ -z "${!var}" ]; then
            echo "❌ $var: NOT SET"
            ((missing_count++))
        else
            echo "✅ $var: SET"
        fi
    done

    if [ $missing_count -gt 0 ]; then
        echo ""
        echo "❌ $missing_count critical environment variables are missing"
        echo "💡 This suggests the script was not run with 'sudo -E'"
        echo ""
        echo "To fix this, run:"
        echo "   sudo -E $0 $*"
        echo ""
        echo "The -E flag tells sudo to preserve your environment variables."
        exit 1
    else
        echo ""
        echo "✅ All critical environment variables are present"
        echo "✅ Script appears to be running with proper sudo -E"
    fi

    echo "=============================================="
}

# Exit immediately if a command exits with a non-zero status
set -e

# Echo all environment variables from .bashrc
echo "=============================================="
echo "ENVIRONMENT VARIABLES:"
echo "=============================================="
echo "KIWI_ENC_PASSWORD: $KIWI_ENC_PASSWORD"
echo "DB_IP: $DB_IP"
echo "GROK_API_KEY: $GROK_API_KEY"
echo "MYSQL_ROOT_PASSWORD: $MYSQL_ROOT_PASSWORD"
echo "REDIS_PASSWORD: $REDIS_PASSWORD"
echo "FASTDFS_HOSTNAME: $FASTDFS_HOSTNAME"
echo "ES_ROOT_PASSWORD: $ES_ROOT_PASSWORD"
echo "ES_USER_NAME: $ES_USER_NAME"
echo "ES_USER_PASSWORD: $ES_USER_PASSWORD"
echo "=============================================="
echo ""

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
  echo "Please run: sudo -E $0 $*"
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
  echo "Moving Dockerfiles, GCP credentials and JARs..."
  echo "Copying Dockerfiles..."
  cp -f ~/microservice-kiwi/kiwi-eureka/Dockerfile ~/docker/kiwi/eureka/
  cp -f ~/microservice-kiwi/kiwi-config/Dockerfile ~/docker/kiwi/config/
  cp -f ~/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
  cp -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile ~/docker/kiwi/word/biz
  cp -f ~/gcp-credentials.json ~/docker/kiwi/word/biz
  cp -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile ~/docker/kiwi/word/crawler
  cp -f ~/gcp-credentials.json ~/docker/kiwi/word/crawler
  cp -f ~/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
  cp -f ~/gcp-credentials.json ~/docker/kiwi/crawler
  cp -f ~/microservice-kiwi/kiwi-auth/Dockerfile ~/docker/kiwi/auth/
  cp -f ~/microservice-kiwi/kiwi-gateway/Dockerfile ~/docker/kiwi/gate/
  cp -f ~/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/biz/Dockerfile ~/docker/kiwi/ai/biz
  cp -f ~/gcp-credentials.json ~/docker/kiwi/ai/biz
  cp -f ~/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/batch/Dockerfile ~/docker/kiwi/ai/batch
  cp -f ~/gcp-credentials.json ~/docker/kiwi/ai/batch

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