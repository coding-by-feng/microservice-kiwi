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

# Function to show help
show_help() {
  echo "Usage: $0 [MODE] [OPTIONS]"
  echo ""
  echo "Available modes:"
  echo "  -mode=sg      Skip git operations (stash and pull)"
  echo "  -mode=sm      Skip maven build operation"
  echo "  -mode=sbd     Skip Dockerfile building operation (copying Dockerfiles and JARs)"
  echo "  -mode=sa      Skip all build operations - FAST DEPLOY MODE"
  echo "                (Only stop containers → remove containers → start containers)"
  echo ""
  echo "Available options:"
  echo "  -c            Enable autoCheckService after deployment"
  echo "  -help         Show this help message"
  echo ""
  echo "If no mode is specified, all operations will be executed."
  echo ""
  echo "Examples:"
  echo "  sudo -E $0                # Full deployment (git + maven + docker build + deploy)"
  echo "  sudo -E $0 -mode=sg       # Skip only git operations"
  echo "  sudo -E $0 -mode=sm       # Skip only maven build"
  echo "  sudo -E $0 -mode=sbd      # Skip only Dockerfile building"
  echo "  sudo -E $0 -mode=sa       # FAST DEPLOY: Skip all builds, only redeploy containers"
  echo "  sudo -E $0 -c             # Full deployment with autoCheckService"
  echo "  sudo -E $0 -mode=sa -c    # Fast deploy with autoCheckService"
}

# Initialize variables
MODE=""
ENABLE_AUTO_CHECK=false
SKIP_GIT=false
SKIP_MAVEN=false
SKIP_DOCKER_BUILD=false
FAST_DEPLOY_MODE=false

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
  rm -rf "$CURRENT_DIR/docker/kiwi/eureka/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/config/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/upms/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/auth/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/gate/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/word/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/word/crawlerTmp/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/word/bizTmp/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/crawler/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/ai/logs/"* 2>/dev/null || true
  rm -rf "$CURRENT_DIR/docker/kiwi/ai/tmp/"* 2>/dev/null || true

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

    echo "🔨 Running maven build..."
    mvn clean install -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
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
    echo "📄 Copying Dockerfiles..."
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-eureka/Dockerfile" "$CURRENT_DIR/docker/kiwi/eureka/"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-config/Dockerfile" "$CURRENT_DIR/docker/kiwi/config/"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/upms/"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/biz"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/crawler"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/crawler/"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-auth/Dockerfile" "$CURRENT_DIR/docker/kiwi/auth/"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-gateway/Dockerfile" "$CURRENT_DIR/docker/kiwi/gate/"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/biz"
    cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/batch/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/batch"

    echo "📦 Copying JAR files..."
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar" "$CURRENT_DIR/docker/kiwi/eureka/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar" "$CURRENT_DIR/docker/kiwi/config/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/upms/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar" "$CURRENT_DIR/docker/kiwi/auth/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar" "$CURRENT_DIR/docker/kiwi/gate/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/word/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar" "$CURRENT_DIR/docker/kiwi/crawler/"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/ai/biz"
    cp -f "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar" "$CURRENT_DIR/docker/kiwi/ai/batch"

    echo "📄 Copying GCP credential json..."
    cp -f "$CURRENT_DIR/gcp-credentials.json" "$CURRENT_DIR/docker/kiwi/word/biz/gcp-credentials.json"
    cp -f "$CURRENT_DIR/gcp-credentials.json" "$CURRENT_DIR/docker/kiwi/word/crawler/gcp-credentials.json"

  else
    echo "⏭️  Dockerfile building skipped"
  fi
fi

# Core deployment operations (always executed)
echo "=============================================="
echo "🚀 STARTING CONTAINER DEPLOYMENT:"
echo "=============================================="

echo "🛑 Stopping all services..."
"$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" "$MODE"

echo "🧹 Cleaning up Docker resources..."
echo "🗑️  Removing dangling images..."
docker image prune -f >/dev/null 2>&1 || true
echo "🌐 Removing unused networks..."
docker network prune -f >/dev/null 2>&1 || true
echo "📦 Removing unused volumes..."
docker volume prune -f >/dev/null 2>&1 || true
echo "✅ Docker cleanup completed"

echo "🚀 Starting auto deployment..."
"$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh" "$MODE"

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
  echo "🎉 FULL DEPLOYMENT COMPLETED SUCCESSFULLY!"
fi
echo "=============================================="