#!/bin/bash

# Kiwi Microservice Cleanup Script for macOS
# This script removes all resources created by the Kiwi setup script

set +e  # Continue on errors (some resources might not exist)

# Configuration
PROGRESS_FILE="$(pwd)/.kiwi_setup_progress"
CONFIG_FILE="$(pwd)/.kiwi_setup_config"
SCRIPT_USER=${USER}
SCRIPT_HOME="$HOME"

echo "=============================================="
echo "Kiwi Microservice Cleanup Script for macOS"
echo "Running as: $(whoami)"
echo "Target user: $SCRIPT_USER"
echo "Target home: $SCRIPT_HOME"
echo "=============================================="

# Function to prompt for confirmation
confirm_action() {
    local action="$1"
    echo
    read -p "Do you want to $action? (y/N): " confirm
    if [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]]; then
        return 0
    else
        echo "Skipping: $action"
        return 1
    fi
}

# Function to show cleanup menu
show_cleanup_menu() {
    echo
    echo "=============================================="
    echo "CLEANUP OPTIONS MENU"
    echo "=============================================="
    echo "Choose what to clean up:"
    echo "1. Clean Docker containers and images only"
    echo "2. Clean directories and files only"
    echo "3. Clean environment variables only"
    echo "4. Clean everything (containers, images, files, env vars)"
    echo "5. Show current status and exit"
    echo "0. Exit without cleaning"
    echo
    read -p "Enter your choice (0-5) [default: 4]: " CLEANUP_MODE

    # Default to 4 if empty
    CLEANUP_MODE=${CLEANUP_MODE:-4}

    case $CLEANUP_MODE in
        1)
            CLEAN_DOCKER=true
            CLEAN_FILES=false
            CLEAN_ENV=false
            ;;
        2)
            CLEAN_DOCKER=false
            CLEAN_FILES=true
            CLEAN_ENV=false
            ;;
        3)
            CLEAN_DOCKER=false
            CLEAN_FILES=false
            CLEAN_ENV=true
            ;;
        4)
            CLEAN_DOCKER=true
            CLEAN_FILES=true
            CLEAN_ENV=true
            ;;
        5)
            show_current_status
            exit 0
            ;;
        0)
            echo "Exiting without cleaning..."
            exit 0
            ;;
        *)
            echo "Invalid choice. Exiting..."
            exit 1
            ;;
    esac
}

