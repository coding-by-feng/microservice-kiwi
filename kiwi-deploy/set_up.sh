#!/bin/bash

# Enhanced Kiwi Microservice Setup Script for Raspberry Pi OS
# This script automates the complete setup process with step tracking and selective re-initialization
# Version: 2.0 - Enhanced with better error handling and package management

set -e  # Exit on any error

# Configuration
PROGRESS_FILE="$(pwd)/.kiwi_setup_progress"
CONFIG_FILE="$(pwd)/.kiwi_setup_config"
LOG_FILE="$(pwd)/.kiwi_setup.log"
SCRIPT_USER=${SUDO_USER:-$USER}
SCRIPT_HOME=$(eval echo ~$SCRIPT_USER)

# Color codes for better output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# Colored output functions
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
    log "SUCCESS: $1"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
    log "ERROR: $1"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
    log "WARNING: $1"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
    log "INFO: $1"
}

echo "=================================="
echo "Enhanced Kiwi Microservice Setup for Raspberry Pi"
echo "Version: 2.0"
echo "Running as: $(whoami)"
echo "Target user: $SCRIPT_USER"
echo "Target home: $SCRIPT_HOME"
echo "Architecture: $(uname -m)"
echo "OS Version: $(lsb_release -d 2>/dev/null | cut -f2 || cat /etc/os-release | grep PRETTY_NAME | cut -d'"' -f2)"
echo "Log file: $LOG_FILE"
echo "=================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    print_error "This script must be run as root (use sudo)"
    echo "Usage: sudo ./$(basename $0)"
    exit 1
fi

# Function to check system requirements
check_system_requirements() {
    print_info "Checking system requirements..."

    # Check available disk space (minimum 10GB)
    AVAILABLE_SPACE=$(df / | awk 'NR==2 {print $4}')
    if [ "$AVAILABLE_SPACE" -lt 10485760 ]; then
        print_warning "Less than 10GB of disk space available. This might cause issues."
        read -p "Continue anyway? (y/N): " CONTINUE
        if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi

    # Check available memory (minimum 2GB recommended)
    TOTAL_MEM=$(free -m | awk 'NR==2 {print $2}')
    if [ "$TOTAL_MEM" -lt 2048 ]; then
        print_warning "Less than 2GB RAM detected. Performance may be limited."
    fi

    # Check internet connectivity
    if ! ping -c 1 google.com &> /dev/null; then
        print_error "No internet connection detected. This script requires internet access."
        exit 1
    fi

    print_success "System requirements check completed"
}

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
    print_success "Step completed: $step_name"

    # Also save step completion timestamp to config
    save_config "${step_name}_completed_at" "$(date '+%Y-%m-%d %H:%M:%S')"
}

# Function to force re-initialize a step (remove from progress)
force_reinitialize_step() {
    local step_name="$1"
    if [ -f "$PROGRESS_FILE" ]; then
        sed -i "/^$step_name$/d" "$PROGRESS_FILE" 2>/dev/null || true
        print_success "Step '$step_name' marked for re-initialization"
    fi
}

# Function to prompt for input with validation
prompt_for_input() {
    local prompt_message="$1"
    local var_name="$2"
    local is_secret="$3"
    local validation_regex="$4"
    local validation_message="$5"

    while true; do
        if [ "$is_secret" = "true" ]; then
            read -s -p "$prompt_message: " input_value
            echo
        else
            read -p "$prompt_message: " input_value
        fi

        if [ -n "$input_value" ]; then
            if [ -n "$validation_regex" ]; then
                if [[ $input_value =~ $validation_regex ]]; then
                    eval "$var_name='$input_value'"
                    break
                else
                    print_warning "$validation_message"
                fi
            else
                eval "$var_name='$input_value'"
                break
            fi
        else
            print_warning "This field cannot be empty. Please try again."
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
        print_info "Checking if $container_name is running..."

        # Check if container exists
        if docker ps -a --format '{{.Names}}' | grep -q "^${container_name}$"; then
            # Check if container is running
            if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
                print_success "$container_name is already running"
                save_config "${container_name}_status_check" "running"
            else
                print_warning "$container_name exists but is stopped, starting..."
                if docker start "$container_name"; then
                    print_success "$container_name started successfully"
                    save_config "${container_name}_status_check" "started"
                    sleep 5
                    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
                        print_success "$container_name is now running and healthy"
                    else
                        print_warning "$container_name started but may not be healthy"
                        save_config "${container_name}_status_check" "started_but_unhealthy"
                    fi
                else
                    print_error "Failed to start $container_name"
                    save_config "${container_name}_status_check" "failed_to_start"
                fi
            fi
        else
            print_warning "$container_name does not exist (setup may have been incomplete)"
            save_config "${container_name}_status_check" "container_missing"
        fi
    fi
}

# Function to check if container is running
check_container_running() {
    local container_name="$1"
    docker ps --format '{{.Names}}' | grep -q "^${container_name}$"
    return $?
}

# Function to get sudo user configuration
get_sudo_user_config() {
    print_info "Loading sudo user configuration..."

    # Load sudo username
    if has_config "SUDO_USERNAME"; then
        SUDO_USERNAME=$(load_config "SUDO_USERNAME")
        print_info "Using saved sudo username: $SUDO_USERNAME"
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
    print_info "Loading environment variables..."

    # Load KIWI_ENC_PASSWORD
    if has_config "KIWI_ENC_PASSWORD"; then
        KIWI_ENC_PASSWORD=$(load_config "KIWI_ENC_PASSWORD")
        print_info "Using saved KIWI_ENC_PASSWORD"
    else
        prompt_for_input "Enter KIWI_ENC_PASSWORD" "KIWI_ENC_PASSWORD" "true"
        save_config "KIWI_ENC_PASSWORD" "$KIWI_ENC_PASSWORD"
    fi

    # Load GROK_API_KEY
    if has_config "GROK_API_KEY"; then
        GROK_API_KEY=$(load_config "GROK_API_KEY")
        print_info "Using saved GROK_API_KEY"
    else
        prompt_for_input "Enter GROK_API_KEY" "GROK_API_KEY" "true"
        save_config "GROK_API_KEY" "$GROK_API_KEY"
    fi
}

