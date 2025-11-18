#!/bin/bash

# Kiwi Microservice Cleanup Script for Raspberry Pi OS
# This script cleans up progress tracking and configuration files
# and can perform a deep clean of Docker, Maven, repo, env/hosts, and setup files
# Updated: manage files under user's home (to match set_up.sh) with legacy fallback

set -e  # Exit on any error

# Configuration / Identity
SCRIPT_USER=${SUDO_USER:-$USER}
SCRIPT_HOME=$(eval echo ~$SCRIPT_USER)

# Primary (matches set_up.sh)
HOME_BASE_DIR="$SCRIPT_HOME"
HOME_PROGRESS_FILE="$HOME_BASE_DIR/.kiwi_setup_progress"
HOME_CONFIG_FILE="$HOME_BASE_DIR/.kiwi_setup_config"
HOME_LOG_FILE="$HOME_BASE_DIR/.kiwi_setup.log"

# Legacy (older clean script stored under /root path)
LEGACY_BASE_DIR="$HOME/microservice-kiwi/kiwi-deploy"
LEGACY_PROGRESS_FILE="$LEGACY_BASE_DIR/.kiwi_setup_progress"
LEGACY_CONFIG_FILE="$LEGACY_BASE_DIR/.kiwi_setup_config"
LEGACY_LOG_FILE="$LEGACY_BASE_DIR/.kiwi_setup.log"

# Default file targets are the home-based ones
PROGRESS_FILE="$HOME_PROGRESS_FILE"
CONFIG_FILE="$HOME_CONFIG_FILE"
LOG_FILE="$HOME_LOG_FILE"

mkdir -p "$HOME_BASE_DIR" 2>/dev/null || true

# Detect Docker command (with sudo fallback)
DOCKER_CMD=""
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  DOCKER_CMD="docker"
elif command -v sudo >/dev/null 2>&1 && sudo docker info >/dev/null 2>&1; then
  DOCKER_CMD="sudo docker"
fi

# Portable sed -i (creates a .bak backup file)
sed_delete_lines() {
  # usage: sed_delete_lines "pattern" file
  local pattern="$1" file="$2"
  [ -f "$file" ] || return 0
  sed -i.bak "$pattern" "$file" 2>/dev/null || {
    # If sed -i not supported, fall back to temp file method
    local tmp
    tmp="$(mktemp)"
    sed "$pattern" "$file" > "$tmp" && mv "$tmp" "$file"
  }
}

# Summary header
echo "=================================="
echo "Kiwi Microservice Setup Cleanup"
echo "Running as: $(whoami)"
echo "Target user: $SCRIPT_USER"
echo "Target home: $SCRIPT_HOME"
echo "Files (primary): $HOME_BASE_DIR"
echo "Legacy files (fallback): $LEGACY_BASE_DIR"
echo "=================================="

# Function to display menu
show_menu() {
    echo
    echo "CLEANUP OPTIONS:"
    echo "1. Clean progress file only (keep saved passwords/config)"
    echo "2. Clean config file only (keep progress, remove saved passwords)"
    echo "3. Clean both progress and config files (full reset)"
    echo "4. Show current status"
    echo "5. Backup files before cleaning"
    echo "6. Deep clean EVERYTHING (Docker + Maven .m2 + repo + env + hosts + dirs + shortcuts)"
    echo "7. Exit"
    echo
}

