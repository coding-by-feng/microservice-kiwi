#!/bin/bash

# Kiwi Microservice Setup Script for Raspberry Pi OS
# This script automates the complete setup process with step tracking and selective re-initialization

set -e  # Exit on any error

# Configuration
PROGRESS_FILE="$(pwd)/.kiwi_setup_progress"
CONFIG_FILE="$(pwd)/.kiwi_setup_config"
SCRIPT_USER=${SUDO_USER:-$USER}
SCRIPT_HOME=$(eval echo ~$SCRIPT_USER)

echo "=================================="
echo "Kiwi Microservice Setup for Raspberry Pi"
echo "Running as: $(whoami)"
echo "Target user: $SCRIPT_USER"
echo "Target home: $SCRIPT_HOME"
echo "=================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "This script must be run as root (use sudo)"
    echo "Usage: sudo ./setup_kiwi.sh"
    exit 1
fi

# Function to save configuration value
save_config() {
    local key="$1"
    local value="$2"

    # Create config file if it doesn't exist
    touch "$CONFIG_FILE"
    chown $SCRIPT_USER:$SCRIPT_USER "$CONFIG_FILE" 2>/dev/null || true
    chmod 600 "$CONFIG_FILE"  # Secure permissions for passwords

    # Remove existing key if present
    sed -i "/^$key=/d" "$CONFIG_FILE" 2>/dev/null || true

    # Add new key-value pair
    echo "$key=$value" >> "$CONFIG_FILE"
}

# Function to load configuration value
load_config() {
    local key="$1"
    if [ -f "$CONFIG_FILE" ]; then
        grep "^$key=" "$CONFIG_FILE" 2>/dev/null | cut -d'=' -f2- || echo ""
    else
        echo ""
    fi
}

# Function to check if configuration exists
has_config() {
    local key="$1"
    [ -f "$CONFIG_FILE" ] && grep -q "^$key=" "$CONFIG_FILE" 2>/dev/null
}

# Function to check if step is completed
is_step_completed() {
    local step_name="$1"
    if [ -f "$PROGRESS_FILE" ]; then
        grep -q "^$step_name$" "$PROGRESS_FILE"
    else
        return 1
    fi
}

# Function to mark step as completed
mark_step_completed() {
    local step_name="$1"
    echo "$step_name" >> "$PROGRESS_FILE"
    chown $SCRIPT_USER:$SCRIPT_USER "$PROGRESS_FILE" 2>/dev/null || true
    echo "✓ Step completed: $step_name"

    # Also save step completion timestamp to config
    save_config "${step_name}_completed_at" "$(date '+%Y-%m-%d %H:%M:%S')"
}

# Function to force re-initialize a step (remove from progress)
force_reinitialize_step() {
    local step_name="$1"
    if [ -f "$PROGRESS_FILE" ]; then
        sed -i "/^$step_name$/d" "$PROGRESS_FILE" 2>/dev/null || true
        echo "✓ Step '$step_name' marked for re-initialization"
    fi
}

# Function to prompt for input
prompt_for_input() {
    local prompt_message="$1"
    local var_name="$2"
    local is_secret="$3"

    while true; do
        if [ "$is_secret" = "true" ]; then
            read -s -p "$prompt_message: " input_value
            echo
        else
            read -p "$prompt_message: " input_value
        fi

        if [ -n "$input_value" ]; then
            eval "$var_name='$input_value'"
            break
        else
            echo "This field cannot be empty. Please try again."
        fi
    done
}

# Function to run command as target user
run_as_user() {
    sudo -u $SCRIPT_USER "$@"
}

# Function to run command as target user with home directory
run_as_user_home() {
    sudo -u $SCRIPT_USER -H "$@"
}

# Function to check and start container if needed
check_and_start_container() {
    local container_name="$1"
    local step_name="$2"

    if is_step_completed "$step_name"; then
        echo "Checking if $container_name is running..."

        # Check if container exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            # Check if container is running
            if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
                echo "✓ $container_name is already running"
                save_config "${container_name}_status_check" "running"
            else
                echo "⚠ $container_name exists but is stopped, starting..."
                if docker start "$container_name"; then
                    echo "✓ $container_name started successfully"
                    save_config "${container_name}_status_check" "started"

                    # Wait a moment for container to stabilize
                    sleep 5

                    # Verify it's actually running
                    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
                        echo "✓ $container_name is now running and healthy"
                    else
                        echo "⚠ $container_name started but may not be healthy"
                        save_config "${container_name}_status_check" "started_but_unhealthy"
                    fi
                else
                    echo "✗ Failed to start $container_name"
                    save_config "${container_name}_status_check" "failed_to_start"
                fi
            fi
        else
            echo "⚠ $container_name does not exist (setup may have been incomplete)"
            save_config "${container_name}_status_check" "container_missing"
        fi
    fi
}

# Function to check MySQL readiness
#!/bin/bash

# Kiwi Microservice Setup Script for Raspberry Pi OS
# This script automates the complete setup process with step tracking and selective re-initialization

set -e  # Exit on any error

# Configuration
PROGRESS_FILE="$(pwd)/.kiwi_setup_progress"
CONFIG_FILE="$(pwd)/.kiwi_setup_config"
SCRIPT_USER=${SUDO_USER:-$USER}
SCRIPT_HOME=$(eval echo ~$SCRIPT_USER)

echo "=================================="
echo "Kiwi Microservice Setup for Raspberry Pi"
echo "Running as: $(whoami)"
echo "Target user: $SCRIPT_USER"
echo "Target home: $SCRIPT_HOME"
echo "=================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    echo "This script must be run as root (use sudo)"
    echo "Usage: sudo ./setup_kiwi.sh"
    exit 1
fi

# Function to save configuration value
save_config() {
    local key="$1"
    local value="$2"

    # Create config file if it doesn't exist
    touch "$CONFIG_FILE"
    chown $SCRIPT_USER:$SCRIPT_USER "$CONFIG_FILE" 2>/dev/null || true
    chmod 600 "$CONFIG_FILE"  # Secure permissions for passwords

    # Remove existing key if present
    sed -i "/^$key=/d" "$CONFIG_FILE" 2>/dev/null || true

    # Add new key-value pair
    echo "$key=$value" >> "$CONFIG_FILE"
}

# Function to load configuration value
load_config() {
    local key="$1"
    if [ -f "$CONFIG_FILE" ]; then
        grep "^$key=" "$CONFIG_FILE" 2>/dev/null | cut -d'=' -f2- || echo ""
    else
        echo ""
    fi
}

# Function to check if configuration exists
has_config() {
    local key="$1"
    [ -f "$CONFIG_FILE" ] && grep -q "^$key=" "$CONFIG_FILE" 2>/dev/null
}

# Function to check if step is completed
is_step_completed() {
    local step_name="$1"
    if [ -f "$PROGRESS_FILE" ]; then
        grep -q "^$step_name$" "$PROGRESS_FILE"
    else
        return 1
    fi
}

# Function to mark step as completed
mark_step_completed() {
    local step_name="$1"
    echo "$step_name" >> "$PROGRESS_FILE"
    chown $SCRIPT_USER:$SCRIPT_USER "$PROGRESS_FILE" 2>/dev/null || true
    echo "✓ Step completed: $step_name"

    # Also save step completion timestamp to config
    save_config "${step_name}_completed_at" "$(date '+%Y-%m-%d %H:%M:%S')"
}

# Function to force re-initialize a step (remove from progress)
force_reinitialize_step() {
    local step_name="$1"
    if [ -f "$PROGRESS_FILE" ]; then
        sed -i "/^$step_name$/d" "$PROGRESS_FILE" 2>/dev/null || true
        echo "✓ Step '$step_name' marked for re-initialization"
    fi
}

# Function to prompt for input
prompt_for_input() {
    local prompt_message="$1"
    local var_name="$2"
    local is_secret="$3"

    while true; do
        if [ "$is_secret" = "true" ]; then
            read -s -p "$prompt_message: " input_value
            echo
        else
            read -p "$prompt_message: " input_value
        fi

        if [ -n "$input_value" ]; then
            eval "$var_name='$input_value'"
            break
        else
            echo "This field cannot be empty. Please try again."
        fi
    done
}

# Function to run command as target user
run_as_user() {
    sudo -u $SCRIPT_USER "$@"
}

# Function to run command as target user with home directory
run_as_user_home() {
    sudo -u $SCRIPT_USER -H "$@"
}

# Function to check and start container if needed
check_and_start_container() {
    local container_name="$1"
    local step_name="$2"

    if is_step_completed "$step_name"; then
        echo "Checking if $container_name is running..."

        # Check if container exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            # Check if container is running
            if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
                echo "✓ $container_name is already running"
                save_config "${container_name}_status_check" "running"
            else
                echo "⚠ $container_name exists but is stopped, starting..."
                if docker start "$container_name"; then
                    echo "✓ $container_name started successfully"
                    save_config "${container_name}_status_check" "started"

                    # Wait a moment for container to stabilize
                    sleep 5

                    # Verify it's actually running
                    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
                        echo "✓ $container_name is now running and healthy"
                    else
                        echo "⚠ $container_name started but may not be healthy"
                        save_config "${container_name}_status_check" "started_but_unhealthy"
                    fi
                else
                    echo "✗ Failed to start $container_name"
                    save_config "${container_name}_status_check" "failed_to_start"
                fi
            fi
        else
            echo "⚠ $container_name does not exist (setup may have been incomplete)"
            save_config "${container_name}_status_check" "container_missing"
        fi
    fi
}

# Function to check if container is running (NO MySQL ping test)
check_container_running() {
    local container_name="$1"
    docker ps --format '{{.Names}}' | grep -q "^${container_name}$"
    return $?
}

# Function to get environment variables
get_env_vars() {
    echo "Loading environment variables..."

    # Load KIWI_ENC_PASSWORD
    if has_config "KIWI_ENC_PASSWORD"; then
        KIWI_ENC_PASSWORD=$(load_config "KIWI_ENC_PASSWORD")
        echo "Using saved KIWI_ENC_PASSWORD"
    else
        prompt_for_input "Enter KIWI_ENC_PASSWORD" "KIWI_ENC_PASSWORD" "true"
        save_config "KIWI_ENC_PASSWORD" "$KIWI_ENC_PASSWORD"
    fi

    # Load GROK_API_KEY
    if has_config "GROK_API_KEY"; then
        GROK_API_KEY=$(load_config "GROK_API_KEY")
        echo "Using saved GROK_API_KEY"
    else
        prompt_for_input "Enter GROK_API_KEY" "GROK_API_KEY" "true"
        save_config "GROK_API_KEY" "$GROK_API_KEY"
    fi
}

# Function to get database passwords
get_db_passwords() {
    echo "Loading database and service configurations..."

    # Load MySQL password
    if has_config "MYSQL_ROOT_PASSWORD"; then
        MYSQL_ROOT_PASSWORD=$(load_config "MYSQL_ROOT_PASSWORD")
        echo "Using saved MySQL root password"
    else
        prompt_for_input "Enter MySQL root password" "MYSQL_ROOT_PASSWORD" "true"
        save_config "MYSQL_ROOT_PASSWORD" "$MYSQL_ROOT_PASSWORD"
    fi

    # Load Redis password
    if has_config "REDIS_PASSWORD"; then
        REDIS_PASSWORD=$(load_config "REDIS_PASSWORD")
        echo "Using saved Redis password"
    else
        prompt_for_input "Enter Redis password" "REDIS_PASSWORD" "true"
        save_config "REDIS_PASSWORD" "$REDIS_PASSWORD"
    fi

    # Load FastDFS hostname
    if has_config "FASTDFS_HOSTNAME"; then
        FASTDFS_HOSTNAME=$(load_config "FASTDFS_HOSTNAME")
        echo "Using saved FastDFS hostname: $FASTDFS_HOSTNAME"
    else
        prompt_for_input "Enter FastDFS hostname (e.g., kiwi-fastdfs)" "FASTDFS_HOSTNAME" "false"
        save_config "FASTDFS_HOSTNAME" "$FASTDFS_HOSTNAME"
    fi

    # Load Elasticsearch root password
    if has_config "ES_ROOT_PASSWORD"; then
        ES_ROOT_PASSWORD=$(load_config "ES_ROOT_PASSWORD")
        echo "Using saved Elasticsearch root password"
    else
        prompt_for_input "Enter Elasticsearch root password" "ES_ROOT_PASSWORD" "true"
        save_config "ES_ROOT_PASSWORD" "$ES_ROOT_PASSWORD"
    fi

    # Load Elasticsearch username
    if has_config "ES_USER_NAME"; then
        ES_USER_NAME=$(load_config "ES_USER_NAME")
        echo "Using saved Elasticsearch username: $ES_USER_NAME"
    else
        prompt_for_input "Enter Elasticsearch additional username" "ES_USER_NAME" "false"
        save_config "ES_USER_NAME" "$ES_USER_NAME"
    fi

    # Load Elasticsearch user password
    if has_config "ES_USER_PASSWORD"; then
        ES_USER_PASSWORD=$(load_config "ES_USER_PASSWORD")
        echo "Using saved Elasticsearch user password"
    else
        prompt_for_input "Enter Elasticsearch additional user password" "ES_USER_PASSWORD" "true"
        save_config "ES_USER_PASSWORD" "$ES_USER_PASSWORD"
    fi
}