# Function to get database passwords and network configuration
get_db_passwords() {
    print_info "Loading database and service configurations..."

    # Load MySQL password
    if has_config "MYSQL_ROOT_PASSWORD"; then
        MYSQL_ROOT_PASSWORD=$(load_config "MYSQL_ROOT_PASSWORD")
        print_info "Using saved MySQL root password"
    else
        prompt_for_input "Enter MySQL root password" "MYSQL_ROOT_PASSWORD" "true"
        save_config "MYSQL_ROOT_PASSWORD" "$MYSQL_ROOT_PASSWORD"
    fi

    # Load Redis password
    if has_config "REDIS_PASSWORD"; then
        REDIS_PASSWORD=$(load_config "REDIS_PASSWORD")
        print_info "Using saved Redis password"
    else
        prompt_for_input "Enter Redis password" "REDIS_PASSWORD" "true"
        save_config "REDIS_PASSWORD" "$REDIS_PASSWORD"
    fi

    # Load FastDFS hostname
    if has_config "FASTDFS_HOSTNAME"; then
        FASTDFS_HOSTNAME=$(load_config "FASTDFS_HOSTNAME")
        print_info "Using saved FastDFS hostname: $FASTDFS_HOSTNAME"
    else
        prompt_for_input "Enter FastDFS hostname (e.g., fastdfs.fengorz.me)" "FASTDFS_HOSTNAME" "false" \
            "^[a-zA-Z0-9][a-zA-Z0-9.-]+[a-zA-Z0-9]$" "Please enter a valid hostname"
        save_config "FASTDFS_HOSTNAME" "$FASTDFS_HOSTNAME"
    fi

    # Load FastDFS Non-Local IP
    if has_config "FASTDFS_NON_LOCAL_IP"; then
        FASTDFS_NON_LOCAL_IP=$(load_config "FASTDFS_NON_LOCAL_IP")
        print_info "Using saved FastDFS Non-Local IP: $FASTDFS_NON_LOCAL_IP"
    else
        print_info "FastDFS Non-Local IP is used specifically for: fastdfs.fengorz.me"
        prompt_for_input "Enter FastDFS Non-Local IP (press Enter to use Infrastructure IP)" "input_fastdfs_ip" "false" \
            "^$|^([0-9]{1,3}\.){3}[0-9]{1,3}$" "Please enter a valid IP address or press Enter"
        FASTDFS_NON_LOCAL_IP=${input_fastdfs_ip}
        save_config "FASTDFS_NON_LOCAL_IP" "$FASTDFS_NON_LOCAL_IP"
    fi

    # Load Infrastructure IP
    if has_config "INFRASTRUCTURE_IP"; then
        INFRASTRUCTURE_IP=$(load_config "INFRASTRUCTURE_IP")
        print_info "Using saved Infrastructure IP: $INFRASTRUCTURE_IP"
    else
        print_info "Infrastructure IP is used for: kiwi-ui, kiwi-redis, kiwi-rabbitmq, kiwi-db, kiwi-fastdfs, kiwi-es"
        prompt_for_input "Enter Infrastructure IP (default: 127.0.0.1)" "input_ip" "false" \
            "^$|^([0-9]{1,3}\.){3}[0-9]{1,3}$" "Please enter a valid IP address"
        INFRASTRUCTURE_IP=${input_ip:-127.0.0.1}
        save_config "INFRASTRUCTURE_IP" "$INFRASTRUCTURE_IP"
    fi

    # Set FastDFS Non-Local IP to Infrastructure IP if it wasn't specified
    if [ -z "$FASTDFS_NON_LOCAL_IP" ]; then
        FASTDFS_NON_LOCAL_IP="$INFRASTRUCTURE_IP"
        save_config "FASTDFS_NON_LOCAL_IP" "$FASTDFS_NON_LOCAL_IP"
        print_info "FastDFS Non-Local IP set to Infrastructure IP: $FASTDFS_NON_LOCAL_IP"
    fi

    # Load Service IP
    if has_config "SERVICE_IP"; then
        SERVICE_IP=$(load_config "SERVICE_IP")
        print_info "Using saved Service IP: $SERVICE_IP"
    else
        print_info "Service IP is used for microservices"
        prompt_for_input "Enter Service IP (default: 127.0.0.1)" "input_ip" "false" \
            "^$|^([0-9]{1,3}\.){3}[0-9]{1,3}$" "Please enter a valid IP address"
        SERVICE_IP=${input_ip:-127.0.0.1}
        save_config "SERVICE_IP" "$SERVICE_IP"
    fi

    # Load Elasticsearch passwords
    if has_config "ES_ROOT_PASSWORD"; then
        ES_ROOT_PASSWORD=$(load_config "ES_ROOT_PASSWORD")
        print_info "Using saved Elasticsearch root password"
    else
        prompt_for_input "Enter Elasticsearch root password" "ES_ROOT_PASSWORD" "true"
        save_config "ES_ROOT_PASSWORD" "$ES_ROOT_PASSWORD"
    fi

    if has_config "ES_USER_NAME"; then
        ES_USER_NAME=$(load_config "ES_USER_NAME")
        print_info "Using saved Elasticsearch username: $ES_USER_NAME"
    else
        prompt_for_input "Enter Elasticsearch additional username" "ES_USER_NAME" "false"
        save_config "ES_USER_NAME" "$ES_USER_NAME"
    fi

    if has_config "ES_USER_PASSWORD"; then
        ES_USER_PASSWORD=$(load_config "ES_USER_PASSWORD")
        print_info "Using saved Elasticsearch user password"
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
    echo "3. Clean all and start fresh"
    echo
    read -p "Enter your choice (0-3) [default: 0]: " SETUP_MODE

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
        3)
            clean_all_setup
            ;;
        0)
            print_info "Proceeding with full automatic setup..."
            ;;
        *)
            print_warning "Invalid choice. Proceeding with full automatic setup..."
            ;;
    esac
}

# Function to clean all setup
clean_all_setup() {
    print_warning "This will remove all progress and configuration files!"
    read -p "Are you sure? (yes/no): " CONFIRM
    if [ "$CONFIRM" = "yes" ]; then
        rm -f "$PROGRESS_FILE" "$CONFIG_FILE"
        print_success "All setup files cleaned. Starting fresh..."
    else
        print_info "Clean cancelled. Continuing with normal setup..."
    fi
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
        print_info "No steps selected. Proceeding with normal flow..."
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
        print_info "Re-initializing ALL steps..."
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
                print_warning "Invalid step number '$num' (valid range: 1-22)"
            fi
        done
    fi

    echo
    print_success "Selected steps have been marked for re-initialization."
    print_info "Proceeding with setup..."
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
            status="${GREEN}✓ COMPLETED${NC}"
            completed_time=$(load_config "${step_name}_completed_at")
            if [ -n "$completed_time" ]; then
                status="$status (${completed_time})"
            fi
            ((completed_count++))
        else
            status="${RED}✗ PENDING${NC}"
        fi

        printf "%2d. %-25s " "$step_num" "$step_name"
        echo -e "$status"
        printf "    %s\n" "$step_desc"
        echo
    done

    echo "=================================="
    echo "Progress: $completed_count/$total_count steps completed"
    echo "Progress file: $PROGRESS_FILE"
    echo "Config file: $CONFIG_FILE"
    echo "Log file: $LOG_FILE"
    echo "=================================="
}

