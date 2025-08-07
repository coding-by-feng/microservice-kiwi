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

# Function to check MySQL readiness
check_mysql_ready() {
    docker ps -a --filter "name=kiwi-mysql" --format "{{.Names}}" | grep -q "^kiwi-mysql$"
    return $?
}

# Function to get sudo user configuration
get_sudo_user_config() {
    echo "Loading sudo user configuration..."

    # Load sudo username
    if has_config "SUDO_USERNAME"; then
        SUDO_USERNAME=$(load_config "SUDO_USERNAME")
        echo "Using saved sudo username: $SUDO_USERNAME"
    else
        # Default to current script user
        DEFAULT_USER="$SCRIPT_USER"
        read -p "Enter username to grant sudo privileges [$DEFAULT_USER]: " input_username
        SUDO_USERNAME=${input_username:-$DEFAULT_USER}
        save_config "SUDO_USERNAME" "$SUDO_USERNAME"
    fi
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

# Function to get database passwords and network configuration
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
        prompt_for_input "Enter FastDFS hostname (e.g., fastdfs.fengorz.me)" "FASTDFS_HOSTNAME" "false"
        save_config "FASTDFS_HOSTNAME" "$FASTDFS_HOSTNAME"
    fi

    # Load FastDFS Non-Local IP
    if has_config "FASTDFS_NON_LOCAL_IP"; then
        FASTDFS_NON_LOCAL_IP=$(load_config "FASTDFS_NON_LOCAL_IP")
        echo "Using saved FastDFS Non-Local IP: $FASTDFS_NON_LOCAL_IP"
    else
        echo "FastDFS Non-Local IP is used specifically for: fastdfs.fengorz.me (can be different from Infrastructure IP)"
        prompt_for_input "Enter FastDFS Non-Local IP for fastdfs.fengorz.me (default: same as Infrastructure IP)" "input_fastdfs_ip" "false"
        # If empty, we'll set it to INFRASTRUCTURE_IP later after it's loaded
        FASTDFS_NON_LOCAL_IP=${input_fastdfs_ip}
        save_config "FASTDFS_NON_LOCAL_IP" "$FASTDFS_NON_LOCAL_IP"
    fi

    # Load Infrastructure IP
    if has_config "INFRASTRUCTURE_IP"; then
        INFRASTRUCTURE_IP=$(load_config "INFRASTRUCTURE_IP")
        echo "Using saved Infrastructure IP: $INFRASTRUCTURE_IP"
    else
        echo "Infrastructure IP is used for: kiwi-ui, kiwi-redis, kiwi-rabbitmq, kiwi-db, kiwi-fastdfs, kiwi-es"
        prompt_for_input "Enter Infrastructure IP (default: 127.0.0.1)" "input_ip" "false"
        INFRASTRUCTURE_IP=${input_ip:-127.0.0.1}
        save_config "INFRASTRUCTURE_IP" "$INFRASTRUCTURE_IP"
    fi

    # Set FastDFS Non-Local IP to Infrastructure IP if it wasn't specified
    if [ -z "$FASTDFS_NON_LOCAL_IP" ]; then
        FASTDFS_NON_LOCAL_IP="$INFRASTRUCTURE_IP"
        save_config "FASTDFS_NON_LOCAL_IP" "$FASTDFS_NON_LOCAL_IP"
        echo "FastDFS Non-Local IP set to Infrastructure IP: $FASTDFS_NON_LOCAL_IP"
    fi

    # Load Service IP
    if has_config "SERVICE_IP"; then
        SERVICE_IP=$(load_config "SERVICE_IP")
        echo "Using saved Service IP: $SERVICE_IP"
    else
        echo "Service IP is used for: kiwi-microservice-local, kiwi-microservice, kiwi-eureka, kiwi-config, kiwi-auth, kiwi-upms, kiwi-gate, kiwi-ai, kiwi-crawler"
        prompt_for_input "Enter Service IP (default: 127.0.0.1)" "input_ip" "false"
        SERVICE_IP=${input_ip:-127.0.0.1}
        save_config "SERVICE_IP" "$SERVICE_IP"
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
    echo " 1. sudo_user_setup         - Configure sudo privileges for user"
    echo " 2. system_update           - System update and package installations"
    echo " 3. docker_install          - Install Docker"
    echo " 4. docker_setup            - Setup Docker alias and test"
    echo " 5. docker_cleanup          - Clean Docker system"
    echo " 6. docker_compose_install  - Install Docker Compose"
    echo " 7. python_install          - Install Python and other packages"
    echo " 8. maven_config            - Configure Maven settings"
    echo " 9. directories_created     - Create directory structure"
    echo "10. git_setup               - Clone and setup Git repository"
    echo "11. hosts_configured        - Configure hosts file"
    echo "12. env_vars_setup          - Setup environment variables"
    echo "13. ytdlp_download          - Download yt-dlp"
    echo "14. mysql_setup             - Setup MySQL"
    echo "15. redis_setup             - Setup Redis"
    echo "16. rabbitmq_setup          - Setup RabbitMQ"
    echo "17. fastdfs_setup           - Setup FastDFS"
    echo "18. maven_lib_install       - Maven library installation"
    echo "19. deployment_setup        - Setup deployment script"
    echo "20. elasticsearch_setup     - Setup Elasticsearch"
    echo "21. ik_tokenizer_install    - Install IK Tokenizer"
    echo "22. nginx_ui_setup          - Setup Nginx and UI"
    echo "23. ALL                     - Re-initialize all steps"
    echo
    echo "Enter step numbers separated by spaces (e.g., '1 3 12' or 'ALL'):"
    read -p "Steps to re-initialize: " SELECTED_STEPS

    if [ -z "$SELECTED_STEPS" ]; then
        echo "No steps selected. Proceeding with normal flow..."
        return
    fi

    # Array of all step names
    declare -a STEP_NAMES=(
        "sudo_user_setup"
        "system_update"
        "docker_install"
        "docker_setup"
        "docker_cleanup"
        "docker_compose_install"
        "python_install"
        "maven_config"
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

    if [ "$SELECTED_STEPS" = "ALL" ] || [ "$SELECTED_STEPS" = "23" ]; then
        echo "Re-initializing ALL steps..."
        for step in "${STEP_NAMES[@]}"; do
            force_reinitialize_step "$step"
        done
    else
        for num in $SELECTED_STEPS; do
            if [[ "$num" =~ ^[0-9]+$ ]] && [ "$num" -ge 1 ] && [ "$num" -le 22 ]; then
                step_index=$((num - 1))
                step_name="${STEP_NAMES[$step_index]}"
                force_reinitialize_step "$step_name"
            else
                echo "Warning: Invalid step number '$num' (valid range: 1-22)"
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
        "sudo_user_setup"
        "system_update"
        "docker_install"
        "docker_setup"
        "docker_cleanup"
        "docker_compose_install"
        "python_install"
        "maven_config"
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
        "Configure sudo privileges for user"
        "System update and package installations"
        "Install Docker"
        "Setup Docker alias and test"
        "Clean Docker system"
        "Install Docker Compose"
        "Install Python and other packages"
        "Configure Maven settings"
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

# Get sudo user configuration first
get_sudo_user_config

# Get environment variables and database passwords
get_env_vars
get_db_passwords

# Show step selection menu
show_step_menu

echo
echo "Starting setup process..."

# Step 1: Configure sudo privileges for user
if ! is_step_completed "sudo_user_setup"; then
    echo "Step 1: Configuring sudo privileges for user: $SUDO_USERNAME"

    # Check if user exists
    if ! id "$SUDO_USERNAME" >/dev/null 2>&1; then
        echo "Error: User '$SUDO_USERNAME' does not exist on this system"
        echo "Available users:"
        getent passwd | grep -E '/home|/Users' | cut -d: -f1 | head -10
        echo "Please create the user first or specify a different username"
        exit 1
    fi

    # Check if user is already in sudo group
    if groups "$SUDO_USERNAME" | grep -q "\bsudo\b"; then
        echo "User '$SUDO_USERNAME' is already in the sudo group"
        save_config "sudo_setup_method" "already_in_sudo_group"
    else
        echo "Adding user '$SUDO_USERNAME' to sudo group..."
        usermod -aG sudo "$SUDO_USERNAME"
        save_config "sudo_setup_method" "added_to_sudo_group"
    fi

    # Also add specific sudoers entry for extra safety
    SUDOERS_LINE="$SUDO_USERNAME ALL=(ALL:ALL) ALL"

    # Check if entry already exists
    if sudo grep -Fxq "$SUDOERS_LINE" /etc/sudoers; then
        echo "Sudoers entry already exists for user '$SUDO_USERNAME'"
        save_config "sudoers_entry_added" "already_exists"
    else
        echo "Adding sudoers entry for user '$SUDO_USERNAME'..."

        # Create a temporary sudoers file to validate syntax
        TEMP_SUDOERS=$(mktemp)
        cp /etc/sudoers "$TEMP_SUDOERS"
        echo "$SUDOERS_LINE" >> "$TEMP_SUDOERS"

        # Validate the sudoers file
        if visudo -c -f "$TEMP_SUDOERS" >/dev/null 2>&1; then
            echo "$SUDOERS_LINE" >> /etc/sudoers
            echo "✓ Sudoers entry added successfully"
            save_config "sudoers_entry_added" "success"
        else
            echo "✗ Error: Invalid sudoers syntax, entry not added"
            save_config "sudoers_entry_added" "failed_validation"
        fi

        # Clean up temporary file
        rm -f "$TEMP_SUDOERS"
    fi

    # Record sudo configuration details
    save_config "sudo_username_configured" "$SUDO_USERNAME"
    save_config "sudo_groups" "$(groups $SUDO_USERNAME 2>/dev/null || echo 'unknown')"

    echo "✓ Sudo privileges configured for user: $SUDO_USERNAME"
    echo "  - Added to sudo group: $(groups $SUDO_USERNAME | grep -q sudo && echo 'Yes' || echo 'No')"
    echo "  - Sudoers entry added: $(load_config 'sudoers_entry_added')"
    echo "  - User may need to log out and back in for changes to take effect"

    mark_step_completed "sudo_user_setup"
else
    echo "Step 1: Sudo user setup already completed, skipping..."
    echo "  - Configured user: $(load_config 'sudo_username_configured')"
    echo "  - Setup method: $(load_config 'sudo_setup_method')"
fi

# Step 2: System update and package installations
if ! is_step_completed "system_update"; then
    echo "Step 2: Updating system and installing essential packages..."
    apt update
    
    # Install essential packages including curl, wget, and other utilities
    echo "Installing essential packages (curl, wget, ca-certificates, gnupg, lsb-release)..."
    apt install -y curl wget ca-certificates gnupg lsb-release

    # Record successful completion
    save_config "system_update_status" "completed"
    save_config "system_update_packages" "$(apt list --installed 2>/dev/null | wc -l) packages updated"

    mark_step_completed "system_update"
else
    echo "Step 2: System update already completed, skipping..."
fi

# Step 3: Install Docker
if ! is_step_completed "docker_install"; then
    echo "Step 3: Installing Docker..."
    
    # Check if curl is available, install if missing
    if ! command -v curl &> /dev/null; then
        echo "curl not found, installing essential packages..."
        apt update
        apt install -y curl wget ca-certificates gnupg lsb-release
    fi
    
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
    echo "Step 3: Docker already installed, skipping..."
fi

# Step 4: Setup Docker alias and test
if ! is_step_completed "docker_setup"; then
    echo "Step 4: Setting up Docker..."

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
    echo "Step 4: Docker setup already completed, skipping..."
fi

# Step 5: Clean Docker system (after install or skip)
if ! is_step_completed "docker_cleanup"; then
    echo "Step 5: Cleaning Docker system..."

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
    echo "Step 5: Docker cleanup already completed, skipping..."
fi

# Step 6: Install Docker Compose
if ! is_step_completed "docker_compose_install"; then
    echo "Step 6: Installing Docker Compose..."

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
    echo "Step 6: Docker Compose already installed, skipping..."
fi

# Step 7: Install Python and other packages
if ! is_step_completed "python_install"; then
    echo "Step 7: Installing Python and other packages..."
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
    echo "Step 7: Python and packages already installed, skipping..."
fi

# Step 8: Configure Maven settings
if ! is_step_completed "maven_config"; then
    echo "Step 8: Configuring Maven settings..."

    # Create .m2 directory if it doesn't exist
    run_as_user mkdir -p "$SCRIPT_HOME/.m2"

    # Create minimal Maven settings.xml,
    run_as_user tee "$SCRIPT_HOME/.m2/settings.xml" > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>~/.m2/repository</localRepository>
</settings>
EOF

    echo "✓ Created minimal Maven settings.xml"

    # Record Maven configuration details
    save_config "maven_settings_file" "$SCRIPT_HOME/.m2/settings.xml"
    save_config "maven_local_repository" "$SCRIPT_HOME/.m2/repository"
    save_config "maven_config_status" "minimal settings.xml created"

    mark_step_completed "maven_config"
else
    echo "Step 8: Maven already configured, skipping..."
fi

# Step 9: Create directory structure
if ! is_step_completed "directories_created"; then
    echo "Step 9: Creating directory structure..."
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
    echo "Step 9: Directory structure already created, skipping..."
fi

# Step 10: Clone and setup Git repository
if ! is_step_completed "git_setup"; then
    echo "Step 10: Setting up Git repository..."
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
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/initial_ui.sh" "$SCRIPT_HOME/easy-ui-initial"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/checkContainers.sh" "$SCRIPT_HOME/easy-check"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/set_up.sh" "$SCRIPT_HOME/easy-setup"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/clean_set_up.sh" "$SCRIPT_HOME/easy-clean-setup"

    # Make symbolic links executable
    run_as_user chmod 777 "$SCRIPT_HOME/easy-deploy"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-stop"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-deploy-ui"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-ui-initial"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-check"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-setup"
    run_as_user chmod 777 "$SCRIPT_HOME/easy-clean-setup"

    # Record Git setup details
    GIT_COMMIT_HASH=$(run_as_user_home git rev-parse HEAD 2>/dev/null || echo "unknown")
    GIT_BRANCH=$(run_as_user_home git branch --show-current 2>/dev/null || echo "unknown")
    save_config "git_repository_url" "https://github.com/coding-by-feng/microservice-kiwi.git"
    save_config "git_commit_hash" "$GIT_COMMIT_HASH"
    save_config "git_branch" "$GIT_BRANCH"
    save_config "symbolic_links_created" "easy-deploy, easy-stop, easy-deploy-ui, easy-ui-initial"

    mark_step_completed "git_setup"
else
    echo "Step 10: Git repository already set up, skipping..."
fi

# Step 11: Configure hosts file with separate IPs
if ! is_step_completed "hosts_configured"; then
    echo "Step 11: Configuring hosts file..."
    echo "Using FastDFS Non-Local IP: $FASTDFS_NON_LOCAL_IP for fastdfs.fengorz.me"
    echo "Using Infrastructure IP: $INFRASTRUCTURE_IP for other infrastructure services"
    echo "Using Service IP: $SERVICE_IP for microservices"

    # Backup existing hosts file
    cp /etc/hosts /etc/hosts.backup.$(date +%Y%m%d_%H%M%S)

    # Remove existing Kiwi entries if they exist
    sed -i '/# Kiwi Infrastructure Services/,/# End Kiwi Services/d' /etc/hosts

    # Add new entries with proper categorization and separate FastDFS IP
    tee -a /etc/hosts > /dev/null << EOF

# Kiwi Infrastructure Services
$FASTDFS_NON_LOCAL_IP    fastdfs.fengorz.me
$INFRASTRUCTURE_IP    kiwi-ui
$INFRASTRUCTURE_IP    kiwi-redis
$INFRASTRUCTURE_IP    kiwi-rabbitmq
$INFRASTRUCTURE_IP    kiwi-db
$INFRASTRUCTURE_IP    kiwi-fastdfs
$INFRASTRUCTURE_IP    kiwi-es
$INFRASTRUCTURE_IP    kiwi-chattts

# Kiwi Microservices
$SERVICE_IP    kiwi-microservice-local
$SERVICE_IP    kiwi-microservice
$SERVICE_IP    kiwi-eureka
$SERVICE_IP    kiwi-config
$SERVICE_IP    kiwi-auth
$SERVICE_IP    kiwi-upms
$SERVICE_IP    kiwi-gate
$SERVICE_IP    kiwi-ai
$SERVICE_IP    kiwi-crawler
# End Kiwi Services
EOF

    # Record hosts configuration details
    save_config "hosts_fastdfs_non_local_ip" "$FASTDFS_NON_LOCAL_IP"
    save_config "hosts_infrastructure_ip" "$INFRASTRUCTURE_IP"
    save_config "hosts_service_ip" "$SERVICE_IP"
    save_config "hosts_fastdfs_hostname" "$FASTDFS_HOSTNAME"
    save_config "hosts_infrastructure_services" "kiwi-ui, kiwi-redis, kiwi-rabbitmq, kiwi-db, kiwi-fastdfs, kiwi-es"
    save_config "hosts_microservices" "kiwi-microservice-local, kiwi-microservice, kiwi-eureka, kiwi-config, kiwi-auth, kiwi-upms, kiwi-gate, kiwi-ai, kiwi-crawler"
    save_config "hosts_backup_file" "/etc/hosts.backup.$(date +%Y%m%d_%H%M%S)"

    echo "✓ Hosts file configured with separate IP addresses:"
    echo "  - FastDFS hostname ($FASTDFS_HOSTNAME): $FASTDFS_NON_LOCAL_IP"
    echo "  - Infrastructure services: $INFRASTRUCTURE_IP"
    echo "  - Microservices: $SERVICE_IP"
    echo "  - Backup created: /etc/hosts.backup.*"

    mark_step_completed "hosts_configured"
else
    echo "Step 11: Hosts file already configured, skipping..."
    echo "  - FastDFS Non-Local IP: $(load_config 'hosts_fastdfs_non_local_ip')"
    echo "  - Infrastructure IP: $(load_config 'hosts_infrastructure_ip')"
    echo "  - Service IP: $(load_config 'hosts_service_ip')"
fi

# Step 12: Setup environment variables
if ! is_step_completed "env_vars_setup"; then
    echo "Step 12: Setting up environment variables..."

    # Remove old entries if they exist
    run_as_user sed -i '/export KIWI_ENC_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export GROK_API_KEY=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export DB_IP=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export MYSQL_ROOT_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export REDIS_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export FASTDFS_HOSTNAME=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export FASTDFS_NON_LOCAL_IP=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_ROOT_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_USER_NAME=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export ES_USER_PASSWORD=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export INFRASTRUCTURE_IP=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true
    run_as_user sed -i '/export SERVICE_IP=/d' "$SCRIPT_HOME/.bashrc" 2>/dev/null || true

    # Add new entries to .bashrc
    run_as_user bash -c "echo 'export KIWI_ENC_PASSWORD=\"$KIWI_ENC_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export DB_IP=\"$INFRASTRUCTURE_IP\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export GROK_API_KEY=\"$GROK_API_KEY\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export MYSQL_ROOT_PASSWORD=\"$MYSQL_ROOT_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export REDIS_PASSWORD=\"$REDIS_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export FASTDFS_HOSTNAME=\"$FASTDFS_HOSTNAME\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export FASTDFS_NON_LOCAL_IP=\"$FASTDFS_NON_LOCAL_IP\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_ROOT_PASSWORD=\"$ES_ROOT_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_USER_NAME=\"$ES_USER_NAME\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export ES_USER_PASSWORD=\"$ES_USER_PASSWORD\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export INFRASTRUCTURE_IP=\"$INFRASTRUCTURE_IP\"' >> ~/.bashrc"
    run_as_user bash -c "echo 'export SERVICE_IP=\"$SERVICE_IP\"' >> ~/.bashrc"

    # Record environment variables setup
    save_config "env_vars_added_to_bashrc" "KIWI_ENC_PASSWORD, GROK_API_KEY, DB_IP, MYSQL_ROOT_PASSWORD, REDIS_PASSWORD, FASTDFS_HOSTNAME, FASTDFS_NON_LOCAL_IP, ES_ROOT_PASSWORD, ES_USER_NAME, ES_USER_PASSWORD, INFRASTRUCTURE_IP, SERVICE_IP"
    save_config "bashrc_location" "$SCRIPT_HOME/.bashrc"

    echo "Environment variables added to .bashrc and saved to persistent configuration."
    echo "  - DB_IP set to Infrastructure IP: $INFRASTRUCTURE_IP"
    echo "  - FASTDFS_NON_LOCAL_IP: $FASTDFS_NON_LOCAL_IP"
    echo "  - INFRASTRUCTURE_IP: $INFRASTRUCTURE_IP"
    echo "  - SERVICE_IP: $SERVICE_IP"

    mark_step_completed "env_vars_setup"
else
    echo "Step 12: Environment variables already set up, skipping..."
fi

# Step 13: Download yt-dlp
if ! is_step_completed "ytdlp_download"; then
    echo "Step 13: Downloading yt-dlp..."

    wget https://github.com/yt-dlp/yt-dlp/releases/download/2025.04.30/yt-dlp_linux -O /tmp/yt-dlp_linux
    chmod +x /tmp/yt-dlp_linux

    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/biz/"
    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/batch/"

    rm /tmp/yt-dlp_linux

    mark_step_completed "ytdlp_download"
else
    echo "Step 13: yt-dlp already downloaded, skipping..."
fi

# Step 14: Setup MySQL (Improved Version)
if ! is_step_completed "mysql_setup"; then
    echo "Step 14: Setting up MySQL..."

    # Check if port 3306 is already in use (likely another Docker container)
    if netstat -tlnp | grep -q ":3306 "; then
        echo "Warning: Port 3306 is already in use. Checking what's using it:"
        netstat -tlnp | grep ":3306 "

        # Check if it's another Docker container
        EXISTING_MYSQL_CONTAINER=$(docker ps --format '{{.Names}}' | grep -E '(mysql|mariadb)' || echo "")
        if [ -n "$EXISTING_MYSQL_CONTAINER" ]; then
            echo "Found existing MySQL/MariaDB container: $EXISTING_MYSQL_CONTAINER"
            if [ "$EXISTING_MYSQL_CONTAINER" != "kiwi-mysql" ]; then
                echo "Another MySQL container is running. You may want to stop it first:"
                echo "docker stop $EXISTING_MYSQL_CONTAINER"
                read -p "Continue anyway? (y/N): " CONTINUE_ANYWAY
                if [[ ! "$CONTINUE_ANYWAY" =~ ^[Yy]$ ]]; then
                    echo "Setup aborted. Please stop the conflicting container and retry."
                    exit 1
                fi
            fi
        else
            echo "Port 3306 is in use by another process (not Docker)."
            echo "This might be a system MySQL service or another application."
            echo "You may need to stop it manually or use a different port."
            read -p "Continue anyway? (y/N): " CONTINUE_ANYWAY
            if [[ ! "$CONTINUE_ANYWAY" =~ ^[Yy]$ ]]; then
                echo "Setup aborted. Please resolve port conflict and retry."
                exit 1
            fi
        fi
    fi

    # Stop and remove existing container if it exists
    echo "Cleaning up any existing MySQL container..."
    docker stop kiwi-mysql 2>/dev/null || true
    docker rm kiwi-mysql 2>/dev/null || true

    # Create MySQL directory if it doesn't exist
    echo "Preparing MySQL data directory..."
    run_as_user mkdir -p "$SCRIPT_HOME/docker/mysql"

    # Use MySQL 8.0 for better ARM compatibility
    echo "Pulling MySQL image..."
    if docker pull mysql; then
        echo "Using MySQL latest..."
        MYSQL_IMAGE="mysql"
        save_config "mysql_engine" "MySQL latest"
    else
        echo "Failed to pull MySQL image"
        echo "Please check your internet connection and Docker installation"
        save_config "mysql_setup_status" "failed - unable to pull images"
        mark_step_completed "mysql_setup"  # Mark as completed to prevent retry loop
        exit 1
    fi

    # Run MySQL container with improved configuration
    echo "Starting MySQL container..."
    docker run -itd \
        --name kiwi-mysql \
        -p 3306:3306 \
        -v "$SCRIPT_HOME/docker/mysql:/mysql_tmp" \
        -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
        -e MYSQL_DATABASE=kiwi_db \
        --restart=unless-stopped \
        "$MYSQL_IMAGE" \
        --max-connections=1000 \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci

    # Wait for MySQL to be ready with improved health checking
    echo "Waiting for MySQL to start (this may take up to 3 minutes)..."

    # Define improved MySQL readiness check function
    check_mysql_connection() {
        # Try TCP connection instead of socket
        docker exec kiwi-mysql mysqladmin ping -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null
        return $?
    }

    # Wait up to 180 seconds for MySQL to be ready
    MYSQL_READY=false
    for i in {1..36}; do
        echo "Attempt $i/36: Checking MySQL readiness..."

        # First check if container is running
        if ! docker ps --format '{{.Names}}' | grep -q "^kiwi-mysql$"; then
            echo "Container not running, checking logs..."
            docker logs kiwi-mysql --tail 10
            sleep 5
            continue
        fi

        # Then check MySQL connection
        if check_mysql_connection; then
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
        docker logs kiwi-mysql --tail 50
        echo
        echo "Container status:"
        docker ps -a | grep kiwi-mysql
        echo
        echo "Troubleshooting steps:"
        echo "1. Check if port 3306 is still in use: sudo netstat -tlnp | grep 3306"
        echo "2. Check container status: docker ps -a | grep kiwi-mysql"
        echo "3. View full logs: docker logs kiwi-mysql"
        echo "4. Check system resources: free -h && df -h"
        echo "5. Try manual start: docker start kiwi-mysql"
        echo "6. Check for permission issues: ls -la $SCRIPT_HOME/docker/mysql"

        save_config "mysql_setup_status" "failed - timeout waiting for startup"

        # Ask user if they want to continue
        echo
        read -p "MySQL setup failed. Continue with other steps? (y/N): " CONTINUE_SETUP
        if [[ ! "$CONTINUE_SETUP" =~ ^[Yy]$ ]]; then
            echo "Setup aborted. You can re-run the script later to retry."
            exit 1
        fi
    else
        # Verify database creation using TCP connection
        echo "Verifying database creation..."
        if docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;" | grep -q "kiwi_db"; then
            echo "✓ Database 'kiwi_db' confirmed to exist"
        else
            echo "Creating database manually..."
            if docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS kiwi_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"; then
                echo "✓ Database 'kiwi_db' created successfully"
            else
                echo "⚠ Database creation failed, but continuing (may already exist)"
            fi
        fi

        # Check for and restore database backup if it exists
        echo "Checking for database backup to restore..."

        # First, try to move backup file from root to proper location
        if [ -f "$SCRIPT_HOME/kiwi-db.sql" ]; then
            echo "Moving backup file to proper location..."
            mv "$SCRIPT_HOME/kiwi-db.sql" "$SCRIPT_HOME/docker/mysql/"
        fi

        if [ -f "$SCRIPT_HOME/docker/mysql/kiwi-db.sql" ]; then
            echo "Found kiwi-db.sql backup file, restoring database..."

            # Verify file is accessible inside container
            if docker exec kiwi-mysql test -f /mysql_tmp/kiwi-db.sql; then
                echo "Backup file accessible, proceeding with restoration..."

                # Restore using TCP connection
                if docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" kiwi_db -e "source /mysql_tmp/kiwi-db.sql"; then
                    echo "✓ Basic database backup restored successfully"
                    save_config "mysql_backup_restored" "kiwi-db.sql restored successfully"
                    save_config "mysql_backup_file" "$SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                else
                    echo "Trying alternative restoration method..."
                    if docker exec -i kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" kiwi_db < /mysql_tmp/kiwi-db.sql; then
                        echo "✓ Database backup restored successfully (alternative method)"
                        save_config "mysql_backup_restored" "kiwi-db.sql restored successfully (alternative)"
                    else
                        echo "⚠ Failed to restore database backup automatically"
                        echo "You can manually restore using:"
                        echo "docker exec -i kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p$MYSQL_ROOT_PASSWORD kiwi_db < $SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                        save_config "mysql_backup_restored" "failed - manual restoration required"
                    fi
                fi

                # Also restore YTB table if available
                if [ -f "$SCRIPT_HOME/microservice-kiwi/kiwi-sql/ytb_table_initialize.sql" ]; then
                    echo "Found YTB table initialization script..."
                    cp "$SCRIPT_HOME/microservice-kiwi/kiwi-sql/ytb_table_initialize.sql" "$SCRIPT_HOME/docker/mysql/"
                    if docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" kiwi_db -e "source /mysql_tmp/ytb_table_initialize.sql"; then
                        echo "✓ YTB database backup restored successfully"
                        save_config "mysql_ytb_backup_restored" "ytb_table_initialize.sql restored successfully"
                    else
                        echo "⚠ Failed to restore YTB table backup"
                    fi
                fi
            else
                echo "⚠ Backup file not accessible inside container"
                echo "File permissions or mount issue. Check:"
                echo "ls -la $SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                docker exec kiwi-mysql ls -la /mysql_tmp/
                save_config "mysql_backup_restored" "failed - file not accessible in container"
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
    echo "Step 14: MySQL already set up, skipping..."
    check_and_start_container "kiwi-mysql" "mysql_setup"

    # Check if backup restoration is needed (even on skip)
    if ! has_config "mysql_backup_restored" || [ "$(load_config 'mysql_backup_restored')" = "no backup file found" ]; then
        echo "Checking for database backup to restore..."

        # Move backup file if it exists in root
        if [ -f "$SCRIPT_HOME/kiwi-db.sql" ]; then
            mv "$SCRIPT_HOME/kiwi-db.sql" "$SCRIPT_HOME/docker/mysql/"
        fi

        if [ -f "$SCRIPT_HOME/docker/mysql/kiwi-db.sql" ]; then
            echo "Found kiwi-db.sql backup file, restoring database..."

            # Ensure MySQL is running before attempting restore
            if docker ps --format '{{.Names}}' | grep -q "^kiwi-mysql$"; then
                # Wait a moment for MySQL to be fully ready
                echo "Waiting for MySQL to be ready for backup restoration..."
                sleep 10

                # Check if MySQL is responding using TCP
                if docker exec kiwi-mysql mysqladmin ping -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null; then
                    echo "Restoring database from backup..."
                    if docker exec -i kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" kiwi_db < /mysql_tmp/kiwi-db.sql; then
                        echo "✓ Database backup restored successfully"
                        save_config "mysql_backup_restored" "kiwi-db.sql restored successfully"
                        save_config "mysql_backup_file" "$SCRIPT_HOME/docker/mysql/kiwi-db.sql"
                    else
                        echo "⚠ Failed to restore database backup"
                        echo "You can manually restore using:"
                        echo "docker exec -i kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p$MYSQL_ROOT_PASSWORD kiwi_db < $SCRIPT_HOME/docker/mysql/kiwi-db.sql"
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

# Step 14.5: Configure MySQL settings (Improved)
if ! is_step_completed "mysql_config"; then
    echo "Step 14.5: Configuring MySQL settings..."

    # Check if MySQL container is running
    if ! docker ps --format '{{.Names}}' | grep -q "^kiwi-mysql$"; then
        echo "MySQL container is not running, attempting to start..."
        if ! docker start kiwi-mysql; then
            echo "⚠ Failed to start MySQL container for configuration"
            save_config "mysql_config_status" "failed - container not running"
            mark_step_completed "mysql_config"
        fi
        sleep 10
    fi

    # Wait for MySQL to be ready using TCP connection
    echo "Waiting for MySQL to be ready for configuration..."
    MYSQL_READY=false
    for i in {1..12}; do
        if docker exec kiwi-mysql mysqladmin ping -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null; then
            MYSQL_READY=true
            break
        fi
        echo "MySQL not ready yet, waiting 5 more seconds..."
        sleep 5
    done

    if [ "$MYSQL_READY" = false ]; then
        echo "⚠ MySQL not ready for configuration"
        save_config "mysql_config_status" "failed - MySQL not ready"
    else
        # Verify current max_connections setting
        echo "Checking current MySQL configuration..."
        CURRENT_MAX_CONN=$(docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW VARIABLES LIKE 'max_connections';" 2>/dev/null | grep max_connections | awk '{print $2}' || echo "unknown")
        echo "Current max_connections: $CURRENT_MAX_CONN"

        if [ "$CURRENT_MAX_CONN" = "1000" ]; then
            echo "✓ MySQL max_connections already set to 1000 (configured at startup)"
            save_config "mysql_max_connections_configured" "1000"
            save_config "mysql_current_max_connections" "$CURRENT_MAX_CONN"
            save_config "mysql_config_status" "already configured at startup"
        else
            # Configure MySQL settings if not set at startup
            echo "Setting MySQL max_connections to 1000..."
            if docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" -e "SET GLOBAL max_connections = 1000;" 2>/dev/null; then
                echo "✓ MySQL max_connections set to 1000"
                save_config "mysql_max_connections_configured" "1000"

                # Verify the setting was applied
                UPDATED_MAX_CONN=$(docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p"$MYSQL_ROOT_PASSWORD" -e "SHOW VARIABLES LIKE 'max_connections';" 2>/dev/null | grep max_connections | awk '{print $2}' || echo "unknown")
                echo "✓ Updated max_connections: $UPDATED_MAX_CONN"
                save_config "mysql_current_max_connections" "$UPDATED_MAX_CONN"
                save_config "mysql_config_status" "completed successfully"
            else
                echo "⚠ Failed to configure MySQL max_connections"
                echo "You can manually run: docker exec kiwi-mysql mysql -h 127.0.0.1 -P 3306 -u root -p$MYSQL_ROOT_PASSWORD -e \"SET GLOBAL max_connections = 1000;\""
                save_config "mysql_config_status" "failed - manual configuration required"
            fi
        fi
    fi

    mark_step_completed "mysql_config"
else
    echo "Step 14.5: MySQL configuration already completed, skipping..."
    echo "  - Max connections configured: $(load_config 'mysql_max_connections_configured')"
    echo "  - Current setting: $(load_config 'mysql_current_max_connections')"
fi

# Step 15: Setup Redis
if ! is_step_completed "redis_setup"; then
    echo "Step 15: Setting up Redis..."

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
    echo "Step 15: Redis already set up, skipping..."
    check_and_start_container "kiwi-redis" "redis_setup"
fi

# Step 16: Setup RabbitMQ
if ! is_step_completed "rabbitmq_setup"; then
    echo "Step 16: Setting up RabbitMQ..."

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
    echo "Step 16: RabbitMQ already set up, skipping..."
    check_and_start_container "kiwi-rabbit" "rabbitmq_setup"
fi

# Fixed Function to detect system architecture and select appropriate FastDFS image
get_fastdfs_image() {
    local arch=$(uname -m)
    local fastdfs_image=""

    case $arch in
        x86_64|amd64)
            # For Intel/AMD 64-bit systems
            fastdfs_image="delron/fastdfs:latest"
            save_config "fastdfs_architecture" "x86_64"
            ;;
        aarch64|arm64)
            # For ARM 64-bit systems (Raspberry Pi 4, Apple M1/M2, etc.)
            fastdfs_image="fyclinux/fastdfs-arm64:6.04"
            save_config "fastdfs_architecture" "arm64"
            ;;
        armv7l|armhf)
            # For ARM 32-bit systems (older Raspberry Pi)
            fastdfs_image="morunchang/fastdfs:latest"
            save_config "fastdfs_architecture" "armv7"
            ;;
        *)
            # Fallback to x86_64 image with platform flag
            fastdfs_image="delron/fastdfs:latest"
            save_config "fastdfs_architecture" "unknown_$arch"
            ;;
    esac

    # Echo detection info to stderr so it doesn't interfere with return value
    echo "Detected $arch architecture, using: $fastdfs_image" >&2

    # Return only the image name
    echo "$fastdfs_image"
}