# Function to show step selection menu
show_step_menu() {
    echo
    echo "=================================="
    echo "STEP SELECTION MENU"
    echo "=================================="
    echo "Choose setup mode:"
    echo "0. Full automatic setup (default)"
    echo "1. Select specific steps to re-initialize"
    echo "2. Show step status and exit"
    echo
    read -p "Enter your choice (0-2) [default: 0]: " SETUP_MODE

    # Default to 0 if empty
    SETUP_MODE=${SETUP_MODE:-0}

    case $SETUP_MODE in
        1)
            select_steps_to_reinitialize
            ;;
        2)
            show_step_status
            exit 0
            ;;
        0)
            echo "Proceeding with full automatic setup..."
            ;;
        *)
            echo "Invalid choice. Proceeding with full automatic setup..."
            ;;
    esac
}

# Function to select steps to re-initialize
select_steps_to_reinitialize() {
    echo
    echo "=================================="
    echo "SELECT STEPS TO RE-INITIALIZE"
    echo "=================================="
    echo "Available steps:"
    echo " 1. system_update           - System update and package installations"
    echo " 2. docker_install          - Install Docker"
    echo " 3. docker_setup            - Setup Docker alias and test"
    echo " 4. docker_cleanup          - Clean Docker system"
    echo " 5. docker_compose_install  - Install Docker Compose"
    echo " 6. python_install          - Install Python and other packages"
    echo " 7. directories_created     - Create directory structure"
    echo " 8. git_setup               - Clone and setup Git repository"
    echo " 9. hosts_configured        - Configure hosts file"
    echo "10. env_vars_setup          - Setup environment variables"
    echo "11. ytdlp_download          - Download yt-dlp"
    echo "12. mysql_setup             - Setup MySQL"
    echo "13. redis_setup             - Setup Redis"
    echo "14. rabbitmq_setup          - Setup RabbitMQ"
    echo "15. fastdfs_setup           - Setup FastDFS"
    echo "16. maven_lib_install       - Maven library installation"
    echo "17. deployment_setup        - Setup deployment script"
    echo "18. elasticsearch_setup     - Setup Elasticsearch"
    echo "19. ik_tokenizer_install    - Install IK Tokenizer"
    echo "20. nginx_ui_setup          - Setup Nginx and UI"
    echo "21. ALL                     - Re-initialize all steps"
    echo
    echo "Enter step numbers separated by spaces (e.g., '1 3 12' or 'ALL'):"
    read -p "Steps to re-initialize: " SELECTED_STEPS

    if [ -z "$SELECTED_STEPS" ]; then
        echo "No steps selected. Proceeding with normal flow..."
        return
    fi

    # Array of all step names
    declare -a STEP_NAMES=(
        "system_update"
        "docker_install"
        "docker_setup"
        "docker_cleanup"
        "docker_compose_install"
        "python_install"
        "directories_created"
        "git_setup"
        "hosts_configured"
        "env_vars_setup"
        "ytdlp_download"
        "mysql_setup"
        "redis_setup"
        "rabbitmq_setup"
        "fastdfs_setup"
        "maven_lib_install"
        "deployment_setup"
        "elasticsearch_setup"
        "ik_tokenizer_install"
        "nginx_ui_setup"
    )

    if [ "$SELECTED_STEPS" = "ALL" ] || [ "$SELECTED_STEPS" = "21" ]; then
        echo "Re-initializing ALL steps..."
        for step in "${STEP_NAMES[@]}"; do
            force_reinitialize_step "$step"
        done
    else
        for num in $SELECTED_STEPS; do
            if [[ "$num" =~ ^[0-9]+$ ]] && [ "$num" -ge 1 ] && [ "$num" -le 20 ]; then
                step_index=$((num - 1))
                step_name="${STEP_NAMES[$step_index]}"
                force_reinitialize_step "$step_name"
            else
                echo "Warning: Invalid step number '$num' (valid range: 1-20)"
            fi
        done
    fi

    echo
    echo "Selected steps have been marked for re-initialization."
    echo "Proceeding with setup..."
}

# Function to show step status
show_step_status() {
    echo
    echo "=================================="
    echo "CURRENT STEP STATUS"
    echo "=================================="

    declare -a STEP_NAMES=(
        "system_update"
        "docker_install"
        "docker_setup"
        "docker_cleanup"
        "docker_compose_install"
        "python_install"
        "directories_created"
        "git_setup"
        "hosts_configured"
        "env_vars_setup"
        "ytdlp_download"
        "mysql_setup"
        "redis_setup"
        "rabbitmq_setup"
        "fastdfs_setup"
        "maven_lib_install"
        "deployment_setup"
        "elasticsearch_setup"
        "ik_tokenizer_install"
        "nginx_ui_setup"
    )

    declare -a STEP_DESCRIPTIONS=(
        "System update and package installations"
        "Install Docker"
        "Setup Docker alias and test"
        "Clean Docker system"
        "Install Docker Compose"
        "Install Python and other packages"
        "Create directory structure"
        "Clone and setup Git repository"
        "Configure hosts file"
        "Setup environment variables"
        "Download yt-dlp"
        "Setup MySQL"
        "Setup Redis"
        "Setup RabbitMQ"
        "Setup FastDFS"
        "Maven library installation"
        "Setup deployment script"
        "Setup Elasticsearch"
        "Install IK Tokenizer"
        "Setup Nginx and UI"
    )

    echo "Step Status Summary:"
    echo "==================="

    completed_count=0
    total_count=${#STEP_NAMES[@]}

    for i in "${!STEP_NAMES[@]}"; do
        step_name="${STEP_NAMES[$i]}"
        step_desc="${STEP_DESCRIPTIONS[$i]}"
        step_num=$((i + 1))

        if is_step_completed "$step_name"; then
            status="✓ COMPLETED"
            completed_time=$(load_config "${step_name}_completed_at")
            if [ -n "$completed_time" ]; then
                status="$status ($completed_time)"
            fi
            ((completed_count++))
        else
            status="✗ PENDING"
        fi

        printf "%2d. %-25s %s\n" "$step_num" "$step_name" "$status"
        printf "    %s\n" "$step_desc"
        echo
    done

    echo "=================================="
    echo "Progress: $completed_count/$total_count steps completed"
    echo "Progress file: $PROGRESS_FILE"
    echo "Config file: $CONFIG_FILE"
    echo "=================================="
}

# Get environment variables and database passwords first
get_env_vars
get_db_passwords

# Show step selection menu
show_step_menu

echo
echo "Starting setup process..."

# Step 1: System update and package installations
if ! is_step_completed "system_update"; then
    echo "Step 1: Updating system..."
    apt update

    # Record successful completion
    save_config "system_update_status" "completed"
    save_config "system_update_packages" "$(apt list --installed 2>/dev/null | wc -l) packages updated"

    mark_step_completed "system_update"
else
    echo "Step 1: System update already completed, skipping..."
fi

# Step 2: Install Docker
if ! is_step_completed "docker_install"; then
    echo "Step 2: Installing Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh

    # Add user to docker group
    usermod -aG docker $SCRIPT_USER

    # Start and enable docker service
    systemctl start docker
    systemctl enable docker

    # Record Docker version and status
    DOCKER_VERSION=$(docker --version 2>/dev/null || echo "unknown")
    save_config "docker_version" "$DOCKER_VERSION"
    save_config "docker_service_status" "enabled and started"
    save_config "docker_user_added" "$SCRIPT_USER added to docker group"

    mark_step_completed "docker_install"
else
    echo "Step 2: Docker already installed, skipping..."
fi

# Step 3: Setup Docker alias and test
if ! is_step_completed "docker_setup"; then
    echo "Step 3: Setting up Docker..."

    # Setup Docker alias (using podman if needed)
    if ! run_as_user grep -q "alias docker='podman'" "$SCRIPT_HOME/.bashrc" 2>/dev/null; then
        run_as_user bash -c "echo \"alias docker='podman'\" >> ~/.bashrc"
    fi

    # Test docker (may need newgrp to take effect)
    echo "Testing Docker installation..."
    DOCKER_TEST_STATUS="success"
    if ! run_as_user_home docker version >/dev/null 2>&1; then
        DOCKER_TEST_STATUS="needs_relogin"
        echo "Note: You may need to log out and back in for Docker permissions to take effect"
        echo "Alternatively, run: newgrp docker"
    fi

    # Record Docker setup details
    save_config "docker_alias_added" "podman alias configured"
    save_config "docker_test_status" "$DOCKER_TEST_STATUS"

    mark_step_completed "docker_setup"
else
    echo "Step 3: Docker setup already completed, skipping..."
fi

# Step 3.5: Clean Docker system (after install or skip)
if ! is_step_completed "docker_cleanup"; then
    echo "Step 3.5: Cleaning Docker system..."

    # Check if Docker is accessible
    if docker version >/dev/null 2>&1; then
        echo "Pruning Docker system to clean old images and cache..."

        # Show current Docker usage before cleanup
        DOCKER_USAGE_BEFORE=$(docker system df 2>/dev/null || echo "Unable to check")

        # Perform system prune
        docker system prune -a -f

        # Show usage after cleanup
        DOCKER_USAGE_AFTER=$(docker system df 2>/dev/null || echo "Unable to check")

        # Record cleanup details
        save_config "docker_cleanup_performed" "system prune -a -f executed"
        save_config "docker_usage_before_cleanup" "$DOCKER_USAGE_BEFORE"
        save_config "docker_usage_after_cleanup" "$DOCKER_USAGE_AFTER"

        echo "Docker system cleanup completed."
    else
        echo "Docker not accessible, skipping cleanup. Will retry after permissions are fixed."
        save_config "docker_cleanup_skipped" "Docker not accessible at this time"
    fi

    mark_step_completed "docker_cleanup"
else
    echo "Step 3.5: Docker cleanup already completed, skipping..."
fi

# Step 4: Install Docker Compose
if ! is_step_completed "docker_compose_install"; then
    echo "Step 4: Installing Docker Compose..."

    # Detect architecture
    ARCH=$(uname -m)
    case $ARCH in
        x86_64)
            COMPOSE_ARCH="linux-x86_64"
            ;;
        aarch64|arm64)
            COMPOSE_ARCH="linux-aarch64"
            ;;
        armv7l)
            COMPOSE_ARCH="linux-armv7"
            ;;
        *)
            echo "Unsupported architecture: $ARCH"
            COMPOSE_ARCH="linux-x86_64"  # fallback
            ;;
    esac

    wget https://github.com/docker/compose/releases/download/v2.35.1/docker-compose-$COMPOSE_ARCH -O docker-compose
    mv docker-compose /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Record Docker Compose details
    COMPOSE_VERSION=$(docker-compose --version 2>/dev/null || echo "unknown")
    save_config "docker_compose_version" "$COMPOSE_VERSION"
    save_config "docker_compose_arch" "$COMPOSE_ARCH"
    save_config "system_architecture" "$ARCH"

    mark_step_completed "docker_compose_install"
else
    echo "Step 4: Docker Compose already installed, skipping..."
fi

# Step 5: Install Python and other packages
if ! is_step_completed "python_install"; then
    echo "Step 5: Installing Python and other packages..."
    apt install python3 python3-pip python3-venv python3-full podman-compose openjdk-17-jdk maven git -y

    # Record installed versions
    PYTHON_VERSION=$(python3 --version 2>/dev/null || echo "unknown")
    PIP_VERSION=$(pip3 --version 2>/dev/null || echo "unknown")
    JAVA_VERSION=$(java -version 2>&1 | head -n1 || echo "unknown")
    MAVEN_VERSION=$(mvn --version 2>/dev/null | head -n1 || echo "unknown")
    GIT_VERSION=$(git --version 2>/dev/null || echo "unknown")

    save_config "python_version" "$PYTHON_VERSION"
    save_config "pip_version" "$PIP_VERSION"
    save_config "java_version" "$JAVA_VERSION"
    save_config "maven_version" "$MAVEN_VERSION"
    save_config "git_version" "$GIT_VERSION"
    save_config "packages_installed" "python3 python3-pip python3-venv python3-full podman-compose openjdk-17-jdk maven git"

    mark_step_completed "python_install"
