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
set -o pipefail
set -E  # inherit ERR in functions/subshells

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
  local exit_code=$?   # capture immediately
  echo "=============================================="
  echo "ERROR: Script failed at line $line_number"
  echo "Failed command: $command"
  echo "Exit code: $exit_code"
  # If autoDeploy log exists show tail for quick context
  if [ -f "$CURRENT_DIR/autoDeploy.log" ]; then
     echo "--- Last 40 lines of autoDeploy.log ---"
     tail -n 40 "$CURRENT_DIR/autoDeploy.log" || true
     echo "--------------------------------------"
  fi
  echo "TIP: Re-run with: bash -x $0 (plus your params) for more tracing."
  echo "=============================================="
  exit "$exit_code"
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
  echo "  -mode=obm     Only build with Maven, then copy built JARs to ~/built_jar"
  echo "  -mode=obmas   Only build with Maven, then send built JARs to remote via scp"
  echo "  -mode=ouej    Only use existing jars from ~/built_jar (skip git and maven)"
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
  echo "  sudo -E $0 -mode=obm              # Only Maven build and copy jars to ~/built_jar"
  echo "  sudo -E $0 -mode=obmas            # Maven build then scp jars to remote"
  echo "  sudo -E $0 -mode=ouej             # Use jars from ~/built_jar and deploy"
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
ONLY_BUILD_MAVEN=false
SEND_AFTER_BUILD=false
USE_EXISTING_JARS_ONLY=false

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
    -mode=obm)
      MODE="$arg"
      ONLY_BUILD_MAVEN=true
      echo "🧱 OBM MODE: Only Maven build and copy jars to ~/built_jar"
      ;;
    -mode=obmas)
      MODE="$arg"
      ONLY_BUILD_MAVEN=true
      SEND_AFTER_BUILD=true
      echo "📦➡️  OBMAS MODE: Maven build, then send jars to remote via scp"
      ;;
    -mode=ouej)
      MODE="$arg"
      SKIP_GIT=true
      SKIP_MAVEN=true
      USE_EXISTING_JARS_ONLY=true
      echo "📦 OUEJ MODE: Using jars from ~/built_jar (skip git and maven)"
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

# === New: helper to copy built jars to ~/built_jar (under original user's home) ===
copy_built_jars_to_dir() {
  local original_home="$1"
  local dest_dir="${original_home}/built_jar"
  mkdir -p "$dest_dir"

  # Map service -> expected jar path in local maven repo
  declare -A JAR_MAP=(
    ["eureka"]="$original_home/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar"
    ["config"]="$original_home/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar"
    ["upms"]="$original_home/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar"
    ["auth"]="$original_home/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar"
    ["gate"]="$original_home/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar"
    ["word"]="$original_home/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar"
    ["crawler"]="$original_home/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar"
    ["ai"]="$original_home/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar"
  )

  local copied=0
  if [ "$BUILD_ALL_SERVICES" = true ]; then
    services=(eureka config upms auth gate word crawler ai)
  else
    IFS=',' read -ra services <<< "$SELECTED_SERVICES"
  fi

  echo "📦 Copying built jars to: $dest_dir"
  for svc in "${services[@]}"; do
    jar_path="${JAR_MAP[$svc]}"
    if [ -n "$jar_path" ] && [ -f "$jar_path" ]; then
      cp -f "$jar_path" "$dest_dir/"
      echo "   ✅ $svc -> $(basename "$jar_path")"
      ((copied++))
    else
      echo "   ⚠️  $svc jar not found in local repo, expected: ${jar_path:-<unknown>}"
    fi
  done

  # Ensure ownership for original user, if available
  if [ -n "$SUDO_USER" ]; then
    chown -R "$SUDO_USER":"$SUDO_USER" "$dest_dir" || true
  fi

  if [ "$copied" -eq 0 ]; then
    echo "❌ No jars were copied. Did the Maven build succeed?"
    return 1
  fi

  echo "✅ Copied $copied jar(s) to $dest_dir"
  return 0
}