# Modified Step 17: Setup FastDFS with architecture detection
if ! is_step_completed "fastdfs_setup"; then
    echo "Step 17: Setting up FastDFS..."

    # Detect architecture and get appropriate image
    FASTDFS_IMAGE=$(get_fastdfs_image)

    # Pull FastDFS image with architecture consideration
    echo "Pulling FastDFS image: $FASTDFS_IMAGE"

    # For non-ARM64 systems, try to pull with platform specification
    ARCH=$(uname -m)
    if [[ "$ARCH" == "x86_64" || "$ARCH" == "amd64" ]]; then
        # Pull with explicit platform for x86_64
        docker pull --platform linux/amd64 "$FASTDFS_IMAGE" || docker pull "$FASTDFS_IMAGE"
    elif [[ "$ARCH" == "armv7l" || "$ARCH" == "armhf" ]]; then
        # Pull with explicit platform for ARM v7
        docker pull --platform linux/arm/v7 "$FASTDFS_IMAGE" || docker pull "$FASTDFS_IMAGE"
    else
        # For ARM64 and others, pull normally
        docker pull "$FASTDFS_IMAGE"
    fi

    # Stop and remove existing containers if they exist
    docker stop tracker storage 2>/dev/null || true
    docker rm tracker storage 2>/dev/null || true

    # Run tracker with the selected image
    echo "Starting FastDFS tracker..."

    docker run -ti -d \
        -p 8081:80 \
        -p 8881:8888 \
        --name tracker \
        "$FASTDFS_IMAGE" \
        tracker

    # Wait for tracker to start
    echo "Waiting for tracker to initialize..."
    sleep 15

    # Run storage with the selected image
    echo "Starting FastDFS storage..."
    docker run -ti -d \
        -p 8082:80 \
        -p 8882:8888 \
        --name storage \
        -v "$SCRIPT_HOME/storage_data:/fastdfs/storage/data" \
        -v "$SCRIPT_HOME/store_path:/fastdfs/store_path" \
        -e TRACKER_SERVER="$FASTDFS_HOSTNAME:22122" \
        "$FASTDFS_IMAGE" \
        storage

    # Wait for storage to start
    echo "Waiting for storage to initialize..."
    sleep 20

    # Configure storage.conf based on image type
    echo "Configuring FastDFS storage..."

    # Different images may have different configuration paths
    case "$FASTDFS_IMAGE" in
        *"delron/fastdfs"*|*"morunchang/fastdfs"*)
            # These images typically use different config paths
            docker exec storage sh -c "find /etc -name 'storage.conf' -exec sed -i 's/tracker_server=.*/tracker_server=$FASTDFS_HOSTNAME:22122/' {} +" 2>/dev/null || true
            ;;
        *"fyclinux/fastdfs-arm64"*)
            # Original ARM64 image configuration
            docker exec storage sed -i "s/tracker_server=.*/tracker_server=$FASTDFS_HOSTNAME:22122/" /etc/fdfs/storage.conf
            ;;
        *)
            # Generic configuration attempt
            docker exec storage sh -c "find /etc -name 'storage.conf' -exec sed -i 's/tracker_server=.*/tracker_server=$FASTDFS_HOSTNAME:22122/' {} +" 2>/dev/null || true
            ;;
    esac

    # Restart storage container to apply configuration
    echo "Restarting storage to apply configuration..."
    docker restart storage

    # Verify containers are running
    sleep 10
    if docker ps | grep -q "tracker" && docker ps | grep -q "storage"; then
        echo "✓ FastDFS setup completed successfully"
        echo "  - Architecture: $(uname -m)"
        echo "  - Image used: $FASTDFS_IMAGE"
        echo "  - Tracker: listening on port 22122"
        echo "  - Storage: connected to tracker"

        # Record setup details
        save_config "fastdfs_image_used" "$FASTDFS_IMAGE"
        save_config "fastdfs_tracker_port" "22122"
        save_config "fastdfs_setup_status" "completed successfully"
    else
        echo "⚠ FastDFS containers may not have started properly"
        echo "Check container status with: docker ps -a"
        echo "View logs with: docker logs tracker && docker logs storage"

        save_config "fastdfs_setup_status" "completed with warnings"
    fi

    mark_step_completed "fastdfs_setup"