# Function to show current status
show_current_status() {
    echo
    echo "=============================================="
    echo "CURRENT KIWI MICROSERVICE STATUS"
    echo "=============================================="

    echo "DOCKER CONTAINERS:"
    echo "=================="
    CONTAINERS=("kiwi-mysql" "kiwi-redis" "kiwi-rabbit" "tracker" "storage" "kiwi-es" "kiwi-ui")

    for container in "${CONTAINERS[@]}"; do
        if docker ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
            if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
                echo "✓ $container - RUNNING"
            else
                echo "⚠ $container - STOPPED"
            fi
        else
            echo "✗ $container - NOT FOUND"
        fi
    done

    echo
    echo "DOCKER IMAGES:"
    echo "=============="
    echo "MySQL images:"
    docker images mysql --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"
    echo "Redis images:"
    docker images redis --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"
    echo "RabbitMQ images:"
    docker images rabbitmq --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"
    echo "FastDFS images:"
    docker images delron/fastdfs --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"
    echo "Elasticsearch images:"
    docker images elasticsearch --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"
    echo "Nginx images:"
    docker images nginx --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"
    echo "Kiwi UI images:"
    docker images kiwi-ui --format "  {{.Repository}}:{{.Tag}} ({{.Size}})" 2>/dev/null || echo "  None found"

    echo
    echo "DOCKER VOLUMES:"
    echo "==============="
    docker volume ls --format "{{.Name}}" | grep -E "(es_config|es_data)" | sed 's/^/  /' || echo "  No Kiwi-related volumes found"

    echo
    echo "DIRECTORIES AND FILES:"
    echo "======================"

    DIRECTORIES=("microservice-kiwi" "docker" "storage_data" "store_path" "tracker_data")
    for dir in "${DIRECTORIES[@]}"; do
        if [ -d "$SCRIPT_HOME/$dir" ]; then
            dir_size=$(du -sh "$SCRIPT_HOME/$dir" 2>/dev/null | cut -f1)
            echo "✓ $SCRIPT_HOME/$dir (Size: $dir_size)"
        else
            echo "✗ $SCRIPT_HOME/$dir - NOT FOUND"
        fi
    done

    echo
    echo "SHORTCUTS:"
    echo "=========="
    SHORTCUTS=("easy-deploy" "easy-stop" "easy-deploy-ui" "easy-check" "easy-setup" "easy-clean-setup" "deployKiwi.sh")
    for shortcut in "${SHORTCUTS[@]}"; do
        if [ -L "$SCRIPT_HOME/$shortcut" ] || [ -f "$SCRIPT_HOME/$shortcut" ]; then
            if [ -L "$SCRIPT_HOME/$shortcut" ]; then
                echo "✓ $SCRIPT_HOME/$shortcut (symbolic link)"
            else
                echo "✓ $SCRIPT_HOME/$shortcut (file)"
            fi
        else
            echo "✗ $SCRIPT_HOME/$shortcut - NOT FOUND"
        fi
    done

    echo
    echo "CONFIGURATION FILES:"
    echo "==================="
    if [ -f "$PROGRESS_FILE" ]; then
        progress_count=$(wc -l < "$PROGRESS_FILE" 2>/dev/null || echo "0")
        echo "✓ $PROGRESS_FILE ($progress_count completed steps)"
    else
        echo "✗ $PROGRESS_FILE - NOT FOUND"
    fi

    if [ -f "$CONFIG_FILE" ]; then
        config_count=$(wc -l < "$CONFIG_FILE" 2>/dev/null || echo "0")
        echo "✓ $CONFIG_FILE ($config_count configuration items)"
    else
        echo "✗ $CONFIG_FILE - NOT FOUND"
    fi

    echo
    echo "ENVIRONMENT VARIABLES:"
    echo "======================"
    ENV_VARS=("KIWI_ENC_PASSWORD" "GROK_API_KEY" "DB_IP" "MYSQL_ROOT_PASSWORD" "REDIS_PASSWORD" "FASTDFS_HOSTNAME" "ES_ROOT_PASSWORD" "ES_USER_NAME" "ES_USER_PASSWORD")

    # Check shell profiles
    SHELL_PROFILES=("$HOME/.zshrc" "$HOME/.bash_profile" "$HOME/.bashrc")
    for profile in "${SHELL_PROFILES[@]}"; do
        if [ -f "$profile" ]; then
            echo "Checking $profile:"
            for var in "${ENV_VARS[@]}"; do
                if grep -q "export $var=" "$profile" 2>/dev/null; then
                    echo "  ✓ $var is set"
                fi
            done
        fi
    done

    echo
    echo "HOSTS FILE ENTRIES:"
    echo "=================="
    HOSTS_ENTRIES=("fastdfs.fengorz.me" "kiwi-microservice-local" "kiwi-microservice" "kiwi-ui" "kiwi-eureka" "kiwi-redis" "kiwi-rabbitmq" "kiwi-db" "kiwi-es" "kiwi-config" "kiwi-auth" "kiwi-upms" "kiwi-gate" "kiwi-ai" "kiwi-crawler")

    for entry in "${HOSTS_ENTRIES[@]}"; do
        if grep -q "$entry" /etc/hosts 2>/dev/null; then
            echo "✓ $entry"
        else
            echo "✗ $entry - NOT FOUND"
        fi
    done
}