else
    echo "Step 5: Python and packages already installed, skipping..."
fi

# Step 6: Create directory structure
if ! is_step_completed "directories_created"; then
    echo "Step 6: Creating directory structure..."
    cd "$SCRIPT_HOME"

    run_as_user mkdir -p microservice-kiwi docker storage_data store_path tracker_data
    run_as_user mkdir -p docker/kiwi docker/ui docker/rabbitmq docker/mysql

    cd "$SCRIPT_HOME/docker/kiwi"
    run_as_user mkdir -p auth config crawler eureka gate upms word ai
    run_as_user mkdir -p auth/logs config/logs crawler/logs crawler/tmp eureka/logs gate/logs upms/logs word/logs word/bizTmp word/crawlerTmp word/biz word/crawler ai/logs ai/tmp ai/biz ai/batch

    cd "$SCRIPT_HOME/docker/ui"
    run_as_user mkdir -p dist nginx

    # Record directory structure
    TOTAL_DIRS=$(find "$SCRIPT_HOME" -type d -name "docker" -o -name "microservice-kiwi" -o -name "storage_data" -o -name "store_path" -o -name "tracker_data" | wc -l)
    save_config "directories_created_count" "$TOTAL_DIRS main directories"
    save_config "directory_structure" "microservice-kiwi, docker, storage_data, store_path, tracker_data with subdirectories"

    mark_step_completed "directories_created"
else
    echo "Step 6: Directory structure already created, skipping..."
fi

# Step 7: Clone and setup Git repository
if ! is_step_completed "git_setup"; then
    echo "Step 7: Setting up Git repository..."
    cd "$SCRIPT_HOME/microservice-kiwi/"

    # Check for corrupted git index and fix it
    if [ -d ".git" ]; then
        echo "Checking Git repository integrity..."
        if ! run_as_user_home git status >/dev/null 2>&1; then
            echo "Git repository appears corrupted, cleaning up..."
            run_as_user rm -rf .git
            echo "Removed corrupted .git directory"
            save_config "git_corruption_fixed" "true - removed corrupted .git directory"
        fi
    fi

    if [ ! -d ".git" ]; then
        echo "Initializing fresh Git repository..."
        run_as_user_home git init
        run_as_user_home git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
    fi

    # Ensure remote exists
    if ! run_as_user_home git remote get-url origin >/dev/null 2>&1; then
        run_as_user_home git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
    fi

    echo "Fetching latest code..."
    run_as_user_home git fetch --all
    run_as_user_home git reset --hard origin/master
    run_as_user_home git branch --set-upstream-to=origin/master master 2>/dev/null || true
    run_as_user_home git pull

    # Create symbolic links
    echo "Creating deployment shortcuts..."
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/deployKiwi.sh" "$SCRIPT_HOME/easy-deploy"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" "$SCRIPT_HOME/easy-stop"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/auto_deploy_ui.sh" "$SCRIPT_HOME/easy-deploy-ui"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/checkContainers.sh" "$SCRIPT_HOME/easy-check"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/set_up.sh" "$SCRIPT_HOME/easy-setup"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/clean_set_up.sh" "$SCRIPT_HOME/easy-clean-setup"

    # Make symbolic links executable
    run_as_user chmod 777 "$SCRIPT_HOME/easy-deploy"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-stop"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-deploy-ui"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-check"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-setup"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-clean-setup"

    # Record Git setup details
    GIT_COMMIT_HASH=$(run_as_user_home git rev-parse HEAD 2>/dev/null || echo "unknown")
    GIT_BRANCH=$(run_as_user_home git branch --show-current 2>/dev/null || echo "unknown")
    save_config "git_repository_url" "https://github.com/coding-by-feng/microservice-kiwi.git"
    save_config "git_commit_hash" "$GIT_COMMIT_HASH"
    save_config "git_branch" "$GIT_BRANCH"
    save_config "symbolic_links_created" "easy-deploy, easy-stop, easy-deploy-ui"

    mark_step_completed "git_setup"
else
    echo "Step 7: Git repository already set up, skipping..."
fi

# Step 8: Configure hosts file
if ! is_step_completed "hosts_configured"; then
    echo "Step 8: Configuring hosts file..."

    # Check if hosts entries already exist
    if ! grep -q "fastdfs.fengorz.me" /etc/hosts; then
        tee -a /etc/hosts > /dev/null << EOF
127.0.0.1    fastdfs.fengorz.me
127.0.0.1    kiwi-microservice-local
127.0.0.1    kiwi-microservice
127.0.0.1    kiwi-ui
127.0.0.1    kiwi-eureka
127.0.0.1    kiwi-redis
127.0.0.1    kiwi-rabbitmq
127.0.0.1    kiwi-db
127.0.0.1    kiwi-es
127.0.0.1    kiwi-config
127.0.0.1    kiwi-auth
127.0.0.1    kiwi-upms
127.0.0.1    kiwi-gate
127.0.0.1    kiwi-ai
127.0.0.1    kiwi-crawler
EOF
    fi

    mark_step_completed "hosts_configured"
else
    echo "Step 8: Hosts file already configured, skipping..."
fi

# Step 9: Setup environment variables
if ! is_step_completed "env_vars_setup"; then
    echo "Step 9: Setting up environment variables..."

    # Remove old entries if they exist
    run_as_user sed -i '/export KIWI_ENC_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export GROK_API_KEY=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export DB_IP=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export MYSQL_ROOT_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export REDIS_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export FASTDFS_HOSTNAME=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_ROOT_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_USER_NAME=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_USER_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true

    # Add new entries to .bashrc
    run_as_user bash -c "echo 'export KIWI_ENC_PASSWORD=\"$KIWI_ENC_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export DB_IP=\"127.0.0.1\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export GROK_API_KEY=\"$GROK_API_KEY\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export MYSQL_ROOT_PASSWORD=\"$MYSQL_ROOT_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export REDIS_PASSWORD=\"$REDIS_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export FASTDFS_HOSTNAME=\"$FASTDFS_HOSTNAME\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_ROOT_PASSWORD=\"$ES_ROOT_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_USER_NAME=\"$ES_USER_NAME\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_USER_PASSWORD=\"$ES_USER_PASSWORD\"' >> ~/.bashrc"

    # Record environment variables setup
    save_config "env_vars_added_to_bashrc" "KIWI_ENC_PASSWORD, GROK_API_KEY, DB_IP, MYSQL_ROOT_PASSWORD, REDIS_PASSWORD, FASTDFS_HOSTNAME, ES_ROOT_PASSWORD, ES_USER_NAME, ES_USER_PASSWORD"
    save_config "bashrc_location" "$SCRIPT_HOME/.bashrc"

    echo "Environment variables added to .bashrc and saved to persistent configuration."

    mark_step_completed "env_vars_setup"
else
    echo "Step 9: Environment variables already set up, skipping..."
fi

# Step 10: Download yt-dlp
if ! is_step_completed "ytdlp_download"; then
    echo "Step 10: Downloading yt-dlp..."

    wget https://github.com/yt-dlp/yt-dlp/releases/download/2025.04.30/yt-dlp_linux -O /tmp/yt-dlp_linux
    chmod +x /tmp/yt-dlp_linux

    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/biz/"
    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/batch/"

    rm /tmp/yt-dlp_linux

    mark_step_completed "ytdlp_download"
else
    echo "Step 10: yt-dlp already downloaded, skipping..."
fi

# Step 11: Setup MySQL (SIMPLIFIED VERSION - Container Status Only)
if ! is_step_completed "mysql_setup"; then
    echo "Step 11: Setting up MySQL..."

    # Check if port 3306 is already in use
    if netstat -tlnp | grep -q ":3306 "; then
        echo "Warning: Port 3306 is already in use. Checking what's using it:"
        netstat -tlnp | grep ":3306 "
        echo "You may need to stop the conflicting service or use a different port."
    fi

    # Stop and remove existing container if it exists
    docker stop kiwi-mysql 2>/dev/null || true
    docker rm kiwi-mysql 2>/dev/null || true

    # Use MySQL 8.0 for better ARM compatibility
    echo "Pulling MySQL 8.0 image..."
    if docker pull mysql:8.0; then
        echo "Using MySQL 8.0..."

        # Run MySQL container with memory optimization for Raspberry Pi
        docker run -d --name kiwi-mysql \
            -p 3306:3306 \
            -v "$SCRIPT_HOME/docker/mysql:/mysql_tmp" \
            -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
            -e MYSQL_DATABASE="kiwi_db" \
            --restart=unless-stopped \
            mysql:8.0 \
            --innodb-buffer-pool-size=128M \
            --innodb-log-file-size=32M \
            --innodb-flush-method=O_DSYNC \
            --innodb-flush-log-at-trx-commit=2 \
            --max-connections=50 \
            --default-authentication-plugin=mysql_native_password

        save_config "mysql_engine" "MySQL 8.0"
    else
        echo "Failed to pull MySQL 8.0, trying latest..."
        if docker pull mysql:latest; then
            echo "Using MySQL latest..."

            # Run MySQL container
            docker run -d --name kiwi-mysql \
                -p 3306:3306 \
                -v "$SCRIPT_HOME/docker/mysql:/mysql_tmp" \
                -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
                -e MYSQL_DATABASE="kiwi_db" \
                --restart=unless-stopped \
                mysql:latest \
                --default-authentication-plugin=mysql_native_password

            save_config "mysql_engine" "MySQL latest"
        else
            echo "❌ Failed to pull MySQL images"
            echo "Please check your internet connection and Docker installation"
            save_config "mysql_setup_status" "failed - unable to pull images"
            mark_step_completed "mysql_setup"  # Mark as completed to prevent retry loop
            exit 1
        fi
    fi

    # Wait for container to start and check if it's running
    echo "Waiting for MySQL container to start..."

    CONTAINER_STARTED=false
    for i in {1..12}; do
        echo "Attempt $i/12: Checking if container is running..."
        if check_container_running "kiwi-mysql"; then
            echo "✓ MySQL container is running!"
            CONTAINER_STARTED=true
            break
        fi
        echo "Container not running yet, waiting 5 more seconds..."
        sleep 5
    done

    if [ "$CONTAINER_STARTED" = false ]; then
        echo "✗ MySQL container failed to start within 1 minute"
        echo "Checking container status and logs:"
        echo "Container status:"
        docker ps -a | grep kiwi-mysql
        echo "Container logs:"
        docker logs kiwi-mysql --tail 20
        echo
        echo "Troubleshooting steps:"
        echo "1. Check if port 3306 is in use: sudo netstat -tlnp | grep 3306"
        echo "2. Check container status: docker ps -a | grep kiwi-mysql"
        echo "3. View full logs: docker logs kiwi-mysql"
        echo "4. Check system resources: free -h"
        echo "5. Try manual start: docker start kiwi-mysql"

        save_config "mysql_setup_status" "failed - container did not start"

        # Ask user if they want to continue
        echo
        read -p "MySQL container failed to start. Continue with other steps? (y/N): " CONTINUE_SETUP
        if [[ ! "$CONTINUE_SETUP" =~ ^[Yy]$ ]]; then
            echo "Setup aborted. You can re-run the script later to retry."
            exit 1
        fi
    else
        echo "✓ MySQL container started successfully!"

        # Give MySQL some time to initialize (but don't test connection)
        echo "Giving MySQL time to initialize database (waiting 30 seconds)..."
        sleep 30

        # Check for and restore database backup if it exists (optional, no connection test)
        echo "Note: You can restore database backups manually after MySQL is fully ready:"
        if [ -f "$SCRIPT_HOME/docker/mysql/kiwi-db.sql" ]; then
            echo "  Found backup: $SCRIPT_HOME/docker/mysql/kiwi-db.sql"
            echo "  Restore command: docker exec kiwi-mysql sh -c \"mysql -h localhost -u root -p'$MYSQL_ROOT_PASSWORD' kiwi_db < /mysql_tmp/kiwi-db.sql\""
        fi

        if [ -f "$SCRIPT_HOME/microservice-kiwi/kiwi-sql/ytb_table_initialize.sql" ]; then
            echo "  Found YTB tables: $SCRIPT_HOME/microservice-kiwi/kiwi-sql/ytb_table_initialize.sql"
            echo "  You can copy this file to docker/mysql/ directory and restore it later"
        fi

        # Record MySQL setup details
        MYSQL_CONTAINER_ID=$(docker ps -q -f name=kiwi-mysql)
        MYSQL_IMAGE_INFO=$(docker inspect kiwi-mysql --format='{{.Config.Image}}' 2>/dev/null || echo "unknown")
        save_config "mysql_container_id" "$MYSQL_CONTAINER_ID"
        save_config "mysql_image_info" "$MYSQL_IMAGE_INFO"
        save_config "mysql_database_created" "kiwi_db (created automatically)"
        save_config "mysql_port" "3306"
        save_config "mysql_volume_mount" "$SCRIPT_HOME/docker/mysql:/mysql_tmp"
        save_config "mysql_setup_status" "container started successfully"
        save_config "mysql_backup_restored" "manual restoration required - no automatic testing"

        echo "✓ MySQL setup completed. Container 'kiwi-mysql' is running."
        echo "Note: MySQL may take a few more minutes to fully initialize. You can check with:"
        echo "  docker logs kiwi-mysql"
        echo "  docker exec kiwi-mysql mysql -h localhost -u root -p'$MYSQL_ROOT_PASSWORD' -e 'SELECT 1;'"
    fi

    mark_step_completed "mysql_setup"
