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

# Get current working directory
CURRENT_DIR="$(pwd)"

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

cd "$CURRENT_DIR/microservice-kiwi/" || { echo "CRITICAL ERROR: Failed to change directory to $CURRENT_DIR/microservice-kiwi/"; exit 1; }

# Define available microservices
declare -A MICROSERVICES=(
    ["eureka"]="kiwi-eureka"
    ["config"]="kiwi-config"
    ["upms"]="kiwi-upms-biz"
    ["auth"]="kiwi-auth"
    ["gate"]="kiwi-gateway"
    ["word"]="kiwi-word-biz"
    ["crawler"]="kiwi-word-crawler"
    ["ai"]="kiwi-ai-biz"
)

# Function to show help
show_help() {
  echo "Usage: $0 [MODE] [OPTIONS]"
  echo ""
  echo "Available modes:"
  echo "  -mode=sg      Skip git operations (stash and pull)"
  echo "  -mode=sm      Skip maven build operation"
  echo "  -mode=sgm     Skip git operations AND maven build"
  echo "  -mode=sbd     Skip Dockerfile building operation (copying Dockerfiles and JARs)"
  echo "  -mode=sa      Skip all build operations - FAST DEPLOY MODE"
  echo "                (Only stop containers → remove containers → start containers)"
  echo ""
  echo "Available options:"
  echo "  -c            Enable autoCheckService after deployment"
  echo "  -s=SERVICE    Build/deploy specific service(s) only"
  echo "                Available services: eureka, config, upms, auth, gate, word, crawler, ai"
  echo "                Multiple services: -s=eureka,config,upms"
  echo "                All services: -s=all (default)"
  echo "  -help         Show this help message"
  echo ""
  echo "If no mode is specified, all operations will be executed."
  echo ""
  echo "Examples:"
  echo "  sudo -E $0                        # Full deployment (all services)"
  echo "  sudo -E $0 -mode=sg               # Skip only git operations"
  echo "  sudo -E $0 -mode=sm               # Skip only maven build"
  echo "  sudo -E $0 -mode=sgm              # Skip git AND maven build"
  echo "  sudo -E $0 -mode=sbd              # Skip only Dockerfile building"
  echo "  sudo -E $0 -mode=sa               # FAST DEPLOY: Skip all builds"
  echo "  sudo -E $0 -s=eureka,config       # Build only eureka and config services"
  echo "  sudo -E $0 -mode=sgm -s=auth      # Skip git+maven, build only auth service"
  echo "  sudo -E $0 -mode=sa -c            # Fast deploy with autoCheckService"
}

# Initialize variables
MODE=""
ENABLE_AUTO_CHECK=false
SKIP_GIT=false
SKIP_MAVEN=false
SKIP_DOCKER_BUILD=false
FAST_DEPLOY_MODE=false
SELECTED_SERVICES=""
BUILD_ALL_SERVICES=true

# Process all arguments
for arg in "$@"; do
  case "$arg" in
    -mode=sg)
      MODE="$arg"
      SKIP_GIT=true
      echo "⏭️  Skipping git stash and pull operations"
      ;;
    -mode=sm)
      MODE="$arg"
      SKIP_MAVEN=true
      echo "⏭️  Skipping maven build operation"
      ;;
    -mode=sgm)
      MODE="$arg"
      SKIP_GIT=true
      SKIP_MAVEN=true
      echo "⏭️  Skipping git operations AND maven build"
      ;;
    -mode=sbd)
      MODE="$arg"
      SKIP_DOCKER_BUILD=true
      echo "⏭️  Skipping Dockerfile building operation"
      ;;
    -mode=sa)
      MODE="$arg"
      SKIP_GIT=true
      SKIP_MAVEN=true
      SKIP_DOCKER_BUILD=true
      FAST_DEPLOY_MODE=true
      echo "🚀 FAST DEPLOY MODE: Skipping all build operations"
      echo "   ⏭️  Git operations: SKIPPED"
      echo "   ⏭️  Maven build: SKIPPED"
      echo "   ⏭️  Docker building: SKIPPED"
      echo "   ✅ Will only: Stop → Remove → Start containers"
      ;;
    -s=*)
      SELECTED_SERVICES="${arg#-s=}"
      if [ "$SELECTED_SERVICES" != "all" ]; then
        BUILD_ALL_SERVICES=false
        echo "🎯 Selected services: $SELECTED_SERVICES"
      fi
      ;;
    -c)
      ENABLE_AUTO_CHECK=true
      echo "🔄 AutoCheckService will be enabled after deployment"
      ;;
    -help|--help|-h)
      show_help
      exit 0
      ;;
    *)
      if [ -n "$arg" ]; then
        echo "❌ Invalid parameter: $arg"
        echo "Use -help to see available options"
        exit 1
      fi
      ;;
  esac