# Function to clean Docker resources
clean_docker_resources() {
    echo
    echo "=============================================="
    echo "CLEANING DOCKER RESOURCES"
    echo "=============================================="

    # Stop and remove containers
    echo "Stopping and removing Kiwi containers..."
    CONTAINERS=("kiwi-mysql" "kiwi-redis" "kiwi-rabbit" "tracker" "storage" "kiwi-es" "kiwi-ui")

    for container in "${CONTAINERS[@]}"; do
        echo "Processing container: $container"
        if docker ps -q -f name="^${container}$" >/dev/null 2>&1; then
            echo "  Stopping $container..."
            docker stop "$container" >/dev/null 2>&1
        fi

        if docker ps -aq -f name="^${container}$" >/dev/null 2>&1; then
            echo "  Removing $container..."
            docker rm "$container" >/dev/null 2>&1
        fi
        echo "  ✓ $container processed"
    done

    # Remove Docker volumes
    echo
    echo "Removing Docker volumes..."
    VOLUMES=("es_config" "es_data")
    for volume in "${VOLUMES[@]}"; do
        if docker volume ls -q | grep -q "^${volume}$"; then
            echo "  Removing volume: $volume"
            docker volume rm "$volume" >/dev/null 2>&1
            echo "  ✓ $volume removed"
        else
            echo "  ✗ Volume $volume not found"
        fi
    done

    # Remove Docker images (optional)
    if confirm_action "remove Docker images (mysql, redis, rabbitmq, elasticsearch, nginx, fastdfs, kiwi-ui)"; then
        echo "Removing Docker images..."

        # Remove images
        IMAGES=("mysql:latest" "redis:latest" "rabbitmq:management" "delron/fastdfs" "elasticsearch:7.17.9" "nginx:latest" "kiwi-ui:1.0")
        for image in "${IMAGES[@]}"; do
            if docker images -q "$image" >/dev/null 2>&1; then
                echo "  Removing image: $image"
                docker rmi "$image" >/dev/null 2>&1
                echo "  ✓ $image removed"
            else
                echo "  ✗ Image $image not found"
            fi
        done

        # Clean up dangling images
        echo "Cleaning up dangling images..."
        docker image prune -f >/dev/null 2>&1
        echo "✓ Dangling images cleaned"
    fi

    echo "✓ Docker cleanup completed"
}

# Function to clean directories and files
clean_files_and_directories() {
    echo
    echo "=============================================="
    echo "CLEANING DIRECTORIES AND FILES"
    echo "=============================================="

    # Remove main directories
    if confirm_action "remove main Kiwi directories (microservice-kiwi, docker, storage_data, store_path, tracker_data)"; then
        echo "Removing main directories..."

        DIRECTORIES=("microservice-kiwi" "docker" "storage_data" "store_path" "tracker_data")
        for dir in "${DIRECTORIES[@]}"; do
            if [ -d "$SCRIPT_HOME/$dir" ]; then
                echo "  Removing directory: $SCRIPT_HOME/$dir"
                rm -rf "$SCRIPT_HOME/$dir"
                echo "  ✓ $dir removed"
            else
                echo "  ✗ Directory $dir not found"
            fi
        done
    fi

    # Remove shortcuts and scripts
    if confirm_action "remove Kiwi shortcuts and scripts"; then
        echo "Removing shortcuts and scripts..."

        SHORTCUTS=("easy-deploy" "easy-stop" "easy-deploy-ui" "easy-check" "easy-setup" "easy-clean-setup" "deployKiwi.sh")
        for shortcut in "${SHORTCUTS[@]}"; do
            if [ -L "$SCRIPT_HOME/$shortcut" ] || [ -f "$SCRIPT_HOME/$shortcut" ]; then
                echo "  Removing: $SCRIPT_HOME/$shortcut"
                rm -f "$SCRIPT_HOME/$shortcut"
                echo "  ✓ $shortcut removed"
            else
                echo "  ✗ $shortcut not found"
            fi
        done
    fi

    # Remove configuration files
    if confirm_action "remove setup configuration files (.kiwi_setup_progress, .kiwi_setup_config)"; then
        echo "Removing configuration files..."

        if [ -f "$PROGRESS_FILE" ]; then
            echo "  Removing: $PROGRESS_FILE"
            rm -f "$PROGRESS_FILE"
            echo "  ✓ Progress file removed"
        else
            echo "  ✗ Progress file not found"
        fi

        if [ -f "$CONFIG_FILE" ]; then
            echo "  Removing: $CONFIG_FILE"
            rm -f "$CONFIG_FILE"
            echo "  ✓ Config file removed"
        else
            echo "  ✗ Config file not found"
        fi
    fi

    echo "✓ Files and directories cleanup completed"
}

