# Function to show help
show_help() {
  echo "Usage: $0 [MODE] [OPTIONS]"
  echo ""
  echo "Available modes:"
  echo "  -mode=sg      Skip git operations (stash and pull)"
  echo "  -mode=sm      Skip maven build operation"
  echo "  -mode=sbd     Skip Dockerfile building operation (copying Dockerfiles and JARs)"
  echo "  -mode=sa      Skip all operations (git + maven + Dockerfile building)"
  echo ""
  echo "Available options:"
  echo "  -c            Enable autoCheckService after deployment"
  echo "  -help         Show this help message"
  echo ""
  echo "If no mode is specified, all operations will be executed."
  echo ""
  echo "Examples:"
  echo "  sudo $0                    # Run all operations without autoCheckService"
  echo "  sudo $0 -c                 # Run all operations with autoCheckService"
  echo "  sudo $0 -mode=sg           # Skip only git operations"
  echo "  sudo $0 -mode=sg -c        # Skip git operations and enable autoCheckService"
  echo "  sudo $0 -c -mode=sm        # Skip maven build and enable autoCheckService"
}

# Parse parameters
MODE=""
ENABLE_AUTO_CHECK=false
SKIP_GIT=false
SKIP_MAVEN=false
SKIP_DOCKER_BUILD=false

# Process all arguments
for arg in "$@"; do
  case "$arg" in
    -mode=sg)
      MODE="$arg"
      SKIP_GIT=true
      echo "Skipping git stash and pull operations"
      ;;
    -mode=sm)
      MODE="$arg"
      SKIP_MAVEN=true
      echo "Skipping maven build operation"
      ;;
    -mode=sbd)
      MODE="$arg"
      SKIP_DOCKER_BUILD=true
      echo "Skipping Dockerfile building operation"
      ;;
    -mode=sa)
      MODE="$arg"
      SKIP_GIT=true
      SKIP_MAVEN=true
      SKIP_DOCKER_BUILD=true
      echo "Skipping all operations (git, maven, and Dockerfile building)"
      ;;
    -c)
      ENABLE_AUTO_CHECK=true
      echo "AutoCheckService will be enabled after deployment"
      ;;
    -help|--help|-h)
      show_help
      exit 0
      ;;
    *)
      if [ -n "$arg" ]; then
        echo "Invalid parameter: $arg"
        echo "Use -help to see available options"
        exit 1
      fi
      ;;
  esac
done

# Display final configuration
if [ "$ENABLE_AUTO_CHECK" = true ]; then
  echo "✅ AutoCheckService will be started after deployment"
else
  echo "❌ AutoCheckService will NOT be started (use -c to enable)"
fi

# ... [rest of your existing script remains the same] ...

echo "Sleeping for 200 seconds..."
sleep 200s

# Start autoCheckService only if -c parameter was provided
if [ "$ENABLE_AUTO_CHECK" = true ]; then
  if ! pgrep -f "autoCheckService.sh" >/dev/null; then
    echo "Starting autoCheckService..."
    nohup ~/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh >~/autoCheck.log 2>&1 &
    echo "✅ AutoCheckService started successfully"
  else
    echo "ℹ️  AutoCheckService is already running"
  fi
else
  echo "ℹ️  AutoCheckService not started (use -c parameter to enable)"
fi