# Function to show current status
show_status() {
    echo
    echo "CURRENT STATUS:"
    echo "=============="

    echo "Primary (home-based) files:"
    echo "Progress file: $HOME_PROGRESS_FILE"
    if [ -f "$HOME_PROGRESS_FILE" ]; then
        echo "  ✓ EXISTS ($(wc -l < \"$HOME_PROGRESS_FILE\") completed steps)"
        echo "  Size: $(ls -lh \"$HOME_PROGRESS_FILE\" | awk '{print $5}')"
        echo "  Last modified: $(ls -l \"$HOME_PROGRESS_FILE\" | awk '{print $6, $7, $8}')"
    else
        echo "  ✗ DOES NOT EXIST"
    fi

    echo
    echo "Config file: $HOME_CONFIG_FILE"
    if [ -f "$HOME_CONFIG_FILE" ]; then
        echo "  ✓ EXISTS ($(wc -l < \"$HOME_CONFIG_FILE\") configuration entries)"
        echo "  Size: $(ls -lh \"$HOME_CONFIG_FILE\" | awk '{print $5}')"
        echo "  Last modified: $(ls -l \"$HOME_CONFIG_FILE\" | awk '{print $6, $7, $8}')"
    else
        echo "  ✗ DOES NOT EXIST"
    fi

    echo
    echo "Log file: $HOME_LOG_FILE"
    if [ -f "$HOME_LOG_FILE" ]; then
        echo "  ✓ EXISTS"
        echo "  Size: $(ls -lh \"$HOME_LOG_FILE\" | awk '{print $5}')"
        echo "  Last modified: $(ls -l \"$HOME_LOG_FILE\" | awk '{print $6, $7, $8}')"
        echo "  Last 5 lines:"
        tail -n 5 "$HOME_LOG_FILE" || true
    else
        echo "  ✗ DOES NOT EXIST"
    fi

    echo
    echo "Legacy (root-based) files:"
    echo "Progress: $LEGACY_PROGRESS_FILE  $( [ -f "$LEGACY_PROGRESS_FILE" ] && echo "✓" || echo "✗" )"
    echo "Config:   $LEGACY_CONFIG_FILE    $( [ -f "$LEGACY_CONFIG_FILE" ] && echo "✓" || echo "✗" )"
    echo "Log:      $LEGACY_LOG_FILE       $( [ -f "$LEGACY_LOG_FILE" ] && echo "✓" || echo "✗" )"

    echo
    if [ -f "$HOME_PROGRESS_FILE" ]; then
        echo "COMPLETED STEPS (home-based):"
        echo "============================="
        sed 's/^/  ✓ /' "$HOME_PROGRESS_FILE"
    fi

    echo
    if [ -f "$HOME_CONFIG_FILE" ]; then
        echo "SAVED CONFIGURATIONS (passwords hidden):"
        echo "======================================="
        while IFS='=' read -r key value; do
            if [[ "$key" =~ PASSWORD|API_KEY ]]; then
                echo "  $key=[HIDDEN]"
            else
                echo "  $key=$value"
            fi
        done < "$HOME_CONFIG_FILE"
    fi

    echo
    echo "Docker availability:"
    if [ -n "$DOCKER_CMD" ]; then
        echo "  ✓ Docker is available via: $DOCKER_CMD"
        $DOCKER_CMD ps --format '{{.Names}}: {{.Status}}' 2>/dev/null | sed 's/^/  - /' || true
    else
        echo "  ✗ Docker not available (or requires sudo not configured)"
    fi
}

# Function to backup files
backup_files() {
    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local backup_dir="$HOME_BASE_DIR/kiwi_setup_backups"

    echo "Creating backup directory: $backup_dir"
    mkdir -p "$backup_dir"

    if [ -f "$HOME_PROGRESS_FILE" ]; then
        local progress_backup="$backup_dir/.kiwi_setup_progress_$timestamp"
        cp "$HOME_PROGRESS_FILE" "$progress_backup"
        echo "✓ Progress file backed up to: $progress_backup"
    else
        echo "⚠ No progress file to backup (home-based)"
    fi

    if [ -f "$HOME_CONFIG_FILE" ]; then
        local config_backup="$backup_dir/.kiwi_setup_config_$timestamp"
        cp "$HOME_CONFIG_FILE" "$config_backup"
        chmod 600 "$config_backup"  # Maintain secure permissions
        echo "✓ Config file backed up to: $config_backup"
    else
        echo "⚠ No config file to backup (home-based)"
    fi

    if [ -f "$HOME_LOG_FILE" ]; then
        local log_backup="$backup_dir/.kiwi_setup.log_$timestamp"
        cp "$HOME_LOG_FILE" "$log_backup"
        echo "✓ Log file backed up to: $log_backup"
    fi

    echo "✓ Backup completed in: $backup_dir"
}

# Function to clean progress file
clean_progress() {
    if [ -f "$HOME_PROGRESS_FILE" ]; then
        echo "Removing progress file: $HOME_PROGRESS_FILE"
        rm -f "$HOME_PROGRESS_FILE"
        echo "✓ Progress file removed successfully"
        echo "  Next setup run will start from the beginning"
    elif [ -f "$LEGACY_PROGRESS_FILE" ]; then
        echo "Removing legacy progress file: $LEGACY_PROGRESS_FILE"
        rm -f "$LEGACY_PROGRESS_FILE"
        echo "✓ Legacy progress file removed"
    else
        echo "⚠ Progress file does not exist (home or legacy)"
    fi
}