done

# Validate selected services
if [ "$BUILD_ALL_SERVICES" = false ]; then
  IFS=',' read -ra SERVICE_ARRAY <<< "$SELECTED_SERVICES"
  for service in "${SERVICE_ARRAY[@]}"; do
    if [[ ! " ${!MICROSERVICES[@]} " =~ " ${service} " ]]; then
      echo "❌ Invalid service: $service"
      echo "Available services: ${!MICROSERVICES[@]}"
      exit 1
    fi
  done
fi

# Function to check if a service should be built
should_build_service() {
  local service=$1
  if [ "$BUILD_ALL_SERVICES" = true ]; then
    return 0
  fi
  if [[ ",$SELECTED_SERVICES," =~ ",$service," ]]; then
    return 0
  fi
  return 1
}

# Display final configuration
echo "=============================================="
echo "DEPLOYMENT CONFIGURATION:"
echo "=============================================="
if [ "$FAST_DEPLOY_MODE" = true ]; then
  echo "🚀 MODE: FAST DEPLOY (Skip All Builds)"
  echo "   📋 Operations: Stop Containers → Remove Containers → Start Containers"
  echo "   ⚡ Estimated time: ~2-3 minutes"
elif [ "$SKIP_GIT" = true ] || [ "$SKIP_MAVEN" = true ] || [ "$SKIP_DOCKER_BUILD" = true ]; then
  echo "⚙️  MODE: PARTIAL BUILD"
  echo "   Git operations: $([ "$SKIP_GIT" = true ] && echo "SKIPPED" || echo "ENABLED")"
  echo "   Maven build: $([ "$SKIP_MAVEN" = true ] && echo "SKIPPED" || echo "ENABLED")"
  echo "   Docker building: $([ "$SKIP_DOCKER_BUILD" = true ] && echo "SKIPPED" || echo "ENABLED")"
else
  echo "🔨 MODE: FULL BUILD"
  echo "   📋 Operations: Git Pull → Maven Build → Docker Build → Deploy"
  echo "   ⏱️  Estimated time: ~10-15 minutes"
fi

if [ "$BUILD_ALL_SERVICES" = false ]; then
  echo "🎯 Services to build: $SELECTED_SERVICES"
else
  echo "🎯 Services to build: ALL"
fi

if [ "$ENABLE_AUTO_CHECK" = true ]; then
  echo "🔄 AutoCheckService: ENABLED"
else
  echo "🔄 AutoCheckService: DISABLED (use -c to enable)"
fi
echo "=============================================="
echo ""

# Fast deploy mode optimization - skip unnecessary operations
if [ "$FAST_DEPLOY_MODE" = true ]; then
  echo "🚀 FAST DEPLOY MODE ACTIVATED"
  echo "=============================================="
  echo "⏭️  Skipping all build operations for maximum speed..."
  echo "📦 Using existing Docker images and configurations"
  echo "⚡ Proceeding directly to container deployment..."
  echo ""