# Check system requirements first
check_system_requirements

# Get configuration
get_sudo_user_config
get_env_vars
get_db_passwords

# Show step selection menu
show_step_menu

echo
print_info "Starting setup process..."

# Step 1: Configure sudo privileges for user
if ! is_step_completed "sudo_user_setup"; then
    print_info "Step 1: Configuring sudo privileges for user: $SUDO_USERNAME"

    # Check if user exists
    if ! id "$SUDO_USERNAME" >/dev/null 2>&1; then
        print_error "User '$SUDO_USERNAME' does not exist on this system"
        echo "Available users:"
        getent passwd | grep -E '/home|/Users' | cut -d: -f1 | head -10
        echo "Please create the user first or specify a different username"
        exit 1
    fi

    # Check if user is already in sudo group
    if groups "$SUDO_USERNAME" | grep -q "\bsudo\b"; then
        print_info "User '$SUDO_USERNAME' is already in the sudo group"
        save_config "sudo_setup_method" "already_in_sudo_group"
    else
        print_info "Adding user '$SUDO_USERNAME' to sudo group..."
        usermod -aG sudo "$SUDO_USERNAME"
        save_config "sudo_setup_method" "added_to_sudo_group"
    fi

    # Also add specific sudoers entry for extra safety
    SUDOERS_LINE="$SUDO_USERNAME ALL=(ALL:ALL) ALL"

    # Check if entry already exists
    if sudo grep -Fxq "$SUDOERS_LINE" /etc/sudoers; then
        print_info "Sudoers entry already exists for user '$SUDO_USERNAME'"
        save_config "sudoers_entry_added" "already_exists"
    else
        print_info "Adding sudoers entry for user '$SUDO_USERNAME'..."

        # Create a temporary sudoers file to validate syntax
        TEMP_SUDOERS=$(mktemp)
        cp /etc/sudoers "$TEMP_SUDOERS"
        echo "$SUDOERS_LINE" >> "$TEMP_SUDOERS"

        # Validate the sudoers file
        if visudo -c -f "$TEMP_SUDOERS" >/dev/null 2>&1; then
            echo "$SUDOERS_LINE" >> /etc/sudoers
            print_success "Sudoers entry added successfully"
            save_config "sudoers_entry_added" "success"
        else
            print_error "Invalid sudoers syntax, entry not added"
            save_config "sudoers_entry_added" "failed_validation"
        fi

        # Clean up temporary file
        rm -f "$TEMP_SUDOERS"
    fi

    save_config "sudo_username_configured" "$SUDO_USERNAME"
    save_config "sudo_groups" "$(groups $SUDO_USERNAME 2>/dev/null || echo 'unknown')"

    print_success "Sudo privileges configured for user: $SUDO_USERNAME"
    mark_step_completed "sudo_user_setup"
else
    print_info "Step 1: Sudo user setup already completed, skipping..."
fi

# Step 2: System update and package installations
if ! is_step_completed "system_update"; then
    print_info "Step 2: Updating system and installing essential packages..."

    # Update package lists
    apt update

    # Upgrade existing packages (optional, can be slow)
    # apt upgrade -y

    # Install essential packages
    print_info "Installing essential packages..."
    apt install -y \
        curl \
        wget \
        ca-certificates \
        gnupg \
        lsb-release \
        apt-transport-https \
        software-properties-common \
        net-tools \
        htop \
        vim \
        nano

    save_config "system_update_status" "completed"
    save_config "system_update_packages" "$(apt list --installed 2>/dev/null | wc -l) packages"

    mark_step_completed "system_update"
else
    print_info "Step 2: System update already completed, skipping..."
fi

# Step 3: Install Docker
if ! is_step_completed "docker_install"; then
    print_info "Step 3: Installing Docker..."

    # Detect architecture
    ARCH=$(uname -m)
    print_info "Detected architecture: $ARCH"

    # Remove any old Docker installations
    apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

    # Try official Docker installation
    print_info "Installing Docker using official method..."
    curl -fsSL https://get.docker.com -o get-docker.sh

    if sh get-docker.sh; then
        print_success "Docker installed successfully"
        save_config "docker_install_method" "official_script"
    else
        print_warning "Official script failed, trying manual installation..."

        # Manual installation
        apt-get update
        apt-get install -y ca-certificates curl gnupg lsb-release

        # Add Docker's official GPG key
        mkdir -p /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg

        # Set up the repository
        echo \
          "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
          $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null

        # Update and install Docker
        apt-get update

        # Install Docker packages (without docker-scan-plugin which may cause issues)
        apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin || \
        apt-get install -y docker-ce docker-ce-cli containerd.io

        save_config "docker_install_method" "manual_installation"
    fi

    rm -f get-docker.sh

    # Add user to docker group
    usermod -aG docker $SCRIPT_USER

    # Start and enable docker service
    systemctl start docker
    systemctl enable docker

    # Test Docker installation
    if docker run hello-world >/dev/null 2>&1; then
        print_success "Docker test successful"
        docker rm $(docker ps -aq -f ancestor=hello-world) 2>/dev/null || true
    else
        print_warning "Docker test failed, may need to restart or re-login"
    fi

    DOCKER_VERSION=$(docker --version 2>/dev/null || echo "unknown")
    save_config "docker_version" "$DOCKER_VERSION"
    save_config "docker_architecture" "$ARCH"

    print_success "Docker installation completed"
    mark_step_completed "docker_install"
else
    print_info "Step 3: Docker already installed, skipping..."
fi

# Step 4: Setup Docker alias and test
if ! is_step_completed "docker_setup"; then
    print_info "Step 4: Setting up Docker..."

    # Test docker
    print_info "Testing Docker installation..."
    if run_as_user_home docker version >/dev/null 2>&1; then
        print_success "Docker is accessible"
        save_config "docker_test_status" "success"
    else
        print_warning "Docker not accessible yet. You may need to log out and back in"
        save_config "docker_test_status" "needs_relogin"
    fi

    mark_step_completed "docker_setup"