else
    echo "Step 11: MySQL already set up, skipping..."

    # Just check if container exists and start it if stopped
    if docker ps -a --format '{{.Names}}' | grep -q "^kiwi-mysql$"; then
        if check_container_running "kiwi-mysql"; then
            echo "✓ MySQL container is already running"
            save_config "mysql_status_check" "running"
        else
            echo "⚠ MySQL container exists but is stopped, starting..."
            if docker start kiwi-mysql; then
                echo "✓ MySQL container started successfully"
                save_config "mysql_status_check" "started"

                # Wait a moment for container to stabilize
                sleep 5

                # Verify it's actually running
                if check_container_running "kiwi-mysql"; then
                    echo "✓ MySQL container is now running"
                else
                    echo "⚠ MySQL container started but may not be healthy"
                    save_config "mysql_status_check" "started_but_may_be_unhealthy"
                fi
            else
                echo "✗ Failed to start MySQL container"
                save_config "mysql_status_check" "failed_to_start"
            fi
        fi
    else
        echo "⚠ MySQL container does not exist (setup may have been incomplete)"
        save_config "mysql_status_check" "container_missing"
    fi
fi

# Step 12: Setup Redis
if ! is_step_completed "redis_setup"; then
    echo "Step 12: Setting up Redis..."

    # Pull Redis image
    docker pull redis:latest

    # Stop and remove existing container if it exists
    docker stop kiwi-redis 2>/dev/null || true
    docker rm kiwi-redis 2>/dev/null || true

    # Run Redis container
    docker run -itd --name kiwi-redis -p 6379:6379 \
        redis --requirepass "$REDIS_PASSWORD"

    echo "Redis setup completed with password protection."

    mark_step_completed "redis_setup"
else
    echo "Step 12: Redis already set up, skipping..."
    check_and_start_container "kiwi-redis" "redis_setup"
fi

# Step 13: Setup RabbitMQ
if ! is_step_completed "rabbitmq_setup"; then
    echo "Step 13: Setting up RabbitMQ..."

    # Pull RabbitMQ image
    docker pull rabbitmq:management

    # Stop and remove existing container if it exists
    docker stop kiwi-rabbit 2>/dev/null || true
    docker rm kiwi-rabbit 2>/dev/null || true

    # Run RabbitMQ container
    docker run -d --hostname kiwi-rabbit \
        -v "$SCRIPT_HOME/docker/rabbitmq:/tmp" \
        --name kiwi-rabbit \
        --net=host \
        rabbitmq:management

    echo "RabbitMQ setup completed."
    echo "Management UI will be available at: http://localhost:15672"
    echo "Default credentials: guest/guest"

    mark_step_completed "rabbitmq_setup"
else
    echo "Step 13: RabbitMQ already set up, skipping..."
    check_and_start_container "kiwi-rabbit" "rabbitmq_setup"
fi

# Step 14: Setup FastDFS
if ! is_step_completed "fastdfs_setup"; then
    echo "Step 14: Setting up FastDFS..."

    # Pull FastDFS image
    docker pull fyclinux/fastdfs-arm64:6.04

    # Stop and remove existing containers if they exist
    docker stop tracker storage 2>/dev/null || true
    docker rm tracker storage 2>/dev/null || true

    # Run tracker
    docker run -ti -d \
        --name tracker \
        --net=host \
        fyclinux/fastdfs-arm64:6.04 \
        tracker

    # Wait for tracker to start
    sleep 10

    # Run storage
    docker run -ti -d \
        --name storage \
        -v "$SCRIPT_HOME/storage_data:/fastdfs/storage/data" \
        -v "$SCRIPT_HOME/store_path:/fastdfs/store_path" \
        --net=host \
        -e TRACKER_SERVER="$FASTDFS_HOSTNAME:22122" \
        fyclinux/fastdfs-arm64:6.04 \
        storage

    # Wait for storage to start
    sleep 15

    # Configure storage.conf
    echo "Configuring FastDFS storage..."
    docker exec storage sed -i "s/tracker_server=.*/tracker_server=$FASTDFS_HOSTNAME:22122/" /etc/fdfs/storage.conf

    # Restart storage container to apply configuration
    docker restart storage

    echo "FastDFS setup completed."
    echo "Tracker and Storage containers are running."

    mark_step_completed "fastdfs_setup"
else
    echo "Step 14: FastDFS already set up, skipping..."
    check_and_start_container "tracker" "fastdfs_setup"
    check_and_start_container "storage" "fastdfs_setup"
fi

# Step 15: Maven library installation
if ! is_step_completed "maven_lib_install"; then
    echo "Step 15: Installing Maven library..."

    cd "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"

    # Install voicerss_tts.jar if it exists
    if [ -f "voicerss_tts.jar" ]; then
        run_as_user_home mvn install:install-file \
            -Dfile=voicerss_tts.jar \
            -DgroupId=voicerss \
            -DartifactId=tts \
            -Dversion=2.0 \
            -Dpackaging=jar
        echo "Maven library installation completed."

        # Record successful installation
        save_config "maven_lib_installed" "voicerss_tts.jar installed successfully"
        save_config "maven_lib_path" "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
    else
        echo "Warning: voicerss_tts.jar not found in lib directory"
        echo "Expected location: $SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib/voicerss_tts.jar"

        # Check if the directory exists
        if [ ! -d "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib" ]; then
            echo "Directory doesn't exist. Creating directory structure..."
            run_as_user mkdir -p "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
            echo "Please place voicerss_tts.jar in the lib directory and re-run this step"
        fi

        save_config "maven_lib_installed" "voicerss_tts.jar not found - manual installation required"
    fi

    mark_step_completed "maven_lib_install"
else
    echo "Step 15: Maven library already installed, skipping..."
fi

# Step 16: Setup deployment script
if ! is_step_completed "deployment_setup"; then
    echo "Step 16: Setting up deployment..."

    cd "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker"

    # Copy deployment script if it doesn't already exist as symlink
    if [ ! -L "$SCRIPT_HOME/deployKiwi.sh" ]; then
        run_as_user cp deployKiwi.sh "$SCRIPT_HOME/"
        run_as_user chmod 777 "$SCRIPT_HOME/deployKiwi.sh"
    fi

    echo "Deployment script setup completed."

    mark_step_completed "deployment_setup"
else
    echo "Step 16: Deployment already set up, skipping..."
fi

# Step 17: Setup Elasticsearch
if ! is_step_completed "elasticsearch_setup"; then
    echo "Step 17: Setting up Elasticsearch..."

    # Pull Elasticsearch image
    docker pull elasticsearch:7.17.9

    # Stop and remove existing container if it exists
    docker stop kiwi-es 2>/dev/null || true
    docker rm kiwi-es 2>/dev/null || true

    # Remove existing volumes if they exist
    docker volume rm es_config es_data 2>/dev/null || true

    # Run Elasticsearch container
    docker run -d -p 9200:9200 -p 9300:9300 --hostname kiwi-es \
        -e "discovery.type=single-node" \
        -e "xpack.security.enabled=true" \
        -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
        --name kiwi-es \
        -v es_config:/usr/share/elasticsearch/config \
        -v es_data:/usr/share/elasticsearch/data \
        elasticsearch:7.17.9

    # Wait for Elasticsearch to start
    echo "Waiting for Elasticsearch to start..."
    sleep 60

    # Create users
    echo "Creating Elasticsearch users..."
    docker exec kiwi-es bash -c "echo '$ES_ROOT_PASSWORD' | elasticsearch-users useradd root -r superuser -p"
    docker exec kiwi-es bash -c "echo '$ES_USER_PASSWORD' | elasticsearch-users useradd $ES_USER_NAME -r superuser -p"

    # Wait a bit more for users to be created
    sleep 10

    # Create kiwi_vocabulary index
    echo "Creating kiwi_vocabulary index..."
    INDEX_CREATION_RESULT=$(docker exec kiwi-es curl -X PUT "localhost:9200/kiwi_vocabulary" \
        -u "root:$ES_ROOT_PASSWORD" \
        -H "Content-Type: application/json" \
        -d '{
            "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
            }
        }' 2>/dev/null || echo "failed")

    # Record Elasticsearch setup details
    ES_CONTAINER_ID=$(docker ps -q -f name=kiwi-es)
    ES_IMAGE_INFO=$(docker images elasticsearch --format "{{.Repository}}:{{.Tag}} {{.Size}}" | grep "7.17.9" | head -n1)
    save_config "elasticsearch_container_id" "$ES_CONTAINER_ID"
    save_config "elasticsearch_image_info" "$ES_IMAGE_INFO"
    save_config "elasticsearch_users_created" "root, $ES_USER_NAME"
    save_config "elasticsearch_index_created" "kiwi_vocabulary"
    save_config "elasticsearch_index_creation_result" "$INDEX_CREATION_RESULT"
    save_config "elasticsearch_ports" "9200, 9300"
    save_config "elasticsearch_memory_config" "512m heap size"

    echo "Elasticsearch setup completed."

    mark_step_completed "elasticsearch_setup"
else
    echo "Step 17: Elasticsearch already set up, skipping..."
    check_and_start_container "kiwi-es" "elasticsearch_setup"
fi

# Step 18: Install IK Tokenizer
if ! is_step_completed "ik_tokenizer_install"; then
    echo "Step 18: Installing IK Tokenizer..."

    # Install IK plugin
    docker exec kiwi-es elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/7.17.9 --batch

    # Restart Elasticsearch
    docker restart kiwi-es

    echo "Waiting for Elasticsearch to restart..."
    sleep 30

    echo "IK Tokenizer installation completed."

    mark_step_completed "ik_tokenizer_install"
else
    echo "Step 18: IK Tokenizer already installed, skipping..."
fi

# Step 19: Setup Nginx and UI
if ! is_step_completed "nginx_ui_setup"; then
    echo "Step 19: Setting up Nginx and UI..."

    # Pull Nginx image
    docker pull nginx

    # Stop and remove existing container if it exists
    docker stop kiwi-ui 2>/dev/null || true
    docker rm kiwi-ui 2>/dev/null || true

    # Build Kiwi UI image
    cd "$SCRIPT_HOME"
    docker build -f "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/Dockerfile" -t kiwi-ui:1.0 "$SCRIPT_HOME/docker/ui/"

    # Run Kiwi UI container
    docker run -d \
        -v "$SCRIPT_HOME/docker/ui/dist/:/usr/share/nginx/html" \
        --net=host \
        --name=kiwi-ui \
        -it kiwi-ui:1.0

    echo "Nginx and UI setup completed."
    echo "UI will be available at: http://localhost:80"

    mark_step_completed "nginx_ui_setup"
else
    echo "Step 19: Nginx and UI already set up, skipping..."
    check_and_start_container "kiwi-ui" "nginx_ui_setup"
fi

echo
echo "=================================="
echo "Setup completed successfully!"
echo "=================================="
echo "Target user: $SCRIPT_USER"
echo "Home directory: $SCRIPT_HOME"
echo
echo "Available shortcuts:"
echo "  $SCRIPT_HOME/easy-deploy     - Deploy Kiwi services"
echo "  $SCRIPT_HOME/easy-stop       - Stop all services"
echo "  $SCRIPT_HOME/easy-deploy-ui  - Deploy UI"
echo "  $SCRIPT_HOME/easy-check      - Check container status"
echo "  $SCRIPT_HOME/easy-setup      - Re-run setup"
echo "  $SCRIPT_HOME/easy-clean-setup - Clean and re-setup"
echo
echo "Environment variables set:"
echo "  KIWI_ENC_PASSWORD = [HIDDEN]"
echo "  GROK_API_KEY      = [HIDDEN]"
echo "  DB_IP             = 127.0.0.1"
echo "  MYSQL_ROOT_PASSWORD = [HIDDEN]"
echo "  REDIS_PASSWORD    = [HIDDEN]"
echo "  FASTDFS_HOSTNAME  = $FASTDFS_HOSTNAME"
echo "  ES_ROOT_PASSWORD  = [HIDDEN]"
echo "  ES_USER_NAME      = $ES_USER_NAME"
echo "  ES_USER_PASSWORD  = [HIDDEN]"
echo
echo "IMPORTANT NOTES:"
echo "1. Please log out and back in (or run 'newgrp docker') to apply Docker group permissions"
echo "2. Run 'source ~/.bashrc' to apply environment changes"
echo "3. Progress is saved in: $PROGRESS_FILE"
echo "4. Configuration is saved in: $CONFIG_FILE"
echo "5. To reset progress: rm $PROGRESS_FILE"
echo "6. To reset saved inputs: rm $CONFIG_FILE"
echo "7. To reset everything: rm $PROGRESS_FILE $CONFIG_FILE"
echo "8. To re-run specific steps: sudo ./$(basename $0) and choose option 1"
echo
echo "CONTAINER STATUS CHECK:"
echo "======================"

