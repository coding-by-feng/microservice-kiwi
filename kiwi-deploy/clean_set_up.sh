#!/bin/bash

# Kiwi Microservice Cleanup Script for Raspberry Pi OS
# This script cleans up progress tracking and configuration files

set -e  # Exit on any error

# Configuration
PROGRESS_FILE="$(pwd)/.kiwi_setup_progress"
CONFIG_FILE="$(pwd)/.kiwi_setup_config"
SCRIPT_USER=${SUDO_USER:-$USER}
SCRIPT_HOME=$(eval echo ~$SCRIPT_USER)

echo "=================================="
echo "Kiwi Microservice Setup Cleanup"
echo "Running as: $(whoami)"
echo "Target user: $SCRIPT_USER"
echo "Target home: $SCRIPT_HOME"
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
    echo "6. Exit"
    echo
}

# Function to show current status
show_status() {
    echo
    echo "CURRENT STATUS:"
    echo "=============="

    echo "Progress file: $PROGRESS_FILE"
    if [ -f "$PROGRESS_FILE" ]; then
        echo "  ✓ EXISTS ($(wc -l < "$PROGRESS_FILE") completed steps)"
        echo "  Size: $(ls -lh "$PROGRESS_FILE" | awk '{print $5}')"
        echo "  Last modified: $(ls -l "$PROGRESS_FILE" | awk '{print $6, $7, $8}')"
    else
        echo "  ✗ DOES NOT EXIST"
    fi

    echo
    echo "Config file: $CONFIG_FILE"
    if [ -f "$CONFIG_FILE" ]; then
        echo "  ✓ EXISTS ($(wc -l < "$CONFIG_FILE") configuration entries)"
        echo "  Size: $(ls -lh "$CONFIG_FILE" | awk '{print $5}')"
        echo "  Last modified: $(ls -l "$CONFIG_FILE" | awk '{print $6, $7, $8}')"
    else
        echo "  ✗ DOES NOT EXIST"
    fi

    echo
    if [ -f "$PROGRESS_FILE" ]; then
        echo "COMPLETED STEPS:"
        echo "==============="
        cat "$PROGRESS_FILE" | sed 's/^/  ✓ /'
    fi

    echo
    if [ -f "$CONFIG_FILE" ]; then
        echo "SAVED CONFIGURATIONS (passwords hidden):"
        echo "======================================="
        while IFS='=' read -r key value; do
            if [[ "$key" =~ PASSWORD|API_KEY ]]; then
                echo "  $key=[HIDDEN]"
            else
                echo "  $key=$value"
            fi
        done < "$CONFIG_FILE"
    fi
}

# Function to backup files
backup_files() {
    local timestamp=$(date '+%Y%m%d_%H%M%S')
    local backup_dir="$(pwd)/kiwi_setup_backups"

    echo "Creating backup directory: $backup_dir"
    mkdir -p "$backup_dir"

    if [ -f "$PROGRESS_FILE" ]; then
        local progress_backup="$backup_dir/.kiwi_setup_progress_$timestamp"
        cp "$PROGRESS_FILE" "$progress_backup"
        echo "✓ Progress file backed up to: $progress_backup"
    else
        echo "⚠ No progress file to backup"
    fi

    if [ -f "$CONFIG_FILE" ]; then
        local config_backup="$backup_dir/.kiwi_setup_config_$timestamp"
        cp "$CONFIG_FILE" "$config_backup"
        chmod 600 "$config_backup"  # Maintain secure permissions
        echo "✓ Config file backed up to: $config_backup"
    else
        echo "⚠ No config file to backup"
    fi

    echo "✓ Backup completed in: $backup_dir"
}

# Function to clean progress file
clean_progress() {
    if [ -f "$PROGRESS_FILE" ]; then
        echo "Removing progress file: $PROGRESS_FILE"
        rm "$PROGRESS_FILE"
        echo "✓ Progress file removed successfully"
        echo "  Next setup run will start from the beginning"
    else
        echo "⚠ Progress file does not exist: $PROGRESS_FILE"
    fi
}

# Function to clean config file
clean_config() {
    if [ -f "$CONFIG_FILE" ]; then
        echo "Removing config file: $CONFIG_FILE"
        rm "$CONFIG_FILE"
        echo "✓ Config file removed successfully"
        echo "  Next setup run will prompt for all passwords and settings again"
    else
        echo "⚠ Config file does not exist: $CONFIG_FILE"
    fi
}

# Function to clean both files
clean_both() {
    echo "Removing both progress and config files..."
    clean_progress
    clean_config
    echo "✓ Full cleanup completed"
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

# Main menu loop
while true; do
    show_menu
    read -p "Select option (1-6): " choice

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
            echo "Goodbye!"
            exit 0
            ;;
        *)
            echo "Invalid option. Please select 1-6."
            ;;
    esac

    echo
    read -p "Press Enter to continue..."
done