# === New: send jars to remote host via scp (first-run prompts saved) ===
send_jars_remote() {
  local original_home="$1"
  local src_dir="${original_home}/built_jar"
  local cfg_dir="${original_home}/.kiwi"
  local cfg_file="${cfg_dir}/obmas.conf"

  mkdir -p "$cfg_dir"
  if [ ! -f "$cfg_file" ]; then
    echo "📝 First-time OBMAS setup:"
    read -p "Remote host (hostname or IP): " REMOTE_HOST
    read -p "Remote user: " REMOTE_USER
    read -p "Remote directory (default: ~/kiwi_jars): " REMOTE_DIR
    REMOTE_DIR=${REMOTE_DIR:-"~/kiwi_jars"}
    read -s -p "Remote password: " REMOTE_PASS
    echo
    # Save config (password in plaintext; file will be 600)
    umask 077
    cat > "$cfg_file" <<EOF
REMOTE_HOST="$REMOTE_HOST"
REMOTE_USER="$REMOTE_USER"
REMOTE_DIR="$REMOTE_DIR"
REMOTE_PASS="$REMOTE_PASS"
EOF
    # Restore default umask
    umask 022
    if [ -n "$SUDO_USER" ]; then
      chown "$SUDO_USER":"$SUDO_USER" "$cfg_file" || true
    fi
    echo "✅ Saved OBMAS configuration to $cfg_file"
  fi

  # shellcheck disable=SC1090
  source "$cfg_file"

  if [ ! -d "$src_dir" ] || [ -z "$(ls -1 "$src_dir"/*.jar 2>/dev/null)" ]; then
    echo "❌ No jars found to send in: $src_dir"
    return 1
  fi

  echo "🌐 Sending jars to $REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR ..."
  if command -v sshpass >/dev/null 2>&1; then
    sshpass -p "$REMOTE_PASS" ssh -o StrictHostKeyChecking=no "$REMOTE_USER@$REMOTE_HOST" "mkdir -p \"$REMOTE_DIR\""
    sshpass -p "$REMOTE_PASS" scp -o StrictHostKeyChecking=no "$src_dir"/*.jar "$REMOTE_USER@$REMOTE_HOST":"$REMOTE_DIR"/
  else
    echo "ℹ️  'sshpass' not found; using interactive ssh/scp (you may be prompted for password)."
    ssh -o StrictHostKeyChecking=no "$REMOTE_USER@$REMOTE_HOST" "mkdir -p \"$REMOTE_DIR\""
    scp -o StrictHostKeyChecking=no "$src_dir"/*.jar "$REMOTE_USER@$REMOTE_HOST":"$REMOTE_DIR"/
  fi

  echo "✅ Jars sent to $REMOTE_USER@$REMOTE_HOST:$REMOTE_DIR"
}

# === New: OBM/OBMAS short-circuit workflow ===
if [ "$ONLY_BUILD_MAVEN" = true ]; then
  echo "=============================================="
  echo "OBM/OBMAS CONFIGURATION:"
  echo "=============================================="
  if [ -n "$SUDO_USER" ]; then
    ORIGINAL_HOME=$(eval echo "~$SUDO_USER")
  else
    ORIGINAL_HOME="$HOME"
  fi
  echo "Mode: $MODE"
  echo "Selected services: $([ "$BUILD_ALL_SERVICES" = true ] && echo "ALL" || echo "$SELECTED_SERVICES")"
  echo "Maven local repo: $ORIGINAL_HOME/.m2/repository"
  echo "Jar output dir:   $ORIGINAL_HOME/built_jar"
  echo "=============================================="

  # Ensure required TTS lib in local repo (same as existing logic)
  echo "📚 Installing VoiceRSS TTS library into local Maven repo..."
  cd "$CURRENT_DIR/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
  mvn install:install-file \
      -Dfile=voicerss_tts.jar \
      -DgroupId=voicerss \
      -DartifactId=tts \
      -Dversion=2.0 \
      -Dpackaging=jar \
      -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
  cd "$CURRENT_DIR/microservice-kiwi/"

  # Maven build (all or selective) — mirrors existing logic
  if [ "$BUILD_ALL_SERVICES" = true ]; then
    echo "🔨 Running maven build for all services..."
    mvn clean install -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
  else
    echo "🔨 Running selective maven build..."
    echo "📦 Building common modules..."
    mvn clean install -pl kiwi-common -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"

    IFS=',' read -ra SERVICE_ARRAY <<< "$SELECTED_SERVICES"
    for service in "${SERVICE_ARRAY[@]}"; do
      echo "📦 Building $service ..."
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
          module="${MICROSERVICES[$service]}"
          mvn clean install -pl "$module" -am -Dmaven.test.skip=true -B -Dmaven.repo.local="$ORIGINAL_HOME/.m2/repository"
          ;;
      esac
    done
  fi

  # Copy JARs to ~/built_jar
  copy_built_jars_to_dir "$ORIGINAL_HOME"

  # If OBMAS, send them to remote
  if [ "$SEND_AFTER_BUILD" = true ]; then
    send_jars_remote "$ORIGINAL_HOME"
  fi

  echo "🎉 OBM workflow complete."
  exit 0
fi

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

# Stop all services first (critical step)
echo "=============================================="
echo "🛑 INITIAL CLEANUP - STOPPING ALL SERVICES"
echo "=============================================="
echo "Running stopAll.sh to ensure clean deployment environment..."
"$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" || {
    echo "❌ Failed to stop existing services"
    echo "This might cause deployment conflicts. Continue anyway? (y/N)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Deployment cancelled by user"
        exit 1
    fi
}
echo "✅ All existing services stopped and cleaned up"
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
    echo "📂 Maven repository: $ORIGINAL_HOME/.m2"
    [ "$USE_EXISTING_JARS_ONLY" = true ] && echo "📂 Using existing jars from: $ORIGINAL_HOME/built_jar"

    # Helper to resolve jar path based on mode
    resolve_jar_path() {
      local artifact="$1"    # e.g. kiwi-eureka-2.0.jar
      local m2_path="$2"     # fallback path under ~/.m2
      if [ "$USE_EXISTING_JARS_ONLY" = true ]; then
        echo "$ORIGINAL_HOME/built_jar/$artifact"
      else
        echo "$m2_path"
      fi
    }

    # eureka
    if should_build_service "eureka"; then
      echo "📄 Copying eureka files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-eureka/Dockerfile" "$CURRENT_DIR/docker/kiwi/eureka/"
      eureka_jar=$(resolve_jar_path "kiwi-eureka-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar")
      [ -f "$eureka_jar" ] || { echo "❌ Missing jar: $eureka_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$eureka_jar" "$CURRENT_DIR/docker/kiwi/eureka/"
    fi

    # config
    if should_build_service "config"; then
      echo "📄 Copying config files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-config/Dockerfile" "$CURRENT_DIR/docker/kiwi/config/"
      config_jar=$(resolve_jar_path "kiwi-config-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar")
      [ -f "$config_jar" ] || { echo "❌ Missing jar: $config_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$config_jar" "$CURRENT_DIR/docker/kiwi/config/"
    fi

    # upms
    if should_build_service "upms"; then
      echo "📄 Copying upms files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/upms/"
      upms_jar=$(resolve_jar_path "kiwi-upms-biz-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar")
      [ -f "$upms_jar" ] || { echo "❌ Missing jar: $upms_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$upms_jar" "$CURRENT_DIR/docker/kiwi/upms/"
    fi

    # auth
    if should_build_service "auth"; then
      echo "📄 Copying auth files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-auth/Dockerfile" "$CURRENT_DIR/docker/kiwi/auth/"
      auth_jar=$(resolve_jar_path "kiwi-auth-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar")
      [ -f "$auth_jar" ] || { echo "❌ Missing jar: $auth_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$auth_jar" "$CURRENT_DIR/docker/kiwi/auth/"
    fi

    # gate
    if should_build_service "gate"; then
      echo "📄 Copying gateway files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-gateway/Dockerfile" "$CURRENT_DIR/docker/kiwi/gate/"
      gate_jar=$(resolve_jar_path "kiwi-gateway-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar")
      [ -f "$gate_jar" ] || { echo "❌ Missing jar: $gate_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$gate_jar" "$CURRENT_DIR/docker/kiwi/gate/"
    fi

    # word
    if should_build_service "word"; then
      echo "📄 Copying word service files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/biz"
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/word/crawler"
      word_jar=$(resolve_jar_path "kiwi-word-biz-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar")
      [ -f "$word_jar" ] || { echo "❌ Missing jar: $word_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$word_jar" "$CURRENT_DIR/docker/kiwi/word/"
      cp -f "$CURRENT_DIR/gcp-credentials.json" "$CURRENT_DIR/docker/kiwi/word/bizTmp"
    fi

    # crawler
    if should_build_service "crawler"; then
      echo "📄 Copying crawler files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile" "$CURRENT_DIR/docker/kiwi/crawler/"
      crawler_jar=$(resolve_jar_path "kiwi-word-crawler-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar")
      [ -f "$crawler_jar" ] || { echo "❌ Missing jar: $crawler_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$crawler_jar" "$CURRENT_DIR/docker/kiwi/crawler/"
    fi

    # ai
    if should_build_service "ai"; then
      echo "📄 Copying AI service files..."
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/biz/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/biz"
      cp -f "$CURRENT_DIR/microservice-kiwi/kiwi-ai/kiwi-ai-biz/docker/batch/Dockerfile" "$CURRENT_DIR/docker/kiwi/ai/batch"
      ai_jar=$(resolve_jar_path "kiwi-ai-biz-2.0.jar" "$ORIGINAL_HOME/.m2/repository/me/fengorz/kiwi-ai-biz/2.0/kiwi-ai-biz-2.0.jar")
      [ -f "$ai_jar" ] || { echo "❌ Missing jar: $ai_jar. Use -mode=obm/obmas first."; exit 1; }
      cp -f "$ai_jar" "$CURRENT_DIR/docker/kiwi/ai/biz"
      cp -f "$ai_jar" "$CURRENT_DIR/docker/kiwi/ai/batch"
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

    # Prefer docker compose to (re)create only the requested services
    # Fallback to legacy per-service docker build/run if docker compose is unavailable
    if command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    elif docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    else
        COMPOSE_CMD=""
    fi

    if [ -n "$COMPOSE_CMD" ]; then
        # Map short service keys to compose service names
        declare -A COMPOSE_SERVICES=(
            [eureka]="kiwi-eureka"
            [config]="kiwi-config"
            [upms]="kiwi-upms"
            [auth]="kiwi-auth"
            [gate]="kiwi-gate"
            [word]="kiwi-word-biz"
            [crawler]="kiwi-crawler"
            [ai]="kiwi-ai-biz"
        )

        # Build list of compose services to (re)create
        IFS=',' read -ra SERVICE_ARRAY <<< "$services"
        COMPOSE_TARGETS=()
        for service in "${SERVICE_ARRAY[@]}"; do
            target="${COMPOSE_SERVICES[$service]}"
            if [ -n "$target" ]; then
                COMPOSE_TARGETS+=("$target")
            else
                echo "⚠️  Unknown service key '$service' for docker compose; skipping"
            fi
        done

        if [ ${#COMPOSE_TARGETS[@]} -eq 0 ]; then
            echo "❌ No valid services to deploy via docker compose"
            return 1
        fi

        # Use the base + service compose files if present
        COMPOSE_FILES=(
            "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/docker-compose-base.yml"
            "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/docker-compose-service.yml"
        )

        COMPOSE_FILE_ARGS=()
        for f in "${COMPOSE_FILES[@]}"; do
            if [ -f "$f" ]; then
                COMPOSE_FILE_ARGS+=( -f "$f" )
            fi
        done

        echo "🧩 Using docker compose for selective redeploy..."
        echo "   Files: ${COMPOSE_FILE_ARGS[*]}"
        echo "   Targets: ${COMPOSE_TARGETS[*]}"

        # Recreate only selected services (no implicit dependencies unless present)
        $COMPOSE_CMD "${COMPOSE_FILE_ARGS[@]}" up -d --force-recreate --remove-orphans "${COMPOSE_TARGETS[@]}"
    else
        echo "ℹ️  docker compose not found; falling back to legacy per-service build/run"
        # Legacy path: Deploy each selected service using docker build/run
        IFS=',' read -ra SERVICE_ARRAY <<< "$services"
        for service in "${SERVICE_ARRAY[@]}"; do
            echo ""
            deploy_single_service "$service"
        done
    fi

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

# Core deployment operations (modified to skip the redundant stopAll call)
echo "=============================================="
echo "🚀 STARTING CONTAINER DEPLOYMENT:"
echo "=============================================="

# Note: Services are already stopped by the initial stopAll.sh call above
# No need to stop/remove again unless doing selective deployment

# Build Docker images and deploy
if [ "$BUILD_ALL_SERVICES" = true ]; then
  echo "🚀 Starting auto deployment for all services..."
  AUTO_DEPLOY_SCRIPT="$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh"
  if [ ! -f "$AUTO_DEPLOY_SCRIPT" ]; then
     echo "❌ autoDeploy script not found at: $AUTO_DEPLOY_SCRIPT"
     echo "Current directory: $(pwd)"
     ls -l "$CURRENT_DIR/microservice-kiwi/kiwi-deploy/docker/" || true
     exit 1
  fi
  chmod +x "$AUTO_DEPLOY_SCRIPT" || true
  echo "➡️  Invoking: $AUTO_DEPLOY_SCRIPT $MODE"
  echo "   (Logging to $CURRENT_DIR/autoDeploy.log)"
  # Use bash -x for extra diagnostics; preserve failure exit code via pipefail
  bash -x "$AUTO_DEPLOY_SCRIPT" "$MODE" 2>&1 | tee "$CURRENT_DIR/autoDeploy.log"
  echo "✅ autoDeploy completed"
else
  # For selective deployment, we still need to stop/remove only selected services
  echo "🛑 Stopping selected services: $SELECTED_SERVICES"
  stop_selected_containers "$SELECTED_SERVICES"

  echo "🗑️  Removing selected containers: $SELECTED_SERVICES"
  remove_selected_containers "$SELECTED_SERVICES"

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