# Function to clean environment variables
clean_environment_variables() {
    echo
    echo "=============================================="
    echo "CLEANING ENVIRONMENT VARIABLES"
    echo "=============================================="

    if confirm_action "remove Kiwi environment variables from shell profiles"; then
        echo "Cleaning environment variables from shell profiles..."

        ENV_VARS=("KIWI_ENC_PASSWORD" "GROK_API_KEY" "DB_IP" "MYSQL_ROOT_PASSWORD" "REDIS_PASSWORD" "FASTDFS_HOSTNAME" "ES_ROOT_PASSWORD" "ES_USER_NAME" "ES_USER_PASSWORD")
        SHELL_PROFILES=("$HOME/.zshrc" "$HOME/.bash_profile" "$HOME/.bashrc")

        for profile in "${SHELL_PROFILES[@]}"; do
            if [ -f "$profile" ]; then
                echo "  Cleaning $profile..."

                # Create backup
                cp "$profile" "${profile}.kiwi-backup-$(date +%Y%m%d-%H%M%S)"
                echo "    ✓ Backup created: ${profile}.kiwi-backup-$(date +%Y%m%d-%H%M%S)"

                # Remove environment variables
                for var in "${ENV_VARS[@]}"; do
                    if grep -q "export $var=" "$profile" 2>/dev/null; then
                        sed -i '' "/export $var=/d" "$profile" 2>/dev/null
                        echo "    ✓ Removed $var"
                    fi
                done
            else
                echo "  ✗ Profile $profile not found"
            fi
        done

        echo "  ✓ Environment variables cleaned from shell profiles"
        echo "  ⚠ Note: You may need to restart your terminal or run 'source ~/.zshrc' to apply changes"
    fi
}

# Function to clean hosts file
clean_hosts_file() {
    echo
    echo "=============================================="
    echo "CLEANING HOSTS FILE"
    echo "=============================================="

    if confirm_action "remove Kiwi entries from /etc/hosts file (requires sudo)"; then
        echo "Cleaning hosts file entries..."

        HOSTS_ENTRIES=("fastdfs.fengorz.me" "kiwi-microservice-local" "kiwi-microservice" "kiwi-ui" "kiwi-eureka" "kiwi-redis" "kiwi-rabbitmq" "kiwi-db" "kiwi-es" "kiwi-config" "kiwi-auth" "kiwi-upms" "kiwi-gate" "kiwi-ai" "kiwi-crawler")

        # Create backup of hosts file
        sudo cp /etc/hosts "/etc/hosts.kiwi-backup-$(date +%Y%m%d-%H%M%S)"
        echo "✓ Hosts file backup created"

        # Remove entries
        for entry in "${HOSTS_ENTRIES[@]}"; do
            if grep -q "$entry" /etc/hosts 2>/dev/null; then
                sudo sed -i '' "/$entry/d" /etc/hosts
                echo "  ✓ Removed $entry"
            else
                echo "  ✗ Entry $entry not found"
            fi
        done

        echo "✓ Hosts file cleanup completed"
    fi
}

# Function to clean Homebrew packages (optional)
clean_homebrew_packages() {
    echo
    echo "=============================================="
    echo "CLEANING HOMEBREW PACKAGES (OPTIONAL)"
    echo "=============================================="

    if command -v brew &> /dev/null; then
        if confirm_action "remove Homebrew packages installed for Kiwi (maven, git - WARNING: these might be used by other projects)"; then
            echo "Removing Homebrew packages..."

            # Note: We don't remove Docker Desktop as it might be used for other projects
            PACKAGES=("maven")

            for package in "${PACKAGES[@]}"; do
                if brew list "$package" >/dev/null 2>&1; then
                    echo "  Removing: $package"
                    brew uninstall "$package" 2>/dev/null
                    echo "  ✓ $package removed"
                else
                    echo "  ✗ Package $package not installed via Homebrew"
                fi
            done

            echo "  ⚠ Note: Git and Docker Desktop were NOT removed as they might be used by other projects"
            echo "  ⚠ If you want to remove them: brew uninstall git && brew uninstall --cask docker"
        fi
    else
        echo "Homebrew not found, skipping package cleanup"
    fi
}