else
  # Git operations
  if [ "$SKIP_GIT" = false ]; then
    echo "📥 Git pulling..."
    echo "📦 Stashing local changes..."
    git stash
    echo "⬇️  Pulling latest changes..."
    git pull
  else
    echo "⏭️  Git operations skipped"
  fi

  # Set execute permissions
  echo "🔐 Setting execute permissions for scripts..."
  chmod 777 "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/"*.sh
  chmod 777 "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/kiwi-ui/"*.sh

  # Clean log directories efficiently (only if not in fast deploy mode)
  echo "🧹 Cleaning log directories..."
  if should_build_service "eureka"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/eureka/logs/"* 2>/dev/null || true
  fi
  if should_build_service "config"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/config/logs/"* 2>/dev/null || true
  fi
  if should_build_service "upms"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/upms/logs/"* 2>/dev/null || true
  fi
  if should_build_service "auth"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/auth/logs/"* 2>/dev/null || true
  fi
  if should_build_service "gate"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/gate/logs/"* 2>/dev/null || true
  fi
  if should_build_service "word"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/word/logs/"* 2>/dev/null || true
    rm -rf "$CURRENT_DIR/docker/kiwi/word/crawlerTmp/"* 2>/dev/null || true
    rm -rf "$CURRENT_DIR/docker/kiwi/word/bizTmp/"* 2>/dev/null || true
  fi
  if should_build_service "crawler"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/crawler/logs/"* 2>/dev/null || true
  fi
  if should_build_service "ai"; then
    rm -rf "$CURRENT_DIR/docker/kiwi/ai/logs/"* 2>/dev/null || true
    rm -rf "$CURRENT_DIR/docker/kiwi/ai/tmp/"* 2>/dev/null || true
  fi

  # Maven build
  if [ "$SKIP_MAVEN" = false ]; then
    # Get the original user's home directory
    if [ -n "$SUDO_USER" ]; then
      ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
    else
      ORIGINAL_HOME="$HOME"
    fi

    echo "📚 Installing VoiceRSS TTS library..."
    echo "📂 Using Maven repository: $ORIGINAL_HOME/.m2"
    cd "$CURRENT_DIR/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
    mvn install:install-file \
        -Dfile=voicerss_tts.jar \
        -DgroupId=voicerss \
        -DartifactId=tts \
        -Dversion=2.0 \
        -Dpackaging=jar \
        -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
    cd "$CURRENT_DIR/microservice-kiwi/"

    if [ "$BUILD_ALL_SERVICES" = true ]; then
      echo "🔨 Running maven build for all services..."
      mvn clean install -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
    else
      echo "🔨 Running selective maven build..."
      # Build common modules first (always needed)
      echo "📦 Building common modules..."
      mvn clean install -pl kiwi-common -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"

      # Build selected services
      IFS=',' read -ra SERVICE_ARRAY <<< "$SELECTED_SERVICES"
      for service in "${SERVICE_ARRAY[@]}"; do
        module="${MICROSERVICES[$service]}"
        echo "📦 Building $service ($module)..."

        # Handle special cases for nested modules
        case "$service" in
          "upms")
            mvn clean install -pl kiwi-upms/kiwi-upms-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          "word")
            mvn clean install -pl kiwi-word/kiwi-word-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          "crawler")
            mvn clean install -pl kiwi-word/kiwi-word-crawler -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          "ai")
            mvn clean install -pl kiwi-ai/kiwi-ai-biz -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
          *)
            mvn clean install -pl $module -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
            ;;
        esac
      done
    fi
  else
    echo "⏭️  Maven build skipped"
  fi

  # Move Dockerfiles and JARs efficiently
  if [ "$SKIP_DOCKER_BUILD" = false ]; then
    # Get the original user's home directory for JAR files
    if [ -n "$SUDO_USER" ]; then
      ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
    else
      ORIGINAL_HOME="$HOME"
    fi

    echo "📋 Moving Dockerfiles, GCP credentials and JARs..."
    echo "📂 Using Maven repository: $ORIGINAL_HOME/.m2"

    if should_build_service "eureka"; then
      echo "📄 Copying eureka files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-eureka/Dockerfile" "$CURRENT_DIR/docker/kiwi/eureka/"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar" "$CURRENT_DIR/docker/kiwi/eureka/"
    fi

    if should_build_service "config"; then
      echo "📄 Copying config files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-config/Dockerfile" "$CURRENT_DIR/docker/kiwi/config/"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar" "$CURRENT_DIR/docker/kiwi/config/"
    fi

    if should_build_service "upms"; then
      echo "📄 Copying upms files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/upms/"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/upms/"
    fi

    if should_build_service "auth"; then
      echo "📄 Copying auth files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-auth/Dockerfile" "$CURRENT_DIR/docker/kiwi/auth/"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar" "$CURRENT_DIR/docker/kiwi/auth/"
    fi

    if should_build_service "gate"; then
      echo "📄 Copying gateway files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-gateway/Dockerfile" "$CURRENT_DIR/docker/kiwi/gate/"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar" "$CURRENT_DIR/docker/kiwi/gate/"
    fi

    if should_build_service "word"; then
      echo "📄 Copying word service files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/biz"
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/crawler"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/word/"
      cp -f "$CURRENT_DIR/gcp-credentials.json" "$CURRENT_DIR/docker/kiwi/word/bizTmp"
    fi

    if should_build_service "crawler"; then
      echo "📄 Copying crawler files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/crawler/"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar" "$CURRENT_DIR/docker/kiwi/crawler/"
    fi

    if should_build_service "ai"; then
      echo "📄 Copying AI service files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/biz"
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/batch/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/batch"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/ai/biz"
      cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/ai/batch"
    fi

  else
    echo "⏭️  Dockerfile building skipped"
  fi