# Final container health check - check all containers regardless of step completion status
CONTAINERS=("kiwi-mysql" "kiwi-redis" "kiwi-rabbit" "tracker" "storage" "kiwi-es" "kiwi-ui")
RUNNING_COUNT=0
STOPPED_COUNT=0
MISSING_COUNT=0
TOTAL_COUNT=${#CONTAINERS[@]}

echo "Checking all expected containers..."

# Get list of running containers and all containers with error handling
echo "Fetching container lists..."
RUNNING_CONTAINERS=$(docker ps --format '{{.Names}}' 2>/dev/null)
ALL_CONTAINERS=$(docker ps -a --format '{{.Names}}' 2>/dev/null)

for container in "${CONTAINERS[@]}"; do
    if echo "$ALL_CONTAINERS" | grep -q "^${container}$"; then
        if echo "$RUNNING_CONTAINERS" | grep -q "^${container}$"; then
            echo "✓ $container - RUNNING"
            ((RUNNING_COUNT++))
        else
            echo "⚠ $container - STOPPED"
            ((STOPPED_COUNT++))
        fi
    else
        echo "✗ $container - MISSING"
        ((MISSING_COUNT++))
    fi
done

echo
echo "CONTAINER SUMMARY:"
echo "  Running: $RUNNING_COUNT/$TOTAL_COUNT"
echo "  Stopped: $STOPPED_COUNT/$TOTAL_COUNT"
echo "  Missing: $MISSING_COUNT/$TOTAL_COUNT"
echo
echo "USEFUL COMMANDS:"
echo "  Check all containers:     docker ps -a"
echo "  Check running only:       docker ps"
echo "  View container logs:      docker logs <container-name>"
echo "  Start stopped container:  docker start <container-name>"
echo "  Stop running container:   docker stop <container-name>"
echo "  Restart container:        docker restart <container-name>"
echo
echo "SERVICES:"
echo "- MySQL: Container 'kiwi-mysql' on port 3306"
echo "- Redis: Container 'kiwi-redis' on port 6379"
echo "- RabbitMQ: Container 'kiwi-rabbit' with management UI on port 15672"
echo "- FastDFS: Containers 'tracker' and 'storage' on port 22122"
echo "- Elasticsearch: Container 'kiwi-es' on ports 9200/9300 with IK tokenizer"
echo "- Nginx/UI: Container 'kiwi-ui' on port 80"
echo
echo "WEB INTERFACES:"
echo "- RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo "- Elasticsearch: http://localhost:9200 (root/[your_password])"
echo "- Kiwi UI: http://localhost:80"
echo
echo "To check container status: docker ps"
echo "To view container logs: docker logs <container-name>"
echo "To re-run this script with step selection: sudo ./$(basename $0)"
echo "=================================="

# Final step: Show Docker status
echo "Docker service status:"
systemctl status docker --no-pager -l || true() {
    docker exec kiwi-mysql mysql -h localhost -u root -p"$MYSQL_ROOT_PASSWORD" -e "SELECT 1;" >/dev/null 2>&1
    return $?
}

# Function to get environment variables
get_env_vars() {
    echo "Loading environment variables..."

    # Load KIWI_ENC_PASSWORD
    if has_config "KIWI_ENC_PASSWORD"; then
        KIWI_ENC_PASSWORD=$(load_config "KIWI_ENC_PASSWORD")
        echo "Using saved KIWI_ENC_PASSWORD"
    else
        prompt_for_input "Enter KIWI_ENC_PASSWORD" "KIWI_ENC_PASSWORD" "true"
        save_config "KIWI_ENC_PASSWORD" "$KIWI_ENC_PASSWORD"
    fi

    # Load GROK_API_KEY
    if has_config "GROK_API_KEY"; then
        GROK_API_KEY=$(load_config "GROK_API_KEY")
        echo "Using saved GROK_API_KEY"
    else
        prompt_for_input "Enter GROK_API_KEY" "GROK_API_KEY" "true"
        save_config "GROK_API_KEY" "$GROK_API_KEY"
    fi
}

# Function to get database passwords
get_db_passwords() {
    echo "Loading database and service configurations..."

    # Load MySQL password
    if has_config "MYSQL_ROOT_PASSWORD"; then
        MYSQL_ROOT_PASSWORD=$(load_config "MYSQL_ROOT_PASSWORD")
        echo "Using saved MySQL root password"
    else
        prompt_for_input "Enter MySQL root password" "MYSQL_ROOT_PASSWORD" "true"
        save_config "MYSQL_ROOT_PASSWORD" "$MYSQL_ROOT_PASSWORD"
    fi

    # Load Redis password
    if has_config "REDIS_PASSWORD"; then
        REDIS_PASSWORD=$(load_config "REDIS_PASSWORD")
        echo "Using saved Redis password"
    else
        prompt_for_input "Enter Redis password" "REDIS_PASSWORD" "true"
        save_config "REDIS_PASSWORD" "$REDIS_PASSWORD"
    fi

    # Load FastDFS hostname
    if has_config "FASTDFS_HOSTNAME"; then
        FASTDFS_HOSTNAME=$(load_config "FASTDFS_HOSTNAME")
        echo "Using saved FastDFS hostname: $FASTDFS_HOSTNAME"
    else
        prompt_for_input "Enter FastDFS hostname (e.g., kiwi-fastdfs)" "FASTDFS_HOSTNAME" "false"
        save_config "FASTDFS_HOSTNAME" "$FASTDFS_HOSTNAME"
    fi

    # Load Elasticsearch root password
    if has_config "ES_ROOT_PASSWORD"; then
        ES_ROOT_PASSWORD=$(load_config "ES_ROOT_PASSWORD")
        echo "Using saved Elasticsearch root password"
    else
        prompt_for_input "Enter Elasticsearch root password" "ES_ROOT_PASSWORD" "true"
        save_config "ES_ROOT_PASSWORD" "$ES_ROOT_PASSWORD"
    fi

    # Load Elasticsearch username
    if has_config "ES_USER_NAME"; then
        ES_USER_NAME=$(load_config "ES_USER_NAME")
        echo "Using saved Elasticsearch username: $ES_USER_NAME"
    else
        prompt_for_input "Enter Elasticsearch additional username" "ES_USER_NAME" "false"
        save_config "ES_USER_NAME" "$ES_USER_NAME"
    fi

    # Load Elasticsearch user password
    if has_config "ES_USER_PASSWORD"; then
        ES_USER_PASSWORD=$(load_config "ES_USER_PASSWORD")
        echo "Using saved Elasticsearch user password"
    else
        prompt_for_input "Enter Elasticsearch additional user password" "ES_USER_PASSWORD" "true"
        save_config "ES_USER_PASSWORD" "$ES_USER_PASSWORD"
    fi
}

# Function to show step selection menu
show_step_menu() {
    echo
    echo "=================================="
    echo "STEP SELECTION MENU"
    echo "=================================="
    echo "Choose setup mode:"
    echo "0. Full automatic setup (default)"
    echo "1. Select specific steps to re-initialize"
    echo "2. Show step status and exit"
    echo
    read -p "Enter your choice (0-2) [default: 0]: " SETUP_MODE

    # Default to 0 if empty
    SETUP_MODE=${SETUP_MODE:-0}

    case $SETUP_MODE in
        1)
            select_steps_to_reinitialize
            ;;
        2)
            show_step_status
            exit 0
            ;;
        0)
            echo "Proceeding with full automatic setup..."
            ;;
        *)
            echo "Invalid choice. Proceeding with full automatic setup..."
            ;;
    esac
}

# Function to select steps to re-initialize
select_steps_to_reinitialize() {
    echo
    echo "=================================="
    echo "SELECT STEPS TO RE-INITIALIZE"
    echo "=================================="
    echo "Available steps:"
    echo " 1. system_update           - System update and package installations"
    echo " 2. docker_install          - Install Docker"
    echo " 3. docker_setup            - Setup Docker alias and test"
    echo " 4. docker_cleanup          - Clean Docker system"
    echo " 5. docker_compose_install  - Install Docker Compose"
    echo " 6. python_install          - Install Python and other packages"
    echo " 7. directories_created     - Create directory structure"
    echo " 8. git_setup               - Clone and setup Git repository"
    echo " 9. hosts_configured        - Configure hosts file"
    echo "10. env_vars_setup          - Setup environment variables"
    echo "11. ytdlp_download          - Download yt-dlp"
    echo "12. mysql_setup             - Setup MySQL"
    echo "13. redis_setup             - Setup Redis"
    echo "14. rabbitmq_setup          - Setup RabbitMQ"
    echo "15. fastdfs_setup           - Setup FastDFS"
    echo "16. maven_lib_install       - Maven library installation"
    echo "17. deployment_setup        - Setup deployment script"
    echo "18. elasticsearch_setup     - Setup Elasticsearch"
    echo "19. ik_tokenizer_install    - Install IK Tokenizer"
    echo "20. nginx_ui_setup          - Setup Nginx and UI"
    echo "21. ALL                     - Re-initialize all steps"
    echo
    echo "Enter step numbers separated by spaces (e.g., '1 3 12' or 'ALL'):"
    read -p "Steps to re-initialize: " SELECTED_STEPS

    if [ -z "$SELECTED_STEPS" ]; then
        echo "No steps selected. Proceeding with normal flow..."
        return
    fi

    # Array of all step names
    declare -a STEP_NAMES=(
        "system_update"
        "docker_install"
        "docker_setup"
        "docker_cleanup"
        "docker_compose_install"
        "python_install"
        "directories_created"
        "git_setup"
        "hosts_configured"
        "env_vars_setup"
        "ytdlp_download"
        "mysql_setup"
        "redis_setup"
        "rabbitmq_setup"
        "fastdfs_setup"
        "maven_lib_install"
        "deployment_setup"
        "elasticsearch_setup"
        "ik_tokenizer_install"
        "nginx_ui_setup"
    )

    if [ "$SELECTED_STEPS" = "ALL" ] || [ "$SELECTED_STEPS" = "21" ]; then
        echo "Re-initializing ALL steps..."
        for step in "${STEP_NAMES[@]}"; do
            force_reinitialize_step "$step"
        done
    else
        for num in $SELECTED_STEPS; do
            if [[ "$num" =~ ^[0-9]+$ ]] && [ "$num" -ge 1 ] && [ "$num" -le 20 ]; then
                step_index=$((num - 1))
                step_name="${STEP_NAMES[$step_index]}"
                force_reinitialize_step "$step_name"
            else
                echo "Warning: Invalid step number '$num' (valid range: 1-20)"
            fi
        done
    fi

    echo
    echo "Selected steps have been marked for re-initialization."
    echo "Proceeding with setup..."
}