# Function to clean config file
clean_config() {
    if [ -f "$HOME_CONFIG_FILE" ]; then
        echo "Removing config file: $HOME_CONFIG_FILE"
        rm -f "$HOME_CONFIG_FILE"
        echo "✓ Config file removed successfully"
        echo "  Next setup run will prompt for all passwords and settings again"
    elif [ -f "$LEGACY_CONFIG_FILE" ]; then
        echo "Removing legacy config file: $LEGACY_CONFIG_FILE"
        rm -f "$LEGACY_CONFIG_FILE"
        echo "✓ Legacy config file removed"
    else
        echo "⚠ Config file does not exist (home or legacy)"
    fi
}

# Function to clean both files
clean_both() {
    echo "Removing both progress and config files..."
    clean_progress
    clean_config
    echo "✓ Full setup-file cleanup completed"
    echo "  Next setup run will be completely fresh"
}

# Function to confirm action
confirm_action() {
    local action="$1"
    echo
    echo "⚠ WARNING: This will $action"
    read -p "Are you sure? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Action cancelled."
        return 1
    fi
    return 0
}

# Deep clean helpers
remove_shell_exports() {
    # Clean environment exports in both bashrc and zshrc
    for rc in "$SCRIPT_HOME/.bashrc" "$SCRIPT_HOME/.zshrc"; do
        [ -f "$rc" ] || continue
        echo "Cleaning environment exports in: $rc (backup: .bak)"
        sed_delete_lines '/export KIWI_ENC_PASSWORD=/d' "$rc"
        sed_delete_lines '/export GROK_API_KEY=/d' "$rc"
        sed_delete_lines '/export DB_IP=/d' "$rc"
        sed_delete_lines '/export MYSQL_ROOT_PASSWORD=/d' "$rc"
        sed_delete_lines '/export REDIS_PASSWORD=/d' "$rc"
        sed_delete_lines '/export ES_ROOT_PASSWORD=/d' "$rc"
        sed_delete_lines '/export ES_USER_NAME=/d' "$rc"
        sed_delete_lines '/export ES_USER_PASSWORD=/d' "$rc"
        sed_delete_lines '/export INFRASTRUCTURE_IP=/d' "$rc"
        sed_delete_lines '/export SERVICE_IP=/d' "$rc"
    done
}

remove_hosts_block() {
    echo "Removing Kiwi hosts block from /etc/hosts (backup: .bak)"
    if [ $(id -u) -eq 0 ]; then
        sed -i.bak '/# Kiwi Infrastructure Services/,/# End Kiwi Services/d' /etc/hosts || true
    else
        if command -v sudo >/dev/null 2>&1; then
            sudo sed -i.bak '/# Kiwi Infrastructure Services/,/# End Kiwi Services/d' /etc/hosts || true
        else
            echo "⚠ Skipped editing /etc/hosts (no root/sudo)"
        fi
    fi
}

remove_shortcuts_and_dirs() {
    # Remove shortcuts created by set_up.sh
    for link in \
        "$SCRIPT_HOME/easy-deploy" \
        "$SCRIPT_HOME/easy-stop" \
        "$SCRIPT_HOME/easy-deploy-ui" \
        "$SCRIPT_HOME/easy-ui-initial" \
        "$SCRIPT_HOME/easy-check" \
        "$SCRIPT_HOME/easy-clean-setup" \
        "$SCRIPT_HOME/easy-setup"; do
        [ -L "$link" ] || [ -f "$link" ] && rm -f "$link" || true
    done

    # Remove docker data directory created by setup
    if [ -d "$SCRIPT_HOME/docker" ]; then
        echo "Removing directory: $SCRIPT_HOME/docker"
        rm -rf "$SCRIPT_HOME/docker"
    fi

    # Remove the cloned repo directory (contains this script path)
    if [ -d "$SCRIPT_HOME/microservice-kiwi" ]; then
        echo "Removing directory: $SCRIPT_HOME/microservice-kiwi"
        rm -rf "$SCRIPT_HOME/microservice-kiwi"
    fi
}

remove_maven_cache_and_config() {
    local m2dir="$SCRIPT_HOME/.m2"
    if [ -d "$m2dir" ]; then
        echo "Removing Maven directory: $m2dir (settings.xml, repository, caches)"
        rm -rf "$m2dir"
    else
        echo "Maven directory not found at $m2dir (skipped)"
    fi
}