else
    echo "Step 17: FastDFS already set up, skipping..."
    check_and_start_container "tracker" "fastdfs_setup"
    check_and_start_container "storage" "fastdfs_setup"

    # Display current configuration
    USED_IMAGE=$(load_config "fastdfs_image_used")
    USED_ARCH=$(load_config "fastdfs_architecture")
    if [ -n "$USED_IMAGE" ]; then
        echo "  - Previously used image: $USED_IMAGE"
        echo "  - Architecture: $USED_ARCH"
    fi
fi

# Step 18: Maven library installation
if ! is_step_completed "maven_lib_install"; then
    echo "Step 18: Installing Maven library..."

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
    echo "Step 18: Maven library already installed, skipping..."
fi

# Step 19: Setup deployment script
if ! is_step_completed "deployment_setup"; then
    echo "Step 19: Setting up deployment..."

    cd "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker"

    # Copy deployment script if it doesn't already exist as symlink
    if [ ! -L "$SCRIPT_HOME/deployKiwi.sh" ]; then
        run_as_user cp deployKiwi.sh "$SCRIPT_HOME/"
        run_as_user chmod 777 "$SCRIPT_HOME/deployKiwi.sh"
    fi

    echo "Deployment script setup completed."

    mark_step_completed "deployment_setup"