fi

# Define container names mapping
declare -A CONTAINER_NAMES=(
    ["eureka"]="kiwi-eureka"
    ["config"]="kiwi-config"
    ["upms"]="kiwi-upms-biz"
    ["auth"]="kiwi-auth"
    ["gate"]="kiwi-gateway"
    ["word"]="kiwi-word-biz"
    ["crawler"]="kiwi-word-crawler"
    ["ai"]="kiwi-ai-biz"
)

# Function to stop specific containers
stop_selected_containers() {
    local services="$1"
    IFS=',' read -ra SERVICE_ARRAY <<< "$services"

    for service in "${SERVICE_ARRAY[@]}"; do
        container_name="${CONTAINER_NAMES[$service]}"
        echo "🛑 Stopping $service container ($container_name)..."

        # Check if container exists and is running
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            docker stop "$container_name" 2>/dev/null || true
            echo "   ✅ Stopped $container_name"
        else
            echo "   ℹ️  Container $container_name not found or not running"
        fi
    done
}

# Function to remove specific containers
remove_selected_containers() {
    local services="$1"
    IFS=',' read -ra SERVICE_ARRAY <<< "$services"

    for service in "${SERVICE_ARRAY[@]}"; do
        container_name="${CONTAINER_NAMES[$service]}"
        echo "🗑️  Removing $service container ($container_name)..."

        # Check if container exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            docker rm -f "$container_name" 2>/dev/null || true
            echo "   ✅ Removed $container_name"
        else
            echo "   ℹ️  Container $container_name already removed"
        fi
    done
}