else
    print_info "Step 4: Docker setup already completed, skipping..."
fi

# Step 5: Clean Docker system
if ! is_step_completed "docker_cleanup"; then
    print_info "Step 5: Cleaning Docker system..."

    if docker version >/dev/null 2>&1; then
        print_info "Pruning Docker system..."
        docker system prune -a -f --volumes 2>/dev/null || true
        print_success "Docker system cleaned"
    else
        print_warning "Docker not accessible, skipping cleanup"
    fi

    mark_step_completed "docker_cleanup"
else
    print_info "Step 5: Docker cleanup already completed, skipping..."
fi

# Step 6: Install Docker Compose
if ! is_step_completed "docker_compose_install"; then
    print_info "Step 6: Installing Docker Compose..."

    # Check if docker-compose is already installed via Docker plugin
    if docker compose version >/dev/null 2>&1; then
        print_success "Docker Compose already available as Docker plugin"
        save_config "docker_compose_type" "plugin"
    else
        # Install standalone Docker Compose
        ARCH=$(uname -m)
        case $ARCH in
            x86_64)
                COMPOSE_ARCH="x86_64"
                ;;
            aarch64|arm64)
                COMPOSE_ARCH="aarch64"
                ;;
            armv7l)
                COMPOSE_ARCH="armv7"
                ;;
            *)
                COMPOSE_ARCH="x86_64"
                ;;
        esac

        print_info "Downloading Docker Compose for $COMPOSE_ARCH..."
        COMPOSE_VERSION="v2.24.0"
        wget "https://github.com/docker/compose/releases/download/${COMPOSE_VERSION}/docker-compose-linux-${COMPOSE_ARCH}" \
            -O /usr/local/bin/docker-compose
        chmod +x /usr/local/bin/docker-compose

        save_config "docker_compose_type" "standalone"
    fi

    COMPOSE_VERSION=$(docker-compose --version 2>/dev/null || docker compose version 2>/dev/null || echo "unknown")
    save_config "docker_compose_version" "$COMPOSE_VERSION"

    print_success "Docker Compose installed"
    mark_step_completed "docker_compose_install"
else
    print_info "Step 6: Docker Compose already installed, skipping..."
fi

# Step 7: Install Python and other packages (ENHANCED)
if ! is_step_completed "python_install"; then
    print_info "Step 7: Installing Python and development packages..."

    # Detect OS version for package compatibility
    OS_VERSION=$(lsb_release -cs 2>/dev/null || echo "unknown")

    # Install Python packages
    print_info "Installing Python packages..."
    apt install -y python3 python3-pip python3-venv python3-dev || {
        print_warning "Some Python packages failed to install"
    }

    # Install Java
    print_info "Installing Java..."
    # Try different Java versions based on availability
    if apt-cache show openjdk-17-jdk >/dev/null 2>&1; then
        apt install -y openjdk-17-jdk
        save_config "java_version_installed" "17"
    elif apt-cache show openjdk-11-jdk >/dev/null 2>&1; then
        apt install -y openjdk-11-jdk
        print_warning "Java 17 not available, installed Java 11 instead"
        save_config "java_version_installed" "11"
    else
        print_error "No suitable Java version found"
        save_config "java_version_installed" "none"
    fi

    # Install Maven and Git
    print_info "Installing Maven and Git..."
    apt install -y maven git

    # Install additional Python packages via pip if needed
    if command -v pip3 >/dev/null 2>&1; then
        print_info "Installing additional Python packages via pip..."
        pip3 install --upgrade pip 2>/dev/null || true

        # Install podman-compose via pip if not available as package
        if ! command -v podman-compose >/dev/null 2>&1; then
            pip3 install podman-compose 2>/dev/null || {
                print_warning "podman-compose installation failed (optional)"
            }
        fi
    fi

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

    print_success "Development packages installed"
    mark_step_completed "python_install"
else
    print_info "Step 7: Python and packages already installed, skipping..."
fi

# Step 8: Configure Maven settings
if ! is_step_completed "maven_config"; then
    print_info "Step 8: Configuring Maven settings..."

    run_as_user mkdir -p "$SCRIPT_HOME/.m2"

    # Create Maven settings.xml with mirror configuration for better performance
    run_as_user tee "$SCRIPT_HOME/.m2/settings.xml" > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <localRepository>${user.home}/.m2/repository</localRepository>

    <!-- Optional: Add mirrors for faster downloads
    <mirrors>
        <mirror>
            <id>central-mirror</id>
            <mirrorOf>central</mirrorOf>
            <name>Central Mirror</name>
            <url>https://repo1.maven.org/maven2</url>
        </mirror>
    </mirrors>
    -->

    <profiles>
        <profile>
            <id>default</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>
    </profiles>
</settings>
EOF

    save_config "maven_settings_file" "$SCRIPT_HOME/.m2/settings.xml"
    print_success "Maven configured"
    mark_step_completed "maven_config"
else
    print_info "Step 8: Maven already configured, skipping..."
fi

# Step 9: Create directory structure
if ! is_step_completed "directories_created"; then
    print_info "Step 9: Creating directory structure..."
    cd "$SCRIPT_HOME"

    # Create main directories
    run_as_user mkdir -p microservice-kiwi docker storage_data store_path tracker_data
    run_as_user mkdir -p docker/kiwi docker/ui docker/rabbitmq docker/mysql

    # Create Kiwi service directories
    cd "$SCRIPT_HOME/docker/kiwi"
    run_as_user mkdir -p auth config crawler eureka gate upms word ai
    run_as_user mkdir -p auth/logs config/logs crawler/logs crawler/tmp
    run_as_user mkdir -p eureka/logs gate/logs upms/logs word/logs
    run_as_user mkdir -p word/bizTmp word/crawlerTmp word/biz word/crawler
    run_as_user mkdir -p ai/logs ai/tmp ai/biz ai/batch

    # Create UI directories
    cd "$SCRIPT_HOME/docker/ui"
    run_as_user mkdir -p dist nginx

    save_config "directory_structure" "created"
    print_success "Directory structure created"
    mark_step_completed "directories_created"
else
    print_info "Step 9: Directory structure already created, skipping..."
fi