else
    echo "Step 19: Deployment already set up, skipping..."
fi

# Step 20: Setup Elasticsearch
if ! is_step_completed "elasticsearch_setup"; then
    echo "Step 20: Setting up Elasticsearch..."

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
    echo "$ES_ROOT_PASSWORD" | docker exec -i kiwi-es elasticsearch-users useradd root -r superuser -p
    echo "$ES_USER_PASSWORD" | docker exec -i kiwi-es elasticsearch-users useradd "$ES_USER_NAME" -r superuser -p

    echo "Users created successfully!"

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
    echo "Step 20: Elasticsearch already set up, skipping..."
    check_and_start_container "kiwi-es" "elasticsearch_setup"
fi

# Step 21: Install IK Tokenizer
if ! is_step_completed "ik_tokenizer_install"; then
    echo "Step 21: Installing IK Tokenizer..."

    # Install IK plugin
    docker exec kiwi-es elasticsearch-plugin install https://get.infini.cloud/elasticsearch/analysis-ik/7.17.9 --batch

    # Restart Elasticsearch
    docker restart kiwi-es

    echo "Waiting for Elasticsearch to restart..."
    sleep 30

    echo "IK Tokenizer installation completed."

    mark_step_completed "ik_tokenizer_install"
else
    echo "Step 21: IK Tokenizer already installed, skipping..."