# Function to deploy specific service with Docker build and run
deploy_single_service() {
    local service=$1
    local DOCKER_DIR="$CURRENT_DIR/docker/kiwi"

    # Ensure Docker network exists
    docker network create kiwi-network 2>/dev/null || true

    case "$service" in
        "eureka")
            echo "📡 Building Eureka Service Discovery Server..."
            if [ ! -d "$DOCKER_DIR/eureka" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/eureka"
                return 1
            fi
            cd "$DOCKER_DIR/eureka"
            docker build -t kiwi-eureka:latest .
            docker run -d --name kiwi-eureka \
                --network kiwi-network \
                -p 18000:18000 \
                -v "$DOCKER_DIR/eureka/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-eureka:latest
            echo "✅ Eureka deployed successfully"
            ;;

        "config")
            echo "⚙️ Building Config Server..."
            if [ ! -d "$DOCKER_DIR/config" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/config"
                return 1
            fi
            cd "$DOCKER_DIR/config"
            docker build -t kiwi-config:latest .
            docker run -d --name kiwi-config \
                --network kiwi-network \
                -p 18001:18001 \
                -v "$DOCKER_DIR/config/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-config:latest
            echo "✅ Config Server deployed successfully"
            ;;

        "upms")
            echo "👤 Building UPMS Service..."
            if [ ! -d "$DOCKER_DIR/upms" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/upms"
                return 1
            fi
            cd "$DOCKER_DIR/upms"
            docker build -t kiwi-upms:latest .
            docker run -d --name kiwi-upms-biz \
                --network kiwi-network \
                -p 18002:18002 \
                -v "$DOCKER_DIR/upms/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-upms:latest
            echo "✅ UPMS deployed successfully"
            ;;

        "auth")
            echo "🔐 Building Auth Service..."
            if [ ! -d "$DOCKER_DIR/auth" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/auth"
                return 1
            fi
            cd "$DOCKER_DIR/auth"
            docker build -t kiwi-auth:latest .
            docker run -d --name kiwi-auth \
                --network kiwi-network \
                -p 18003:18003 \
                -v "$DOCKER_DIR/auth/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-auth:latest
            echo "✅ Auth Service deployed successfully"
            ;;

        "gate")
            echo "🌐 Building API Gateway..."
            if [ ! -d "$DOCKER_DIR/gate" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/gate"
                return 1
            fi
            cd "$DOCKER_DIR/gate"
            docker build -t kiwi-gateway:latest .
            docker run -d --name kiwi-gateway \
                --network kiwi-network \
                -p 18004:18004 \
                -v "$DOCKER_DIR/gate/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-gateway:latest
            echo "✅ API Gateway deployed successfully"
            ;;

        "word")
            echo "📚 Building Word Service..."
            if [ ! -d "$DOCKER_DIR/word/biz" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/word/biz"
                return 1
            fi
            cd "$DOCKER_DIR/word/biz"
            docker build -t kiwi-word-biz:latest .
            docker run -d --name kiwi-word-biz \
                --network kiwi-network \
                -p 18010:18010 \
                -v "$DOCKER_DIR/word/logs:/logs" \
                -v "$DOCKER_DIR/word/bizTmp:/bizTmp" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-word-biz:latest
            echo "✅ Word Service deployed successfully"
            ;;

        "crawler")
            echo "🕷️ Building Crawler Service..."
            if [ ! -d "$DOCKER_DIR/crawler" ]; then
                echo "❌ Directory not found: $DOCKER_DIR/crawler"
                return 1
            fi
            cd "$DOCKER_DIR/crawler"
            docker build -t kiwi-word-crawler:latest .
            docker run -d --name kiwi-word-crawler \
                --network kiwi-network \
                -p 18011:18011 \
                -v "$DOCKER_DIR/crawler/logs:/logs" \
                -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                kiwi-word-crawler:latest
            echo "✅ Crawler Service deployed successfully"
            ;;

        "ai")
            echo "🤖 Building AI Service..."

            # Check if AI directories exist
            if [ ! -d "$DOCKER_DIR/ai" ]; then
                echo "❌ AI directory not found: $DOCKER_DIR/ai"
                echo "Creating AI directory structure..."
                mkdir -p "$DOCKER_DIR/ai/biz" "$DOCKER_DIR/ai/batch" "$DOCKER_DIR/ai/logs" "$DOCKER_DIR/ai/tmp"
            fi

            # Build AI Business Logic
            if [ -d "$DOCKER_DIR/ai/biz" ] && [ -f "$DOCKER_DIR/ai/biz/Dockerfile" ]; then
                echo "   → Building AI Business Logic component..."
                cd "$DOCKER_DIR/ai/biz"
                docker build -t kiwi-ai-biz:latest .

                echo "   → Starting AI Business Logic container..."
                docker run -d --name kiwi-ai-biz \
                    --network kiwi-network \
                    -p 18015:18015 \
                    -v "$DOCKER_DIR/ai/logs:/logs" \
                    -v "$DOCKER_DIR/ai/tmp:/ai-tmp" \
                    -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                    -e TZ=Pacific/Auckland \
                    kiwi-ai-biz:latest
            else
                echo "⚠️  AI Business Logic Dockerfile not found at $DOCKER_DIR/ai/biz/Dockerfile"
            fi

            # Build AI Batch
            if [ -d "$DOCKER_DIR/ai/batch" ] && [ -f "$DOCKER_DIR/ai/batch/Dockerfile" ]; then
                echo "   → Building AI Batch component..."
                cd "$DOCKER_DIR/ai/batch"
                docker build -t kiwi-ai-batch:latest .

                echo "   → Starting AI Batch container..."
                docker run -d --name kiwi-ai-batch \
                    --network kiwi-network \
                    -p 18016:18016 \
                    -v "$DOCKER_DIR/ai/logs:/logs" \
                    -v "$DOCKER_DIR/ai/tmp:/ai-tmp" \
                    -e EUREKA_SERVER=http://kiwi-eureka:18000/eureka/ \
                    -e TZ=Pacific/Auckland \
                    kiwi-ai-batch:latest
            else
                echo "⚠️  AI Batch Dockerfile not found at $DOCKER_DIR/ai/batch/Dockerfile"
            fi

            echo "✅ AI Service deployment completed"
            ;;

        *)
            echo "❌ Unknown service: $service"
            return 1
            ;;
    esac

    # Return to original directory
    cd "$CURRENT_DIR/microservice-kiwi/"
}