# Step 10: Clone and setup Git repository
if ! is_step_completed "git_setup"; then
    print_info "Step 10: Setting up Git repository..."
    cd "$SCRIPT_HOME/microservice-kiwi/"

    # Check for corrupted git repository
    if [ -d ".git" ]; then
        if ! run_as_user_home git status >/dev/null 2>&1; then
            print_warning "Git repository corrupted, cleaning up..."
            run_as_user rm -rf .git
        fi
    fi

    if [ ! -d ".git" ]; then
        print_info "Initializing Git repository..."
        run_as_user_home git init
        run_as_user_home git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
    fi

    print_info "Fetching latest code..."
    run_as_user_home git fetch --all
    run_as_user_home git reset --hard origin/master
    run_as_user_home git branch --set-upstream-to=origin/master master 2>/dev/null || true
    run_as_user_home git pull

    # Create symbolic links
    print_info "Creating deployment shortcuts..."
    cd "$SCRIPT_HOME"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/deployKiwi.sh" "$SCRIPT_HOME/easy-deploy"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/stopAll.sh" "$SCRIPT_HOME/easy-stop"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/auto_deploy_ui.sh" "$SCRIPT_HOME/easy-deploy-ui"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/initial_ui.sh" "$SCRIPT_HOME/easy-ui-initial"
    run_as_user ln -sf "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker/checkContainers.sh" "$SCRIPT_HOME/easy-check"

    # Make scripts executable
    run_as_user chmod +x "$SCRIPT_HOME/easy-"*

    GIT_COMMIT_HASH=$(run_as_user_home git rev-parse HEAD 2>/dev/null || echo "unknown")
    save_config "git_commit_hash" "$GIT_COMMIT_HASH"

    print_success "Git repository setup completed"
    mark_step_completed "git_setup"
else
    print_info "Step 10: Git repository already set up, skipping..."
fi

# Step 11: Configure hosts file
if ! is_step_completed "hosts_configured"; then
    print_info "Step 11: Configuring hosts file..."

    # Backup hosts file
    cp /etc/hosts /etc/hosts.backup.$(date +%Y%m%d_%H%M%S)

    # Remove existing Kiwi entries
    sed -i '/# Kiwi Infrastructure Services/,/# End Kiwi Services/d' /etc/hosts

    # Add new entries
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

    print_success "Hosts file configured"
    mark_step_completed "hosts_configured"
else
    print_info "Step 11: Hosts file already configured, skipping..."
fi

# Step 12: Setup environment variables
if ! is_step_completed "env_vars_setup"; then
    print_info "Step 12: Setting up environment variables..."

    # Remove old entries
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

    # Add environment variables to .bashrc
    {
        echo ""
        echo "# Kiwi Microservice Environment Variables"
        echo "export KIWI_ENC_PASSWORD=\"$KIWI_ENC_PASSWORD\""
        echo "export DB_IP=\"$INFRASTRUCTURE_IP\""
        echo "export GROK_API_KEY=\"$GROK_API_KEY\""
        echo "export MYSQL_ROOT_PASSWORD=\"$MYSQL_ROOT_PASSWORD\""
        echo "export REDIS_PASSWORD=\"$REDIS_PASSWORD\""
        echo "export FASTDFS_HOSTNAME=\"$FASTDFS_HOSTNAME\""
        echo "export FASTDFS_NON_LOCAL_IP=\"$FASTDFS_NON_LOCAL_IP\""
        echo "export ES_ROOT_PASSWORD=\"$ES_ROOT_PASSWORD\""
        echo "export ES_USER_NAME=\"$ES_USER_NAME\""
        echo "export ES_USER_PASSWORD=\"$ES_USER_PASSWORD\""
        echo "export INFRASTRUCTURE_IP=\"$INFRASTRUCTURE_IP\""
        echo "export SERVICE_IP=\"$SERVICE_IP\""
    } | run_as_user tee -a "$SCRIPT_HOME/.bashrc" > /dev/null

    print_success "Environment variables configured"
    mark_step_completed "env_vars_setup"
else
    print_info "Step 12: Environment variables already set up, skipping..."
fi

# Step 13: Download yt-dlp
if ! is_step_completed "ytdlp_download"; then
    print_info "Step 13: Downloading yt-dlp..."

    YT_DLP_VERSION="2024.12.06"
    wget "https://github.com/yt-dlp/yt-dlp/releases/download/${YT_DLP_VERSION}/yt-dlp_linux" -O /tmp/yt-dlp_linux
    chmod +x /tmp/yt-dlp_linux

    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/biz/"
    run_as_user cp /tmp/yt-dlp_linux "$SCRIPT_HOME/docker/kiwi/ai/batch/"
    rm /tmp/yt-dlp_linux

    print_success "yt-dlp downloaded"
    mark_step_completed "ytdlp_download"
else
    print_info "Step 13: yt-dlp already downloaded, skipping..."
fi

# Step 14: Setup MySQL (Enhanced)
if ! is_step_completed "mysql_setup"; then
    print_info "Step 14: Setting up MySQL..."

    # Check port availability
    if netstat -tlnp 2>/dev/null | grep -q ":3306 "; then
        print_warning "Port 3306 is already in use"
        netstat -tlnp | grep ":3306 "
        read -p "Continue anyway? (y/N): " CONTINUE
        if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
            print_error "MySQL setup aborted"
            exit 1
        fi
    fi

    # Clean up existing container
    docker stop kiwi-mysql 2>/dev/null || true
    docker rm kiwi-mysql 2>/dev/null || true

    # Create MySQL directory
    run_as_user mkdir -p "$SCRIPT_HOME/docker/mysql"

    # Pull and run MySQL
    print_info "Starting MySQL container..."
    docker pull mysql:8.0

    docker run -itd \
        --name kiwi-mysql \
        -p 3306:3306 \
        -v "$SCRIPT_HOME/docker/mysql:/var/lib/mysql" \
        -e MYSQL_ROOT_PASSWORD="$MYSQL_ROOT_PASSWORD" \
        -e MYSQL_DATABASE=kiwi_db \
        --restart=unless-stopped \
        mysql:8.0 \
        --character-set-server=utf8mb4 \
        --collation-server=utf8mb4_unicode_ci \
        --max-connections=1000

    # Wait for MySQL to be ready
    print_info "Waiting for MySQL to start..."
    MYSQL_READY=false
    for i in {1..30}; do
        if docker exec kiwi-mysql mysqladmin ping -h localhost -u root -p"$MYSQL_ROOT_PASSWORD" 2>/dev/null; then
            MYSQL_READY=true
            break
        fi
        sleep 2
    done

    if [ "$MYSQL_READY" = true ]; then
        print_success "MySQL is ready"

        # Check for database backup
        if [ -f "$SCRIPT_HOME/docker/mysql/kiwi-db.sql" ]; then
            print_info "Restoring database backup..."
            docker exec -i kiwi-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" kiwi_db < "$SCRIPT_HOME/docker/mysql/kiwi-db.sql"
            print_success "Database restored"
        fi
    else
        print_error "MySQL failed to start properly"
    fi

    mark_step_completed "mysql_setup"