# Function to show step status
show_step_status() {
    echo
    echo "=================================="
    echo "CURRENT STEP STATUS"
    echo "=================================="

    declare -a STEP_NAMES=(
        "system_update"
        "docker_install"
        "docker_setup"
        "docker_cleanup"
        "docker_compose_install"
        "python_install"
        "directories_created"
        "git_setup"
        "hosts_configured"
        "env_vars_setup"
        "ytdlp_download"
        "mysql_setup"
        "redis_setup"
        "rabbitmq_setup"
        "fastdfs_setup"
        "maven_lib_install"
        "deployment_setup"
        "elasticsearch_setup"
        "ik_tokenizer_install"
        "nginx_ui_setup"
    )

    declare -a STEP_DESCRIPTIONS=(
        "System update and package installations"
        "Install Docker"
        "Setup Docker alias and test"
        "Clean Docker system"
        "Install Docker Compose"
        "Install Python and other packages"
        "Create directory structure"
        "Clone and setup Git repository"
        "Configure hosts file"
        "Setup environment variables"
        "Download yt-dlp"
        "Setup MySQL"
        "Setup Redis"
        "Setup RabbitMQ"
        "Setup FastDFS"
        "Maven library installation"
        "Setup deployment script"
        "Setup Elasticsearch"
        "Install IK Tokenizer"
        "Setup Nginx and UI"
    )

    echo "Step Status Summary:"
    echo "==================="

    completed_count=0
    total_count=${#STEP_NAMES[@]}

    for i in "${!STEP_NAMES[@]}"; do
        step_name="${STEP_NAMES[$i]}"
        step_desc="${STEP_DESCRIPTIONS[$i]}"
        step_num=$((i + 1))

        if is_step_completed "$step_name"; then
            status="✓ COMPLETED"
            completed_time=$(load_config "${step_name}_completed_at")
            if [ -n "$completed_time" ]; then
                status="$status ($completed_time)"
            fi
            ((completed_count++))
        else
            status="✗ PENDING"
        fi

        printf "%2d. %-25s %s\n" "$step_num" "$step_name" "$status"
        printf "    %s\n" "$step_desc"
        echo
    done

    echo "=================================="
    echo "Progress: $completed_count/$total_count steps completed"
    echo "Progress file: $PROGRESS_FILE"
    echo "Config file: $CONFIG_FILE"
    echo "=================================="
}

# Get environment variables and database passwords first
get_env_vars
get_db_passwords

# Show step selection menu
show_step_menu

echo
echo "Starting setup process..."

# Step 1: System update and package installations
if ! is_step_completed "system_update"; then
    echo "Step 1: Updating system..."
    apt update

    # Record successful completion
    save_config "system_update_status" "completed"
    save_config "system_update_packages" "$(apt list --installed 2>/dev/null | wc -l) packages updated"

    mark_step_completed "system_update"
else
    echo "Step 1: System update already completed, skipping..."
fi

# Step 2: Install Docker
if ! is_step_completed "docker_install"; then
    echo "Step 2: Installing Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
    rm get-docker.sh

    # Add user to docker group
    usermod -aG docker $SCRIPT_USER

    # Start and enable docker service
    systemctl start docker
    systemctl enable docker

    # Record Docker version and status
    DOCKER_VERSION=$(docker --version 2>/dev/null || echo "unknown")
    save_config "docker_version" "$DOCKER_VERSION"
    save_config "docker_service_status" "enabled and started"
    save_config "docker_user_added" "$SCRIPT_USER added to docker group"

    mark_step_completed "docker_install"
else
    echo "Step 2: Docker already installed, skipping..."
fi

# Step 3: Setup Docker alias and test
if ! is_step_completed "docker_setup"; then
    echo "Step 3: Setting up Docker..."

    # Setup Docker alias (using podman if needed)
    if ! run_as_user grep -q "alias docker='podman'" "$SCRIPT_HOME/.bashrc" 2>/dev/null; then
        run_as_user bash -c "echo \"alias docker='podman'\" >> ~/.bashrc"
    fi

    # Test docker (may need newgrp to take effect)
    echo "Testing Docker installation..."
    DOCKER_TEST_STATUS="success"
    if ! run_as_user_home docker version >/dev/null 2>&1; then
        DOCKER_TEST_STATUS="needs_relogin"
        echo "Note: You may need to log out and back in for Docker permissions to take effect"
        echo "Alternatively, run: newgrp docker"
    fi

    # Record Docker setup details
    save_config "docker_alias_added" "podman alias configured"
    save_config "docker_test_status" "$DOCKER_TEST_STATUS"

    mark_step_completed "docker_setup"
else
    echo "Step 3: Docker setup already completed, skipping..."
fi

# Step 3.5: Clean Docker system (after install or skip)
if ! is_step_completed "docker_cleanup"; then
    echo "Step 3.5: Cleaning Docker system..."

    # Check if Docker is accessible
    if docker version >/dev/null 2>&1; then
        echo "Pruning Docker system to clean old images and cache..."

        # Show current Docker usage before cleanup
        DOCKER_USAGE_BEFORE=$(docker system df 2>/dev/null || echo "Unable to check")

        # Perform system prune
        docker system prune -a -f

        # Show usage after cleanup
        DOCKER_USAGE_AFTER=$(docker system df 2>/dev/null || echo "Unable to check")

        # Record cleanup details
        save_config "docker_cleanup_performed" "system prune -a -f executed"
        save_config "docker_usage_before_cleanup" "$DOCKER_USAGE_BEFORE"
        save_config "docker_usage_after_cleanup" "$DOCKER_USAGE_AFTER"

        echo "Docker system cleanup completed."
    else
        echo "Docker not accessible, skipping cleanup. Will retry after permissions are fixed."
        save_config "docker_cleanup_skipped" "Docker not accessible at this time"
    fi

    mark_step_completed "docker_cleanup"
else
    echo "Step 3.5: Docker cleanup already completed, skipping..."
fi

# Step 4: Install Docker Compose
if ! is_step_completed "docker_compose_install"; then
    echo "Step 4: Installing Docker Compose..."

    # Detect architecture
    ARCH=$(uname -m)
    case $ARCH in
        x86_64)
            COMPOSE_ARCH="linux-x86_64"
            ;;
        aarch64|arm64)
            COMPOSE_ARCH="linux-aarch64"
            ;;
        armv7l)
            COMPOSE_ARCH="linux-armv7"
            ;;
        *)
            echo "Unsupported architecture: $ARCH"
            COMPOSE_ARCH="linux-x86_64"  # fallback
            ;;
    esac

    wget https://github.com/docker/compose/releases/download/v2.35.1/docker-compose-$COMPOSE_ARCH -O docker-compose
    mv docker-compose /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose

    # Record Docker Compose details
    COMPOSE_VERSION=$(docker-compose --version 2>/dev/null || echo "unknown")
    save_config "docker_compose_version" "$COMPOSE_VERSION"
    save_config "docker_compose_arch" "$COMPOSE_ARCH"
    save_config "system_architecture" "$ARCH"

    mark_step_completed "docker_compose_install"
else
    echo "Step 4: Docker Compose already installed, skipping..."
fi

# Step 5: Install Python and other packages
if ! is_step_completed "python_install"; then
    echo "Step 5: Installing Python and other packages..."
    apt install python3 python3-pip python3-venv python3-full podman-compose openjdk-17-jdk maven git -y

    # Record installed versions
    PYTHON_VERSION=$(python3 --version 2>/dev/null || echo "unknown")
    PIP_VERSION=$(pip3 --version 2>/dev/null || echo "unknown")
    JAVA_VERSION=$(java -version 2>&1 | head -n1 || echo "unknown")
    MAVEN_VERSION=$(mvn --version 2>/dev/null | head -n1 || echo "unknown")
    GIT_VERSION=$(git --version 2>/dev/null || echo "unknown")

    save_config "python_version" "$PYTHON_VERSION"
    save_config "pip_version" "$PIP_VERSION"
    save_config "java_version" "$JAVA_VERSION"
    save_config "maven_version" "$MAVEN_VERSION"
    save_config "git_version" "$GIT_VERSION"
    save_config "packages_installed" "python3 python3-pip python3-venv python3-full podman-compose openjdk-17-jdk maven git"

    mark_step_completed "python_install"
else
    echo "Step 5: Python and packages already installed, skipping..."
fi

# Step 6: Create directory structure
if ! is_step_completed "directories_created"; then
    echo "Step 6: Creating directory structure..."
    cd "$SCRIPT_HOME"

    run_as_user mkdir -p microservice-kiwi docker storage_data store_path tracker_data
    run_as_user mkdir -p docker/kiwi docker/ui docker/rabbitmq docker/mysql

    cd "$SCRIPT_HOME/docker/kiwi"
    run_as_user mkdir -p auth config crawler eureka gate upms word ai
    run_as_user mkdir -p auth/logs config/logs crawler/logs crawler/tmp eureka/logs gate/logs upms/logs word/logs word/bizTmp word/crawlerTmp word/biz word/crawler ai/logs ai/tmp ai/biz ai/batch

    cd "$SCRIPT_HOME/docker/ui"
    run_as_user mkdir -p dist nginx

    # Record directory structure
    TOTAL_DIRS=$(find "$SCRIPT_HOME" -type d -name "docker" -o -name "microservice-kiwi" -o -name "storage_data" -o -name "store_path" -o -name "tracker_data" | wc -l)
    save_config "directories_created_count" "$TOTAL_DIRS main directories"
    save_config "directory_structure" "microservice-kiwi, docker, storage_data, store_path, tracker_data with subdirectories"

    mark_step_completed "directories_created"
else
    echo "Step 6: Directory structure already created, skipping..."
fi

# Step 7: Clone and setup Git repository
if ! is_step_completed "git_setup"; then
    echo "Step 7: Setting up Git repository..."
    cd "$SCRIPT_HOME/microservice-kiwi/"

    # Check for corrupted git index and fix it
    if [ -d ".git" ]; then
        echo "Checking Git repository integrity..."
        if ! run_as_user_home git status >/dev/null 2>&1; then
            echo "Git repository appears corrupted, cleaning up..."
            run_as_user rm -rf .git
            echo "Removed corrupted .git directory"
            save_config "git_corruption_fixed" "true - removed corrupted .git directory"
        fi
    fi

    if [ ! -d ".git" ]; then
        echo "Initializing fresh Git repository..."
        run_as_user_home git init
        run_as_user_home git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
    fi

    # Ensure remote exists
    if ! run_as_user_home git remote get-url origin >/dev/null 2>&1; then
        run_as_user_home git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
    fi

    echo "Fetching latest code..."
    run_as_user_home git fetch --all
    run_as_user_home git reset --hard origin/master
    run_as_user_home git branch --set-upstream-to=origin/master master 2>/dev/null || true
    run_as_user_home git pull

    # Create symbolic links
    echo "Creating deployment shortcuts..."
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/deployKiwi.sh" "$SCRIPT_HOME/easy-deploy"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" "$SCRIPT_HOME/easy-stop"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/auto_deploy_ui.sh" "$SCRIPT_HOME/easy-deploy-ui"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/checkContainers.sh" "$SCRIPT_HOME/easy-check"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/set_up.sh" "$SCRIPT_HOME/easy-setup"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/clean_set_up.sh" "$SCRIPT_HOME/easy-clean-setup"

    # Make symbolic links executable
    run_as_user chmod 777 "$SCRIPT_HOME/easy-deploy"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-stop"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-deploy-ui"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-check"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-setup"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-clean-setup"

    # Record Git setup details
    GIT_COMMIT_HASH=$(run_as_user_home git rev-parse HEAD 2>/dev/null || echo "unknown")
    GIT_BRANCH=$(run_as_user_home git branch --show-current 2>/dev/null || echo "unknown")
    save_config "git_repository_url" "https://github.com/coding-by-feng/microservice-kiwi.git"
    save_config "git_commit_hash" "$GIT_COMMIT_HASH"
    save_config "git_branch" "$GIT_BRANCH"
    save_config "symbolic_links_created" "easy-deploy, easy-stop, easy-deploy-ui"

    mark_step_completed "git_setup"
else
    echo "Step 7: Git repository already set up, skipping..."
fi

# Step 8: Configure hosts file
if ! is_step_completed "hosts_configured"; then
    echo "Step 8: Configuring hosts file..."

    # Check if hosts entries already exist
    if ! grep -q "fastdfs.fengorz.me" /etc/hosts; then
        tee -a /etc/hosts > /dev/null << EOF
127.0.0.1    fastdfs.fengorz.me
127.0.0.1    kiwi-microservice-local
127.0.0.1    kiwi-microservice
127.0.0.1    kiwi-ui
127.0.0.1    kiwi-eureka
127.0.0.1    kiwi-redis
127.0.0.1    kiwi-rabbitmq
127.0.0.1    kiwi-db
127.0.0.1    kiwi-es
127.0.0.1    kiwi-config
127.0.0.1    kiwi-auth
127.0.0.1    kiwi-upms
127.0.0.1    kiwi-gate
127.0.0.1    kiwi-ai
127.0.0.1    kiwi-crawler
EOF
    fi

    mark_step_completed "hosts_configured"
else
    echo "Step 8: Hosts file already configured, skipping..."
fi