fi

# Step 22: Setup Nginx and UI
if ! is_step_completed "nginx_ui_setup"; then
    echo "Step 22: Setting up Nginx and UI..."

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
    echo "Step 22: Nginx and UI already set up, skipping..."
    check_and_start_container "kiwi-ui" "nginx_ui_setup"
fi

echo
echo "=================================="
echo "Setup completed successfully!"
echo "=================================="
echo "Target user: $SCRIPT_USER"
echo "Home directory: $SCRIPT_HOME"
echo "Sudo user configured: $SUDO_USERNAME"
echo
echo "Network Configuration:"
echo "  FastDFS Non-Local IP: $FASTDFS_NON_LOCAL_IP"
echo "    - Used for: $FASTDFS_HOSTNAME"
echo "  Infrastructure IP: $INFRASTRUCTURE_IP"
echo "    - Used for: kiwi-ui, kiwi-redis, kiwi-rabbitmq, kiwi-db, kiwi-fastdfs, kiwi-es"
echo "  Service IP: $SERVICE_IP"
echo "    - Used for: kiwi-microservice-local, kiwi-microservice, kiwi-eureka, kiwi-config, kiwi-auth, kiwi-upms, kiwi-gate, kiwi-ai, kiwi-crawler"
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
echo "  DB_IP             = $INFRASTRUCTURE_IP"
echo "  MYSQL_ROOT_PASSWORD = [HIDDEN]"
echo "  REDIS_PASSWORD    = [HIDDEN]"
echo "  FASTDFS_HOSTNAME  = $FASTDFS_HOSTNAME"
echo "  FASTDFS_NON_LOCAL_IP = $FASTDFS_NON_LOCAL_IP"
echo "  ES_ROOT_PASSWORD  = [HIDDEN]"
echo "  ES_USER_NAME      = $ES_USER_NAME"
echo "  ES_USER_PASSWORD  = [HIDDEN]"
echo "  INFRASTRUCTURE_IP = $INFRASTRUCTURE_IP"
echo "  SERVICE_IP        = $SERVICE_IP"
echo
echo "Maven configuration:"
echo "  Settings file: $SCRIPT_HOME/.m2/settings.xml"
echo "  Local repository: $SCRIPT_HOME/.m2/repository"
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
echo "9. User '$SUDO_USERNAME' has been granted sudo privileges"
echo "10. Hosts file configured with separate IPs for infrastructure and services"
echo "11. FastDFS hostname ($FASTDFS_HOSTNAME) uses separate IP: $FASTDFS_NON_LOCAL_IP"
echo "12. System configured to stay awake when laptop lid is closed"
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
echo "- MySQL: Container 'kiwi-mysql' on port 3306 (accessible at $INFRASTRUCTURE_IP)"
echo "- Redis: Container 'kiwi-redis' on port 6379 (accessible at $INFRASTRUCTURE_IP)"
echo "- RabbitMQ: Container 'kiwi-rabbit' with management UI on port 15672 (accessible at $INFRASTRUCTURE_IP)"
echo "- FastDFS: Containers 'tracker' and 'storage' on port 22122 (accessible at $FASTDFS_NON_LOCAL_IP via $FASTDFS_HOSTNAME)"
echo "- Elasticsearch: Container 'kiwi-es' on ports 9200/9300 with IK tokenizer (accessible at $INFRASTRUCTURE_IP)"
echo "- Nginx/UI: Container 'kiwi-ui' on port 80 (accessible at $INFRASTRUCTURE_IP)"
echo
echo "WEB INTERFACES:"
echo "- RabbitMQ Management: http://$INFRASTRUCTURE_IP:15672 (guest/guest)"
echo "- Elasticsearch: http://$INFRASTRUCTURE_IP:9200 (root/[your_password])"
echo "- Kiwi UI: http://$INFRASTRUCTURE_IP:80"
echo "- FastDFS: http://$FASTDFS_NON_LOCAL_IP:8081 (via $FASTDFS_HOSTNAME)"
echo
echo "MAVEN CONFIGURATION:"
echo "- Settings file: $SCRIPT_HOME/.m2/settings.xml"
echo "- Local repository: $SCRIPT_HOME/.m2/repository"
echo "- Maven version: $(load_config 'maven_version')"
echo
echo "SUDO CONFIGURATION:"
echo "- User with sudo privileges: $SUDO_USERNAME"
echo "- Sudo setup method: $(load_config 'sudo_setup_method')"
echo "- Groups for user: $(load_config 'sudo_groups')"
echo
echo "HOSTS FILE CONFIGURATION:"
echo "- FastDFS hostname ($FASTDFS_HOSTNAME): $FASTDFS_NON_LOCAL_IP"
echo "- Infrastructure services (at $INFRASTRUCTURE_IP):"
echo "  kiwi-ui, kiwi-redis, kiwi-rabbitmq, kiwi-db, kiwi-fastdfs, kiwi-es"
echo "- Microservices (at $SERVICE_IP):"
echo "  kiwi-microservice-local, kiwi-microservice, kiwi-eureka, kiwi-config, kiwi-auth, kiwi-upms, kiwi-gate, kiwi-ai, kiwi-crawler"
echo
echo "To check container status: docker ps"
echo "To view container logs: docker logs <container-name>"
echo "To re-run this script with step selection: sudo ./$(basename $0)"