else
    print_info "Step 14: MySQL already set up, skipping..."
    check_and_start_container "kiwi-mysql" "mysql_setup"
fi

# Step 15: Setup Redis
if ! is_step_completed "redis_setup"; then
    print_info "Step 15: Setting up Redis..."

    docker pull redis:latest
    docker stop kiwi-redis 2>/dev/null || true
    docker rm kiwi-redis 2>/dev/null || true

    docker run -itd \
        --name kiwi-redis \
        -p 6379:6379 \
        --restart=unless-stopped \
        redis --requirepass "$REDIS_PASSWORD"

    print_success "Redis setup completed"
    mark_step_completed "redis_setup"
else
    print_info "Step 15: Redis already set up, skipping..."
    check_and_start_container "kiwi-redis" "redis_setup"
fi

# Step 16: Setup RabbitMQ
if ! is_step_completed "rabbitmq_setup"; then
    print_info "Step 16: Setting up RabbitMQ..."

    docker pull rabbitmq:management
    docker stop kiwi-rabbit 2>/dev/null || true
    docker rm kiwi-rabbit 2>/dev/null || true

    docker run -d \
        --hostname kiwi-rabbit \
        --name kiwi-rabbit \
        -p 5672:5672 \
        -p 15672:15672 \
        -v "$SCRIPT_HOME/docker/rabbitmq:/var/lib/rabbitmq" \
        --restart=unless-stopped \
        rabbitmq:management

    print_success "RabbitMQ setup completed"
    print_info "Management UI: http://localhost:15672 (guest/guest)"
    mark_step_completed "rabbitmq_setup"
else
    print_info "Step 16: RabbitMQ already set up, skipping..."
    check_and_start_container "kiwi-rabbit" "rabbitmq_setup"
fi

# Step 17: Setup FastDFS (Enhanced with architecture detection)
if ! is_step_completed "fastdfs_setup"; then
    print_info "Step 17: Setting up FastDFS..."

    # Architecture-specific image selection
    ARCH=$(uname -m)
    case $ARCH in
        x86_64|amd64)
            FASTDFS_IMAGE="delron/fastdfs:latest"
            ;;
        aarch64|arm64)
            FASTDFS_IMAGE="ygqygq2/fastdfs-nginx:latest"
            ;;
        *)
            FASTDFS_IMAGE="delron/fastdfs:latest"
            print_warning "Unknown architecture, using default image"
            ;;
    esac

    print_info "Using FastDFS image: $FASTDFS_IMAGE"

    # Pull image
    docker pull "$FASTDFS_IMAGE"

    # Clean up existing containers
    docker stop tracker storage 2>/dev/null || true
    docker rm tracker storage 2>/dev/null || true

    # Create directories
    run_as_user mkdir -p "$SCRIPT_HOME/tracker_data" "$SCRIPT_HOME/storage_data" "$SCRIPT_HOME/store_path"

    # Run tracker
    docker run -tid \
        --name tracker \
        -p 22122:22122 \
        -v "$SCRIPT_HOME/tracker_data:/fastdfs/tracker/data" \
        --restart=unless-stopped \
        "$FASTDFS_IMAGE" \
        tracker

    sleep 10

    # Run storage
    docker run -tid \
        --name storage \
        -p 23000:23000 \
        -p 8888:8888 \
        -v "$SCRIPT_HOME/storage_data:/fastdfs/storage/data" \
        -v "$SCRIPT_HOME/store_path:/fastdfs/store_path" \
        -e TRACKER_SERVER="$FASTDFS_NON_LOCAL_IP:22122" \
        --restart=unless-stopped \
        "$FASTDFS_IMAGE" \
        storage

    print_success "FastDFS setup completed"
    save_config "fastdfs_image_used" "$FASTDFS_IMAGE"
    mark_step_completed "fastdfs_setup"
else
    print_info "Step 17: FastDFS already set up, skipping..."
    check_and_start_container "tracker" "fastdfs_setup"
    check_and_start_container "storage" "fastdfs_setup"
fi

# Step 18: Maven library installation
if ! is_step_completed "maven_lib_install"; then
    print_info "Step 18: Installing Maven library..."

    LIB_PATH="$SCRIPT_HOME/microservice-kiwi/kiwi-common/kiwi-common-tts/lib"

    if [ -f "$LIB_PATH/voicerss_tts.jar" ]; then
        cd "$LIB_PATH"
        run_as_user_home mvn install:install-file \
            -Dfile=voicerss_tts.jar \
            -DgroupId=voicerss \
            -DartifactId=tts \
            -Dversion=2.0 \
            -Dpackaging=jar
        print_success "Maven library installed"
    else
        print_warning "voicerss_tts.jar not found in $LIB_PATH"
    fi

    mark_step_completed "maven_lib_install"
else
    print_info "Step 18: Maven library already installed, skipping..."
fi

# Step 19: Setup deployment script
if ! is_step_completed "deployment_setup"; then
    print_info "Step 19: Setting up deployment script..."

    cd "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/docker"

    if [ ! -L "$SCRIPT_HOME/deployKiwi.sh" ]; then
        run_as_user cp deployKiwi.sh "$SCRIPT_HOME/"
        run_as_user chmod +x "$SCRIPT_HOME/deployKiwi.sh"
    fi

    print_success "Deployment script setup completed"
    mark_step_completed "deployment_setup"
else
    print_info "Step 19: Deployment already set up, skipping..."
fi

# Step 20: Setup Elasticsearch
if ! is_step_completed "elasticsearch_setup"; then
    print_info "Step 20: Setting up Elasticsearch..."

    docker pull elasticsearch:7.17.9
    docker stop kiwi-es 2>/dev/null || true
    docker rm kiwi-es 2>/dev/null || true
    docker volume rm es_config es_data 2>/dev/null || true

    docker run -d \
        --name kiwi-es \
        -p 9200:9200 \
        -p 9300:9300 \
        --hostname kiwi-es \
        -e "discovery.type=single-node" \
        -e "xpack.security.enabled=true" \
        -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
        -v es_config:/usr/share/elasticsearch/config \
        -v es_data:/usr/share/elasticsearch/data \
        --restart=unless-stopped \
        elasticsearch:7.17.9

    print_info "Waiting for Elasticsearch to start..."
    sleep 60

    # Create users
    print_info "Creating Elasticsearch users..."
    echo "$ES_ROOT_PASSWORD" | docker exec -i kiwi-es elasticsearch-users useradd root -r superuser -p
    echo "$ES_USER_PASSWORD" | docker exec -i kiwi-es elasticsearch-users useradd "$ES_USER_NAME" -r superuser -p

    # Create index
    docker exec kiwi-es curl -X PUT "localhost:9200/kiwi_vocabulary" \
        -u "root:$ES_ROOT_PASSWORD" \
        -H "Content-Type: application/json" \
        -d '{"settings": {"number_of_shards": 1, "number_of_replicas": 0}}' 2>/dev/null

    print_success "Elasticsearch setup completed"
    mark_step_completed "elasticsearch_setup"