# Step 9: Setup environment variables
if ! is_step_completed "env_vars_setup"; then
    echo "Step 9: Setting up environment variables..."

    # Remove old entries if they exist
    run_as_user sed -i '/export KIWI_ENC_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export GROK_API_KEY=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export DB_IP=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export MYSQL_ROOT_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export REDIS_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export FASTDFS_HOSTNAME=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_ROOT_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_USER_NAME=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_USER_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true

    # Add new entries to .bashrc
    run_as_user bash -c "echo 'export KIWI_ENC_PASSWORD=\"$KIWI_ENC_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export DB_IP=\"127.0.0.1\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export GROK_API_KEY=\"$GROK_API_KEY\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export MYSQL_ROOT_PASSWORD=\"$MYSQL_ROOT_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export REDIS_PASSWORD=\"$REDIS_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export FASTDFS_HOSTNAME=\"$FASTDFS_HOSTNAME\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_ROOT_PASSWORD=\"$ES_ROOT_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_USER_NAME=\"$ES_USER_NAME\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_USER_PASSWORD=\"$ES_USER_PASSWORD\"' >> ~/.bashrc"

    # Record environment variables setup
    save_config "env_vars_added_to_bashrc" "KIWI_ENC_PASSWORD, GROK_API_KEY, DB_IP, MYSQL_ROOT_PASSWORD, REDIS_PASSWORD, FASTDFS_HOSTNAME, ES_ROOT_PASSWORD, ES_USER_NAME, ES_USER_PASSWORD"
    save_config "bashrc_location" "$SCRIPT_HOME/.bashrc"

    echo "Environment variables added to .bashrc and saved to persistent configuration."

    mark_step_completed "env_vars_setup"
else
    echo "Step 9: Environment variables already set up, skipping..."
fi

# Step 10: Download yt-dlp
if ! is_step_completed "ytdlp_download"; then
    echo "Step 10: Downloading yt-dlp..."

    wget https://github.com/yt-dlp/yt-dlp/releases/download/2025.04.30/yt-dlp_linux -O /tmp/yt-dlp_linux
    chmod +x /tmp/yt-dlp_linux

    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/biz/"
    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/batch/"

    rm /tmp/yt-dlp_linux

    mark_step_completed "ytdlp_download"
else
    echo "Step 10: yt-dlp already downloaded, skipping..."
fi

# Step 11: Setup MySQL (MYSQL ONLY VERSION)
if ! is_step_completed "mysql_setup"; then
    echo "Step 11: Setting up MySQL..."

    # Check if port 3306 is already in use
    if netstat -tlnp | grep -q ":3306 "; then
        echo "Warning: Port 3306 is already in use. Checking what's using it:"
        netstat -tlnp | grep ":3306 "
        echo "You may need to stop the conflicting service or use a different port."
    fi

    # Stop and remove existing container if it exists
    docker stop kiwi-mysql 2>/dev/null || true
    docker rm kiwi-mysql 2>/dev/null || true

    # Use MySQL 8.0 for better ARM compatibility
    echo "Pulling MySQL 8.0 image..."
    if docker pull mysql:8.0; then
        echo "Using MySQL 8.0..."

        # Run MySQL container with memory optimization for Raspberry Pi
        docker run -d --name kiwi-mysql \
            -p 3306:3306 \
            -v "$SCRIPT_HOME/docker/mysql:/mysql_tmp" \
            -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
            -e MYSQL_DATABASE="kiwi_db" \
            --restart=unless-stopped \
            mysql:8.0 \
            --innodb-buffer-pool-size=128M \
            --innodb-log-file-size=32M \
            --innodb-flush-method=O_DSYNC \
            --innodb-flush-log-at-trx-commit=2 \
            --max-connections=50 \
            --default-authentication-plugin=mysql_native_password

        save_config "mysql_engine" "MySQL 8.0"
    else
        echo "Failed to pull MySQL 8.0, trying latest..."
        if docker pull mysql:latest; then
            echo "Using MySQL latest..."

            # Run MySQL container
            docker run -d --name kiwi-mysql \
                -p 3306:3306 \
                -v "$SCRIPT_HOME/docker/mysql:/mysql_tmp" \
                -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
                -e MYSQL_DATABASE="kiwi_db" \
                --restart=unless-stopped \
                mysql:latest \
                --default-authentication-plugin=mysql_native_password

            save_config "mysql_engine" "MySQL latest"
        else
            echo "❌ Failed to pull MySQL images"
            echo "Please check your internet connection and Docker installation"
            save_config "mysql_setup_status" "failed - unable to pull images"
            mark_step_completed "mysql_setup"  # Mark as completed to prevent retry loop
            exit 1
        fi
    fi

    # Wait for MySQL to be ready with proper health checking
    echo "Waiting for MySQL to start (this may take up to 3 minutes)..."

    # Wait up to 180 seconds for MySQL to be ready
    MYSQL_READY=false
    for i in {1..36}; do
        echo "Attempt $i/36: Checking MySQL readiness..."
        if check_mysql_ready; then
            echo "✓ MySQL is ready!"
            MYSQL_READY=true
            break
        fi
        echo "MySQL not ready yet, waiting 5 more seconds..."
        sleep 5
    done

    if [ "$MYSQL_READY" = false ]; then
        echo "✗ MySQL failed to start within 3 minutes"
        echo "Checking MySQL logs:"
        docker logs kiwi-mysql --tail 30
        echo
        echo "Troubleshooting steps:"
        echo "1. Check if port 3306 is in use: sudo netstat -tlnp | grep 3306"
        echo "2. Check container status: docker ps -a | grep kiwi-mysql"
        echo "3. View full logs: docker logs kiwi-mysql"
        echo "4. Check system resources: free -h"
        echo "5. Try manual start: docker start kiwi-mysql"

        save_config "mysql_setup_status" "failed - timeout waiting for startup"

        # Ask user if they want to continue
        echo
        read -p "MySQL setup failed. Continue with other steps? (y/N): " CONTINUE_SETUP
        if [[ ! "$CONTINUE_SETUP" =~ ^[Yy]$ ]]; then
            echo "Setup aborted. You can re-run the script later to retry."
            exit 1
        fi
    else
        # Verify database creation
        echo "Verifying database creation..."
        if docker exec kiwi-mysql mysql -h localhost -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;" | grep -q "kiwi_db"; then
            echo "✓ Database 'kiwi_db' confirmed to exist"
        else
            echo "Creating database manually..."
            docker exec kiwi-mysql mysql -h localhost -u root -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS kiwi_db;" || {
                echo "⚠ Database creation failed, but continuing (may already exist)"
            }
        fi

        # Check for and restore database backup if it exists
        echo "Checking for database backup to restore..."
        if [ -f "$SCRIPT_HOME/docker/mysql/kiwi-db.sql" ]; then
            echo "Found kiwi-db.sql backup file, restoring database..."

            echo "Restoring database from backup..."
            if docker exec kiwi-mysql sh -c "mysql -h localhost -u root -p'$MYSQL_ROOT_PASSWORD' kiwi_db < /mysql_tmp/kiwi-db.sql"; then
                echo "✓ Basic database backup restored successfully"
                save_config "mysql_backup_restored" "kiwi-db.sql restored successfully"
                save_config "mysql_backup_file" "$SCRIPT_HOME/docker/mysql/kiwi-db.sql"

                # Also restore YTB table if available
                if [ -f "$SCRIPT_HOME/microservice-kiwi/kiwi-sql/ytb_table_initialize.sql" ]; then
                    cp "$SCRIPT_HOME/microservice-kiwi/kiwi-sql/ytb_table_initialize.sql" "$SCRIPT_HOME/docker/mysql/"
                    if docker exec kiwi-mysql sh -c "mysql -h localhost -u root -p'$MYSQL_ROOT_PASSWORD' kiwi_db < /mysql_tmp/ytb_table_initialize.sql"; then
                        echo "✓ YTB database backup restored successfully"
                        save_config "mysql_ytb_backup_restored" "ytb_table_initialize.sql restored successfully"
                    else
                        echo "⚠ Failed to restore YTB table backup"
                    fi
                fi
            else
                echo "⚠ Failed to restore database backup automatically"
                echo "You can manually restore using:"
                echo "docker exec -i kiwi-mysql mysql -u root -p'$MYSQL_ROOT_PASSWORD' kiwi_db < $SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                save_config "mysql_backup_restored" "failed - manual restoration required"
            fi
        else
            echo "No kiwi-db.sql backup file found, starting with empty database"
            save_config "mysql_backup_restored" "no backup file found"
        fi

        # Record MySQL setup details
        MYSQL_CONTAINER_ID=$(docker ps -q -f name=kiwi-mysql)
        MYSQL_IMAGE_INFO=$(docker inspect kiwi-mysql --format='{{.Config.Image}}' 2>/dev/null || echo "unknown")
        save_config "mysql_container_id" "$MYSQL_CONTAINER_ID"
        save_config "mysql_image_info" "$MYSQL_IMAGE_INFO"
        save_config "mysql_database_created" "kiwi_db"
        save_config "mysql_port" "3306"
        save_config "mysql_volume_mount" "$SCRIPT_HOME/docker/mysql:/mysql_tmp"
        save_config "mysql_setup_status" "completed successfully"

        echo "✓ MySQL setup completed. Database 'kiwi_db' created and ready."
    fi

    mark_step_completed "mysql_setup"
else
    echo "Step 11: MySQL already set up, skipping..."
    check_and_start_container "kiwi-mysql" "mysql_setup"

    # Check if backup restoration is needed (even on skip)
    if ! has_config "mysql_backup_restored" || [ "$(load_config 'mysql_backup_restored')" = "no backup file found" ]; then
        echo "Checking for database backup to restore..."
        if [ -f "$SCRIPT_HOME/docker/mysql/kiwi-db.sql" ]; then
            echo "Found kiwi-db.sql backup file, restoring database..."

            # Ensure MySQL is running before attempting restore
            if docker ps --format '{{.Names}}' | grep -q "^kiwi-mysql$"; then
                # Wait a moment for MySQL to be fully ready
                echo "Waiting for MySQL to be ready for backup restoration..."
                sleep 10

                # Check if MySQL is responding
                if check_mysql_ready; then
                    echo "Restoring database from backup..."
                    if docker exec kiwi-mysql sh -c "mysql -h localhost -u root -p'$MYSQL_ROOT_PASSWORD' kiwi_db < /mysql_tmp/kiwi-db.sql"; then
                        echo "✓ Database backup restored successfully"
                        save_config "mysql_backup_restored" "kiwi-db.sql restored successfully"
                        save_config "mysql_backup_file" "$SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                    else
                        echo "⚠ Failed to restore database backup"
                        echo "You can manually restore using:"
                        echo "docker exec -i kiwi-mysql mysql -u root -p'$MYSQL_ROOT_PASSWORD' kiwi_db < $SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                        save_config "mysql_backup_restored" "failed - manual restoration required"
                    fi
                else
                    echo "⚠ MySQL container not responding, cannot restore backup"
                    save_config "mysql_backup_restored" "failed - MySQL not responding"
                fi
            else
                echo "⚠ MySQL container not running, cannot restore backup"
                save_config "mysql_backup_restored" "failed - container not running"
            fi
        fi
    else
        echo "Database backup already processed: $(load_config 'mysql_backup_restored')"
    fi
fi

# Step 12: Setup Redis
if ! is_step_completed "redis_setup"; then
    echo "Step 12: Setting up Redis..."

    # Pull Redis image
    docker pull redis:latest

    # Stop and remove existing container if it exists
    docker stop kiwi-redis 2>/dev/null || true
    docker rm kiwi-redis 2>/dev/null || true

    # Run Redis container
    docker run -itd --name kiwi-redis -p 6379:6379 \
        redis --requirepass "$REDIS_PASSWORD"

    echo "Redis setup completed with password protection."

    mark_step_completed "redis_setup"
else
    echo "Step 12: Redis already set up, skipping..."
    check_and_start_container "kiwi-redis" "redis_setup"
fi

# Step 13: Setup RabbitMQ
if ! is_step_completed "rabbitmq_setup"; then
    echo "Step 13: Setting up RabbitMQ..."

    # Pull RabbitMQ image
    docker pull rabbitmq:management

    # Stop and remove existing container if it exists
    docker stop kiwi-rabbit 2>/dev/null || true
    docker rm kiwi-rabbit 2>/dev/null || true

    # Run RabbitMQ container
    docker run -d --hostname kiwi-rabbit \
        -v "$SCRIPT_HOME/docker/rabbitmq:/tmp" \
        --name kiwi-rabbit \
        --net=host \
        rabbitmq:management

    echo "RabbitMQ setup completed."
    echo "Management UI will be available at: http://localhost:15672"
    echo "Default credentials: guest/guest"

    mark_step_completed "rabbitmq_setup"
else
    echo "Step 13: RabbitMQ already set up, skipping..."
    check_and_start_container "kiwi-rabbit" "rabbitmq_setup"
fi