remove_setup_files_all_locations() {
    for f in "$HOME_PROGRESS_FILE" "$HOME_CONFIG_FILE" "$HOME_LOG_FILE" \
             "$LEGACY_PROGRESS_FILE" "$LEGACY_CONFIG_FILE" "$LEGACY_LOG_FILE"; do
        [ -f "$f" ] && rm -f "$f" && echo "Removed: $f" || true
    done
}

remove_docker_everything() {
    if [ -z "$DOCKER_CMD" ]; then
        echo "Docker not available, skipping Docker deep clean"
        return 0
    fi

    echo "Stopping all containers..."
    containers=$($DOCKER_CMD ps -aq 2>/dev/null || true)
    if [ -n "$containers" ]; then
        for id in $containers; do $DOCKER_CMD stop "$id" >/dev/null 2>&1 || true; done
    fi

    echo "Removing all containers..."
    containers_all=$($DOCKER_CMD ps -aq 2>/dev/null || true)
    if [ -n "$containers_all" ]; then
        for id in $containers_all; do $DOCKER_CMD rm -f "$id" >/dev/null 2>&1 || true; done
    fi

    echo "Removing all images..."
    images=$($DOCKER_CMD images -aq 2>/dev/null || true)
    if [ -n "$images" ]; then
        for img in $images; do $DOCKER_CMD rmi -f "$img" >/dev/null 2>&1 || true; done
    fi

    echo "Removing all volumes..."
    volumes=$($DOCKER_CMD volume ls -q 2>/dev/null || true)
    if [ -n "$volumes" ]; then
        for v in $volumes; do $DOCKER_CMD volume rm -f "$v" >/dev/null 2>&1 || true; done
    fi

    echo "Removing custom networks..."
    nets=$($DOCKER_CMD network ls --filter type=custom -q 2>/dev/null || true)
    if [ -n "$nets" ]; then
        for n in $nets; do $DOCKER_CMD network rm "$n" >/dev/null 2>&1 || true; done
    fi

    echo "Docker system prune..."
    $DOCKER_CMD system prune -a -f --volumes >/dev/null 2>&1 || true

    # Remove named volumes used in setup explicitly (best-effort)
    for nv in es_config es_data; do $DOCKER_CMD volume rm -f "$nv" >/dev/null 2>&1 || true; done

    # Remove standalone docker-compose binary if installed by setup
    if [ -f "/usr/local/bin/docker-compose" ]; then
        if [ $(id -u) -eq 0 ]; then
            rm -f /usr/local/bin/docker-compose || true
        else
            command -v sudo >/dev/null 2>&1 && sudo rm -f /usr/local/bin/docker-compose || true
        fi
        echo "Removed /usr/local/bin/docker-compose"
    fi
}

# Deep clean orchestration
deep_clean_everything() {
    echo
    echo "This will perform a DEEP CLEAN of all resources created by set_up.sh:"
    echo "- Docker: containers, images, volumes, networks, system prune"
    echo "- Maven: remove ~/.m2 (settings.xml, repository, caches)"
    echo "- Repo and data: ~/microservice-kiwi and ~/docker directories"
    echo "- Shortcuts: easy-* links in home"
    echo "- Environment: remove exports from ~/.bashrc and ~/.zshrc"
    echo "- Hosts: remove Kiwi block from /etc/hosts"
    echo "- Setup files: progress/config/log in home and legacy locations"
    echo
    if ! confirm_action "DEEP CLEAN EVERYTHING listed above"; then
        return 0
    fi

    # Backups (optional but recommended)
    backup_files || true

    # Execute deep clean steps
    remove_docker_everything
    remove_maven_cache_and_config
    remove_shell_exports
    remove_hosts_block
    remove_shortcuts_and_dirs
    remove_setup_files_all_locations

    echo
    echo "✓ Deep clean completed. You may want to restart your shell session."
}

# Main menu loop
while true; do
    show_menu
    read -p "Select option (1-7): " choice

    case $choice in
        1)
            if confirm_action "remove the progress file (setup will restart from beginning)"; then
                clean_progress
            fi
            ;;
        2)
            if confirm_action "remove the config file (you'll need to re-enter all passwords)"; then
                clean_config
            fi
            ;;
        3)
            if confirm_action "remove BOTH files (complete reset)"; then
                clean_both
            fi
            ;;
        4)
            show_status
            ;;
        5)
            backup_files
            ;;
        6)
            deep_clean_everything
            ;;
        7)
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo "Invalid option. Please select 1-7."
            ;;
    esac

    echo
    read -p "Press Enter to continue..."
done