else
    print_info "Step 20: Elasticsearch already set up, skipping..."
    check_and_start_container "kiwi-es" "elasticsearch_setup"
fi

# Step 21: Install IK Tokenizer
if ! is_step_completed "ik_tokenizer_install"; then
    print_info "Step 21: Installing IK Tokenizer..."

    docker exec kiwi-es elasticsearch-plugin install \
        https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.9/elasticsearch-analysis-ik-7.17.9.zip \
        --batch

    docker restart kiwi-es
    sleep 30

    print_success "IK Tokenizer installed"
    mark_step_completed "ik_tokenizer_install"
else
    print_info "Step 21: IK Tokenizer already installed, skipping..."
fi

# Step 22: Setup Nginx and UI
if ! is_step_completed "nginx_ui_setup"; then
    print_info "Step 22: Setting up Nginx and UI..."

    docker pull nginx
    docker stop kiwi-ui 2>/dev/null || true
    docker rm kiwi-ui 2>/dev/null || true

    # Build UI image
    cd "$SCRIPT_HOME"
    if [ -f "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/Dockerfile" ]; then
        docker build -f "$SCRIPT_HOME/microservice-kiwi/kiwi-deploy/kiwi-ui/Dockerfile" \
            -t kiwi-ui:1.0 "$SCRIPT_HOME/docker/ui/"
    else
        print_warning "UI Dockerfile not found, using nginx directly"
        docker tag nginx:latest kiwi-ui:1.0
    fi

    # Run UI container
    docker run -d \
        --name kiwi-ui \
        -p 80:80 \
        -v "$SCRIPT_HOME/docker/ui/dist/:/usr/share/nginx/html" \
        --restart=unless-stopped \
        kiwi-ui:1.0

    print_success "Nginx and UI setup completed"
    mark_step_completed "nginx_ui_setup"
else
    print_info "Step 22: Nginx and UI already set up, skipping..."
    check_and_start_container "kiwi-ui" "nginx_ui_setup"
fi

# Function to display final summary
display_final_summary() {
    echo
    echo "======================================"
    echo -e "${GREEN}SETUP COMPLETED SUCCESSFULLY!${NC}"
    echo "======================================"
    echo "Target user: $SCRIPT_USER"
    echo "Home directory: $SCRIPT_HOME"
    echo "Architecture: $(uname -m)"
    echo "OS Version: $(lsb_release -d 2>/dev/null | cut -f2 || echo 'Unknown')"
    echo
    echo -e "${BLUE}NETWORK CONFIGURATION:${NC}"
    echo "  FastDFS IP: $FASTDFS_NON_LOCAL_IP"
    echo "  Infrastructure IP: $INFRASTRUCTURE_IP"
    echo "  Service IP: $SERVICE_IP"
    echo
    echo -e "${BLUE}AVAILABLE SHORTCUTS:${NC}"
    echo "  $SCRIPT_HOME/easy-deploy     - Deploy Kiwi services"
    echo "  $SCRIPT_HOME/easy-stop       - Stop all services"
    echo "  $SCRIPT_HOME/easy-deploy-ui  - Deploy UI"
    echo "  $SCRIPT_HOME/easy-check      - Check container status"
    echo
    echo -e "${BLUE}CONTAINER STATUS:${NC}"

    # Check container status
    CONTAINERS=("kiwi-mysql" "kiwi-redis" "kiwi-rabbit" "tracker" "storage" "kiwi-es" "kiwi-ui")
    RUNNING_COUNT=0
    TOTAL_COUNT=${#CONTAINERS[@]}

    for container in "${CONTAINERS[@]}"; do
        if docker ps --format '{{.Names}}' 2>/dev/null | grep -q "^${container}$"; then
            echo -e "  ${GREEN}✓${NC} $container - RUNNING"
            ((RUNNING_COUNT++))
        elif docker ps -a --format '{{.Names}}' 2>/dev/null | grep -q "^${container}$"; then
            echo -e "  ${YELLOW}⚠${NC} $container - STOPPED"
        else
            echo -e "  ${RED}✗${NC} $container - NOT FOUND"
        fi
    done

    echo
    echo "Container Summary: $RUNNING_COUNT/$TOTAL_COUNT running"
    echo
    echo -e "${BLUE}WEB INTERFACES:${NC}"
    echo "  RabbitMQ: http://$INFRASTRUCTURE_IP:15672 (guest/guest)"
    echo "  Elasticsearch: http://$INFRASTRUCTURE_IP:9200"
    echo "  Kiwi UI: http://$INFRASTRUCTURE_IP:80"
    echo "  FastDFS: http://$FASTDFS_NON_LOCAL_IP:8888"
    echo
    echo -e "${BLUE}DATABASE CONNECTIONS:${NC}"
    echo "  MySQL: $INFRASTRUCTURE_IP:3306 (root / [password])"
    echo "  Redis: $INFRASTRUCTURE_IP:6379 (password protected)"
    echo
    echo -e "${YELLOW}IMPORTANT NOTES:${NC}"
    echo "1. Run 'source ~/.bashrc' to apply environment variables"
    echo "2. Log out and back in for Docker permissions to take effect"
    echo "3. All configuration saved in: $CONFIG_FILE"
    echo "4. Setup progress saved in: $PROGRESS_FILE"
    echo "5. Complete log available at: $LOG_FILE"
    echo
    echo -e "${GREEN}Setup completed successfully!${NC}"
    echo "======================================"
}

# Function to configure system settings
configure_system_settings() {
    print_info "Configuring system settings..."

    # Disable sleep/hibernation for servers
    if systemctl list-units --all | grep -q "sleep.target"; then
        systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target 2>/dev/null || true
        print_success "System sleep disabled"
    fi

    # Add useful aliases
    if ! grep -q "alias ll=" "$SCRIPT_HOME/.bashrc"; then
        {
            echo ""
            echo "# Useful aliases"
            echo "alias ll='ls -la'"
            echo "alias dp='docker ps'"
            echo "alias dpa='docker ps -a'"
            echo "alias dl='docker logs'"
            echo "alias dex='docker exec -it'"
            echo "alias dps='docker ps --format \"table {{.Names}}\t{{.Status}}\t{{.Ports}}\"'"
        } | run_as_user tee -a "$SCRIPT_HOME/.bashrc" > /dev/null
        print_success "Aliases added"
    fi
}

# Function to perform health checks
perform_health_checks() {
    print_info "Performing system health checks..."

    # Check disk space
    DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ "$DISK_USAGE" -gt 90 ]; then
        print_warning "Disk usage is high: ${DISK_USAGE}%"
    else
        print_success "Disk usage is healthy: ${DISK_USAGE}%"
    fi

    # Check memory
    MEM_USAGE=$(free | grep Mem | awk '{print int($3/$2 * 100)}')
    if [ "$MEM_USAGE" -gt 90 ]; then
        print_warning "Memory usage is high: ${MEM_USAGE}%"
    else
        print_success "Memory usage is healthy: ${MEM_USAGE}%"
    fi

    # Check Docker
    if docker info >/dev/null 2>&1; then
        print_success "Docker is running properly"
    else
        print_error "Docker is not running properly"
    fi

    # Check network connectivity
    if ping -c 1 google.com >/dev/null 2>&1; then
        print_success "Internet connectivity is working"
    else
        print_warning "Internet connectivity issues detected"
    fi
}