# Final step: Configure system to prevent sleep when laptop lid is closed and add aliases
echo "Configuring final system settings..."

# Prevent system sleep when laptop lid is closed
echo "Disabling system sleep targets to keep system running when lid is closed..."
systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target

# Add sleep prevention configuration status
save_config "sleep_prevention_configured" "masked sleep.target suspend.target hibernate.target hybrid-sleep.target"

echo "✓ System sleep targets disabled - laptop will stay awake when lid is closed"

# Add useful aliases
echo "Adding useful aliases..."
echo "alias ll='ls -la'" >> ~/.bashrc
run_as_user bash -c "echo 'alias dp=\"sudo docker ps\"' >> ~/.bashrc"

# Source bashrc to apply changes
source ~/.bashrc 2>/dev/null || true

echo "✓ Added aliases:"
echo "  - ll: for 'ls -la'"
echo "  - dp: for 'sudo docker ps'"

echo "=================================="

# Final step: Show Docker service status
echo "Docker service status:"
systemctl status docker --no-pager -l || true

echo
echo "FINAL SYSTEM CONFIGURATION:"
echo "============================="
echo "✓ System will stay awake when laptop lid is closed"
echo "✓ Sleep targets have been masked"
echo "✓ Docker service is running"
echo "✓ All containers are configured"
echo "✓ Environment variables are set"
echo "✓ Useful aliases are configured"
echo
echo "System is now fully configured and ready for use!"
echo "============================="