# Step 14: Setup FastDFS
if ! is_step_completed "fastdfs_setup"; then
    echo "Step 14: Setting up FastDFS..."

    # Pull FastDFS image
    docker pull fyclinux/fastdfs-arm64:6.04

    # Stop and remove existing containers if they exist
    docker stop tracker storage 2>/dev/null || true
    docker rm tracker storage 2>/dev/null || true

    # Run tracker
    docker run -ti -d \
        --name tracker \
        --net=host \
        fyclinux/fastdfs-arm64:6.04 \
        tracker

    # Wait for tracker to start
    sleep 10

    # Run storage
    docker run -ti -d \
        --name storage \
        -v "$SCRIPT_HOME/storage_data:/fastdfs/storage/data" \
        -v "$SCRIPT_HOME/store_path:/fastdfs/store_path" \
        --net=host \
        -e TRACKER_SERVER="$FASTDFS_HOSTNAME:22122" \
        fyclinux/fastdfs-arm64:6.04 \
        storage

    # Wait for storage to start
    sleep 15

    # Configure storage.conf
    echo "Configuring FastDFS storage..."
    docker exec storage sed -i "s/tracker_server=.*/tracker_server=$FASTDFS_HOSTNAME:22122/" /etc/fdfs/storage.conf

    # Restart storage container to apply configuration
    docker restart storage

    echo "FastDFS setup completed."
    echo "Tracker and Storage containers are running."

    mark_step_completed "fastdfs_setup"
else
    echo "Step 14: FastDFS already set up, skipping..."
    check_and_start_container "tracker" "fastdfs_setup"
    check_and_start_container "storage" "fastdfs_setup"
fi

# Step 15: Maven library installation
if ! is_step_completed "maven_lib_install"; then
    echo "Step 15: Installing Maven library..."

    cd "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"

    # Install voicerss_tts.jar if it exists
    if [ -f "voicerss_tts.jar" ]; then
        run_as_user_home mvn install:install-file \
            -Dfile=voicerss_tts.jar \
            -DgroupId=voicerss \
            -DartifactId=tts \
            -Dversion=2.0 \
            -Dpackaging=jar
        echo "Maven library installation completed."

        # Record successful installation
        save_config "maven_lib_installed" "voicerss_tts.jar installed successfully"
        save_config "maven_lib_path" "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
    else
        echo "Warning: voicerss_tts.jar not found in lib directory"
        echo "Expected location: $SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib/voicerss_tts.jar"

        # Check if the directory exists
        if [ ! -d "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib" ]; then
            echo "Directory doesn't exist. Creating directory structure..."
            run_as_user mkdir -p "$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"
            echo "Please place voicerss_tts.jar in the lib directory and re-run this step"
        fi

        save_config "maven_lib_installed" "voicerss_tts.jar not found - manual installation required"
    fi

    mark_step_completed "maven_lib_install"
else
    echo "Step 15: Maven library already installed, skipping..."
fi

# Step 16: Setup deployment script
if ! is_step_completed "deployment_setup"; then
    echo "Step 16: Setting up deployment..."

    cd "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker"

    # Copy deployment script if it doesn't already exist as symlink
    if [ ! -L "$SCRIPT_HOME/deployKiwi.sh" ]; then
        run_as_user cp deployKiwi.sh "$SCRIPT_HOME/"
        run_as_user chmod 777 "$SCRIPT_HOME/deployKiwi.sh"
    fi

    echo "Deployment script setup completed."

    mark_step_completed "deployment_setup"
else
    echo "Step 16: Deployment already set up, skipping..."
fi

# Step 17: Setup Elasticsearch
if ! is_step_completed "elasticsearch_setup"; then
    echo "Step 17: Setting up Elasticsearch..."

    # Pull Elasticsearch image
    docker pull elasticsearch:7.17.9

    # Stop and remove existing container if it exists
    docker stop kiwi-es 2>/dev/null || true
    docker rm kiwi-es 2>/dev/null || true

    # Remove existing volumes if they exist
    docker volume rm es_config es_data 2>/dev/null || true

    # Run Elasticsearch container
    docker run -d -p 9200:9200 -p 9300:9300 --hostname kiwi-es \
        -e "discovery.type=single-node" \
        -e "xpack.security.enabled=true" \
        -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
        --name kiwi-es \
        -v es_config:/usr/share/elasticsearch/config \
        -v es_data:/usr/share/elasticsearch/data \
        elasticsearch:7.17.9

    # Wait for Elasticsearch to start
    echo "Waiting for Elasticsearch to start..."
    sleep 60

    # Create users
    echo "Creating Elasticsearch users..."
    docker exec kiwi-es bash -c "echo '$ES_ROOT_PASSWORD' | elasticsearch-users useradd root -r superuser -p"
    docker exec kiwi-es bash -c "echo '$ES_USER_PASSWORD' | elasticsearch-users useradd $ES_USER_NAME -r superuser -p"

    # Wait a bit more for users to be created
    sleep 10

    # Create kiwi_vocabulary index
    echo "Creating kiwi_vocabulary index..."
    INDEX_CREATION_RESULT=$(docker exec kiwi-es curl -X PUT "localhost:9200/kiwi_vocabulary" \
        -u "root:$ES_ROOT_PASSWORD" \
        -H "Content-Type: application/json" \
        -d '{
            "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0
            }
        }' 2>/dev/null || echo "failed")

    # Record Elasticsearch setup details
    ES_CONTAINER_ID=$(docker ps -q -f name=kiwi-es)
    ES_IMAGE_INFO=$(docker images elasticsearch --format "{{.Repository}}:{{.Tag}} {{.Size}}" | grep "7.17.9" | head -n1)
    save_config "elasticsearch_container_id" "$ES_CONTAINER_ID"
    save_config "elasticsearch_image_info" "$ES_IMAGE_INFO"
    save_config "elasticsearch_users_created" "root, $ES_USER_NAME"
    save_config "elasticsearch_index_created" "kiwi_vocabulary"
    save_config "elasticsearch_index_creation_result" "$INDEX_CREATION_RESULT"
    save_config "elasticsearch_ports" "9200, 9300"
    save_config "elasticsearch_memory_config" "512m heap size"

    echo "Elasticsearch setup completed."

    mark_step_completed "elasticsearch_setup"
else
    echo "Step 17: Elasticsearch already set up, skipping..."
    check_and_start_container "kiwi-es" "elasticsearch_setup"
fi

# Step 18: Install IK Tokenizer
if ! is_step_completed "ik_tokenizer_install"; then
    echo "Step 18: Installing IK Tokenizer..."

    # Install IK plugin
    docker exec kiwi-es elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/7.17.9 --batch

    # Restart Elasticsearch
    docker restart kiwi-es

    echo "Waiting for Elasticsearch to restart..."
    sleep 30

    echo "IK Tokenizer installation completed."

    mark_step_completed "ik_tokenizer_install"
else
    echo "Step 18: IK Tokenizer already installed, skipping..."
fi

# Step 19: Setup Nginx and UI
if ! is_step_completed "nginx_ui_setup"; then
    echo "Step 19: Setting up Nginx and UI..."

    # Pull Nginx image
    docker pull nginx

    # Stop and remove existing container if it exists
    docker stop kiwi-ui 2>/dev/null || true
    docker rm kiwi-ui 2>/dev/null || true

    # Build Kiwi UI image
    cd "$SCRIPT_HOME"
    docker build -f "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/Dockerfile" -t kiwi-ui:1.0 "$SCRIPT_HOME/docker/ui/"

    # Run Kiwi UI container
    docker run -d \
        -v "$SCRIPT_HOME/docker/ui/dist/:/usr/share/nginx/html" \
        --net=host \
        --name=kiwi-ui \
        -it kiwi-ui:1.0

    echo "Nginx and UI setup completed."
    echo "UI will be available at: http://localhost:80"

    mark_step_completed "nginx_ui_setup"
else
    echo "Step 19: Nginx and UI already set up, skipping..."
    check_and_start_container "kiwi-ui" "nginx_ui_setup"
fi

echo
echo "=================================="
echo "Setup completed successfully!"
echo "=================================="
echo "Target user: $SCRIPT_USER"
echo "Home directory: $SCRIPT_HOME"
echo
echo "Available shortcuts:"
echo "  $SCRIPT_HOME/easy-deploy     - Deploy Kiwi services"
echo "  $SCRIPT_HOME/easy-stop       - Stop all services"
echo "  $SCRIPT_HOME/easy-deploy-ui  - Deploy UI"
echo "  $SCRIPT_HOME/easy-check      - Check container status"
echo "  $SCRIPT_HOME/easy-setup      - Re-run setup"
echo "  $SCRIPT_HOME/easy-clean-setup - Clean and re-setup"
echo
echo "Environment variables set:"
echo "  KIWI_ENC_PASSWORD = [HIDDEN]"
echo "  GROK_API_KEY      = [HIDDEN]"
echo "  DB_IP             = 127.0.0.1"
echo "  MYSQL_ROOT_PASSWORD = [HIDDEN]"
echo "  REDIS_PASSWORD    = [HIDDEN]"
echo "  FASTDFS_HOSTNAME  = $FASTDFS_HOSTNAME"
echo "  ES_ROOT_PASSWORD  = [HIDDEN]"
echo "  ES_USER_NAME      = $ES_USER_NAME"
echo "  ES_USER_PASSWORD  = [HIDDEN]"
echo
echo "IMPORTANT NOTES:"
echo "1. Please log out and back in (or run 'newgrp docker') to apply Docker group permissions"
echo "2. Run 'source ~/.bashrc' to apply environment changes"
echo "3. Progress is saved in: $PROGRESS_FILE"
echo "4. Configuration is saved in: $CONFIG_FILE"
echo "5. To reset progress: rm $PROGRESS_FILE"
echo "6. To reset saved inputs: rm $CONFIG_FILE"
echo "7. To reset everything: rm $PROGRESS_FILE $CONFIG_FILE"
echo "8. To re-run specific steps: sudo ./$(basename $0) and choose option 1"
echo
echo "CONTAINER STATUS CHECK:"
echo "======================"

# Final container health check - check all containers regardless of step completion status
CONTAINERS=("kiwi-mysql" "kiwi-redis" "kiwi-rabbit" "tracker" "storage" "kiwi-es" "kiwi-ui")
RUNNING_COUNT=0
STOPPED_COUNT=0
MISSING_COUNT=0
TOTAL_COUNT=${#CONTAINERS[@]}

echo "Checking all expected containers..."

# Get list of running containers and all containers with error handling
echo "Fetching container lists..."
RUNNING_CONTAINERS=$(docker ps --format '{{.Names}}' 2>/dev/null)
ALL_CONTAINERS=$(docker ps -a --format '{{.Names}}' 2>/dev/null)

for container in "${CONTAINERS[@]}"; do
    if echo "$ALL_CONTAINERS" | grep -q "^${container}$"; then
        if echo "$RUNNING_CONTAINERS" | grep -q "^${container}$"; then
            echo "✓ $container - RUNNING"
            ((RUNNING_COUNT++))
        else
            echo "⚠ $container - STOPPED"
            ((STOPPED_COUNT++))
        fi
    else
        echo "✗ $container - MISSING"
        ((MISSING_COUNT++))
    fi
done

echo
echo "CONTAINER SUMMARY:"
echo "  Running: $RUNNING_COUNT/$TOTAL_COUNT"
echo "  Stopped: $STOPPED_COUNT/$TOTAL_COUNT"
echo "  Missing: $MISSING_COUNT/$TOTAL_COUNT"
echo
echo "USEFUL COMMANDS:"
echo "  Check all containers:     docker ps -a"
echo "  Check running only:       docker ps"
echo "  View container logs:      docker logs <container-name>"
echo "  Start stopped container:  docker start <container-name>"
echo "  Stop running container:   docker stop <container-name>"
echo "  Restart container:        docker restart <container-name>"
echo
echo "SERVICES:"
echo "- MySQL: Container 'kiwi-mysql' on port 3306"
echo "- Redis: Container 'kiwi-redis' on port 6379"
echo "- RabbitMQ: Container 'kiwi-rabbit' with management UI on port 15672"
echo "- FastDFS: Containers 'tracker' and 'storage' on port 22122"
echo "- Elasticsearch: Container 'kiwi-es' on ports 9200/9300 with IK tokenizer"
echo "- Nginx/UI: Container 'kiwi-ui' on port 80"
echo
echo "WEB INTERFACES:"
echo "- RabbitMQ Management: http://localhost:15672 (guest/guest)"
echo "- Elasticsearch: http://localhost:9200 (root/[your_password])"
echo "- Kiwi UI: http://localhost:80"
echo
echo "To check container status: docker ps"
echo "To view container logs: docker logs <container-name>"
echo "To re-run this script with step selection: sudo ./$(basename $0)"
echo "=================================="

# Final step: Show Docker status
echo "Docker service status:"
systemctl status docker --no-pager -l || true