# Function to create backup
create_backup() {
    print_info "Creating configuration backup..."

    BACKUP_DIR="$SCRIPT_HOME/kiwi-backup-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$BACKUP_DIR"

    # Backup configuration files
    cp "$CONFIG_FILE" "$BACKUP_DIR/" 2>/dev/null || true
    cp "$PROGRESS_FILE" "$BACKUP_DIR/" 2>/dev/null || true
    cp "$LOG_FILE" "$BACKUP_DIR/" 2>/dev/null || true
    cp "$SCRIPT_HOME/.bashrc" "$BACKUP_DIR/" 2>/dev/null || true
    cp /etc/hosts "$BACKUP_DIR/" 2>/dev/null || true

    # Create backup info
    cat > "$BACKUP_DIR/backup-info.txt" << EOF
Kiwi Microservice Backup
Created: $(date)
User: $SCRIPT_USER
Home: $SCRIPT_HOME
Docker Version: $(docker --version 2>/dev/null || echo "unknown")
Containers: $(docker ps -q | wc -l) running
EOF

    print_success "Backup created at: $BACKUP_DIR"
}

# Function to offer post-installation options
post_installation_menu() {
    echo
    echo "======================================"
    echo "POST-INSTALLATION OPTIONS"
    echo "======================================"
    echo "1. Start all stopped containers"
    echo "2. Stop all running containers"
    echo "3. View container logs"
    echo "4. Create backup"
    echo "5. Run health checks"
    echo "6. Show connection info"
    echo "7. Exit"
    echo
    read -p "Select an option (1-7): " POST_OPTION

    case $POST_OPTION in
        1)
            print_info "Starting all containers..."
            for container in kiwi-mysql kiwi-redis kiwi-rabbit tracker storage kiwi-es kiwi-ui; do
                docker start $container 2>/dev/null && print_success "$container started" || true
            done
            ;;
        2)
            print_info "Stopping all containers..."
            for container in kiwi-mysql kiwi-redis kiwi-rabbit tracker storage kiwi-es kiwi-ui; do
                docker stop $container 2>/dev/null && print_success "$container stopped" || true
            done
            ;;
        3)
            read -p "Enter container name: " CONTAINER_NAME
            docker logs --tail 50 $CONTAINER_NAME
            ;;
        4)
            create_backup
            ;;
        5)
            perform_health_checks
            ;;
        6)
            display_connection_info
            ;;
        7)
            print_info "Exiting..."
            ;;
        *)
            print_warning "Invalid option"
            ;;
    esac
}

# Function to display connection info
display_connection_info() {
    echo
    echo "======================================"
    echo "CONNECTION INFORMATION"
    echo "======================================"
    echo
    echo "MySQL:"
    echo "  Host: $INFRASTRUCTURE_IP"
    echo "  Port: 3306"
    echo "  User: root"
    echo "  Password: [Your MySQL password]"
    echo "  Database: kiwi_db"
    echo "  Connection: mysql -h $INFRASTRUCTURE_IP -P 3306 -u root -p"
    echo
    echo "Redis:"
    echo "  Host: $INFRASTRUCTURE_IP"
    echo "  Port: 6379"
    echo "  Password: [Your Redis password]"
    echo "  Connection: redis-cli -h $INFRASTRUCTURE_IP -p 6379 -a [password]"
    echo
    echo "RabbitMQ:"
    echo "  Management URL: http://$INFRASTRUCTURE_IP:15672"
    echo "  AMQP Port: 5672"
    echo "  Username: guest"
    echo "  Password: guest"
    echo
    echo "Elasticsearch:"
    echo "  URL: http://$INFRASTRUCTURE_IP:9200"
    echo "  User: root"
    echo "  Password: [Your ES password]"
    echo "  Test: curl -u root:[password] http://$INFRASTRUCTURE_IP:9200/_cluster/health?pretty"
    echo
    echo "FastDFS:"
    echo "  Tracker: $FASTDFS_NON_LOCAL_IP:22122"
    echo "  Storage: $FASTDFS_NON_LOCAL_IP:23000"
    echo "  HTTP: http://$FASTDFS_NON_LOCAL_IP:8888"
    echo
    echo "Web UI:"
    echo "  URL: http://$INFRASTRUCTURE_IP:80"
    echo
    echo "======================================"
}

# Main execution completion
configure_system_settings
perform_health_checks
display_final_summary

# Offer post-installation options
read -p "Would you like to access post-installation options? (y/N): " ACCESS_POST
if [[ "$ACCESS_POST" =~ ^[Yy]$ ]]; then
    post_installation_menu
fi

# Final message
echo
print_success "Enhanced Kiwi Microservice setup completed!"
print_info "Log file available at: $LOG_FILE"
print_info "Run 'source ~/.bashrc' to apply environment changes"
print_info "Thank you for using the enhanced setup script!"
echo "======================================"

# Exit successfully
exit 0