# Main execution
echo "This script will help you clean up resources created by the Kiwi Microservice setup."
echo
echo "⚠  WARNING: This will permanently delete data and containers!"
echo "⚠  Make sure you have backed up any important data!"
echo

# Show menu
show_cleanup_menu

echo
echo "=============================================="
echo "STARTING CLEANUP PROCESS"
echo "=============================================="
echo "Cleanup mode selected:"
echo "  Docker resources: $CLEAN_DOCKER"
echo "  Files and directories: $CLEAN_FILES"
echo "  Environment variables: $CLEAN_ENV"
echo

if ! confirm_action "proceed with the cleanup"; then
    echo "Cleanup cancelled by user."
    exit 0
fi

# Execute cleanup based on selection
if [ "$CLEAN_DOCKER" = true ]; then
    clean_docker_resources
fi

if [ "$CLEAN_FILES" = true ]; then
    clean_files_and_directories
fi

if [ "$CLEAN_ENV" = true ]; then
    clean_environment_variables
    clean_hosts_file
fi

# Optional Homebrew cleanup (only in full cleanup mode)
if [ "$CLEAN_DOCKER" = true ] && [ "$CLEAN_FILES" = true ] && [ "$CLEAN_ENV" = true ]; then
    clean_homebrew_packages
fi

echo
echo "=============================================="
echo "CLEANUP COMPLETED SUCCESSFULLY!"
echo "=============================================="

# Final status check
echo "FINAL STATUS CHECK:"
echo "==================="

# Check remaining containers
echo "Remaining Kiwi containers:"
REMAINING_CONTAINERS=$(docker ps -a --format '{{.Names}}' | grep -E "(kiwi-|tracker|storage)" 2>/dev/null || true)
if [ -z "$REMAINING_CONTAINERS" ]; then
    echo "  ✓ No Kiwi containers remaining"
else
    echo "  ⚠ Found remaining containers:"
    echo "$REMAINING_CONTAINERS" | sed 's/^/    /'
fi

# Check remaining images
echo "Remaining Kiwi images:"
REMAINING_IMAGES=$(docker images --format '{{.Repository}}:{{.Tag}}' | grep -E "(mysql|redis|rabbitmq|elasticsearch|nginx|kiwi-ui|delron/fastdfs)" 2>/dev/null || true)
if [ -z "$REMAINING_IMAGES" ]; then
    echo "  ✓ No Kiwi images remaining"
else
    echo "  ⚠ Found remaining images:"
    echo "$REMAINING_IMAGES" | sed 's/^/    /'
fi

# Check remaining directories
echo "Remaining Kiwi directories:"
REMAINING_DIRS=""
DIRECTORIES=("microservice-kiwi" "docker" "storage_data" "store_path" "tracker_data")
for dir in "${DIRECTORIES[@]}"; do
    if [ -d "$SCRIPT_HOME/$dir" ]; then
        REMAINING_DIRS="$REMAINING_DIRS $dir"
    fi
done

if [ -z "$REMAINING_DIRS" ]; then
    echo "  ✓ No Kiwi directories remaining"
else
    echo "  ⚠ Found remaining directories:"
    for dir in $REMAINING_DIRS; do
        echo "    $SCRIPT_HOME/$dir"
    done
fi

echo
echo "CLEANUP SUMMARY:"
echo "================"
echo "✓ Cleanup process completed"
echo "✓ Most Kiwi Microservice resources have been removed"
echo
echo "NEXT STEPS:"
echo "==========="
echo "1. Restart your terminal or run 'source ~/.zshrc' to apply environment changes"
echo "2. If you want to reinstall Kiwi, run the setup script again"
echo "3. Check 'docker ps -a' and 'docker images' to verify cleanup"
echo "4. Check your shell profile files for any remaining environment variables"
echo
echo "MANUAL CLEANUP (if needed):"
echo "============================"
echo "If some resources weren't automatically removed, you can manually clean them:"
echo "  - Remove containers: docker rm -f \$(docker ps -aq)"
echo "  - Remove images: docker rmi \$(docker images -q)"
echo "  - Remove volumes: docker volume prune -f"
echo "  - Remove networks: docker network prune -f"
echo "  - Clean Docker completely: docker system prune -a --volumes -f"
echo
echo "=============================================="