# Function to perform selective deployment
selective_deployment() {
    local services="$1"

    echo "=============================================="
    echo "🎯 SELECTIVE SERVICE DEPLOYMENT"
    echo "=============================================="
    echo "Services to deploy: $services"
    echo "Working directory: $CURRENT_DIR"
    echo "=============================================="

    # Deploy each selected service
    IFS=',' read -ra SERVICE_ARRAY <<< "$services"
    for service in "${SERVICE_ARRAY[@]}"; do
        echo ""
        deploy_single_service "$service"
    done

    echo ""
    echo "=============================================="
    echo "🎉 SELECTIVE DEPLOYMENT COMPLETED"
    echo "Deployed services: $services"
    echo "=============================================="

    # Show running containers
    echo ""
    echo "Running containers:"
    docker ps --filter "name=kiwi-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

# Core deployment operations (always executed)
echo "=============================================="
echo "🚀 STARTING CONTAINER DEPLOYMENT:"
echo "=============================================="

# Stop services (either selected or all)
if [ "$BUILD_ALL_SERVICES" = true ]; then
  echo "🛑 Stopping all services..."
  "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" "$MODE"
else
  echo "🛑 Stopping selected services: $SELECTED_SERVICES"
  stop_selected_containers "$SELECTED_SERVICES"
fi

# Remove containers
if [ "$BUILD_ALL_SERVICES" = true ]; then
  echo "🗑️  Removing all containers..."
  # Call existing script or implement full removal
  docker ps -a | grep "kiwi-" | awk '{print $1}' | xargs -r docker rm -f 2>/dev/null || true
else
  echo "🗑️  Removing selected containers: $SELECTED_SERVICES"
  remove_selected_containers "$SELECTED_SERVICES"
fi

# Clean up Docker resources (only if building all services)
if [ "$BUILD_ALL_SERVICES" = true ]; then
  echo "🧹 Cleaning up Docker resources..."
  echo "🗑️  Removing dangling images..."
  docker image prune -f >/dev/null 2>&1 || true
  echo "🌐 Removing unused networks..."
  docker network prune -f >/dev/null 2>&1 || true
  echo "📦 Removing unused volumes..."
  docker volume prune -f >/dev/null 2>&1 || true
  echo "✅ Docker cleanup completed"
fi

# Build Docker images and deploy
if [ "$BUILD_ALL_SERVICES" = true ]; then
  echo "🚀 Starting auto deployment for all services..."
  "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh" "$MODE"
else
  # Selective deployment
  selective_deployment "$SELECTED_SERVICES"
fi

# AutoCheck Service Logic - Added at the end as requested
echo "=============================================="
echo "AUTO CHECK SERVICE CONFIGURATION:"
echo "=============================================="

if [ "$ENABLE_AUTO_CHECK" = true ]; then
  if ! pgrep -f "autoCheckService.sh" >/dev/null; then
    echo "🔄 Starting autoCheckService..."
    nohup "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh" >"$CURRENT_DIR/autoCheck.log" 2>&1 &
    echo "✅ AutoCheckService started successfully"
    echo "🔄 AutoCheckService is now running in the background"
    echo "📄 Log file: $CURRENT_DIR/autoCheck.log"
  else
    echo "ℹ️  AutoCheckService is already running"
    echo "✅ AutoCheckService status: ACTIVE"
  fi
else
  echo "ℹ️  You are NOT using auto check mode this time"
  echo "❌ AutoCheckService will not be started"
  echo "💡 To enable AutoCheckService, add -c parameter to your command"
  echo "   Example: sudo -E $0 -c"
  if [ "$FAST_DEPLOY_MODE" = true ]; then
    echo "   Fast deploy with monitoring: sudo -E $0 -mode=sa -c"
  fi
fi

echo "=============================================="
if [ "$FAST_DEPLOY_MODE" = true ]; then
  echo "🚀 FAST DEPLOYMENT COMPLETED SUCCESSFULLY!"
  echo "⚡ Total time saved by skipping build operations"
else
  if [ "$BUILD_ALL_SERVICES" = false ]; then
    echo "🎉 SELECTIVE DEPLOYMENT COMPLETED SUCCESSFULLY!"
    echo "✅ Deployed services: $SELECTED_SERVICES"
  else
    echo "🎉 FULL DEPLOYMENT COMPLETED SUCCESSFULLY!"
  fi
fi
echo "=============================================="