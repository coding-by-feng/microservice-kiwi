#!/bin/bash

# Maven and Docker Cleanup Script for Rock 4C+ Ubuntu
# This script cleans Maven local repository and Docker cache to free up disk space

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to get disk usage
get_disk_usage() {
    df -h / | awk 'NR==2 {print $3 " used, " $4 " available"}'
}

# Function to get directory size
get_dir_size() {
    if [ -d "$1" ]; then
        du -sh "$1" 2>/dev/null | cut -f1
    else
        echo "N/A"
    fi
}

echo "========================================"
echo "  Maven & Docker Cleanup Script"
echo "  Rock 4C+ Ubuntu System"
echo "========================================"
echo ""

print_status "Current disk usage: $(get_disk_usage)"
echo ""

# Check if running as root for Docker operations
if [ "$EUID" -ne 0 ]; then
    print_warning "Script not running as root. Docker cleanup will require sudo."
    SUDO_CMD="sudo"
else
    SUDO_CMD=""
fi

# Maven cleanup section
print_status "Starting Maven cleanup..."

# Default Maven local repository location
MAVEN_REPO="$HOME/.m2/repository"

# Check if Maven repo exists
if [ -d "$MAVEN_REPO" ]; then
    MAVEN_SIZE_BEFORE=$(get_dir_size "$MAVEN_REPO")
    print_status "Maven repository found at: $MAVEN_REPO"
    print_status "Current size: $MAVEN_SIZE_BEFORE"

    # Option 1: Clean specific cache directories (safer)
    print_status "Cleaning Maven temporary and cache files..."

    # Remove temporary files
    find "$MAVEN_REPO" -name "*.tmp" -type f -delete 2>/dev/null || true
    find "$MAVEN_REPO" -name "*.part" -type f -delete 2>/dev/null || true
    find "$MAVEN_REPO" -name "*.lock" -type f -delete 2>/dev/null || true

    # Remove resolver status files
    find "$MAVEN_REPO" -name "_remote.repositories" -type f -delete 2>/dev/null || true
    find "$MAVEN_REPO" -name "resolver-status.properties" -type f -delete 2>/dev/null || true

    # Remove empty directories
    find "$MAVEN_REPO" -type d -empty -delete 2>/dev/null || true

    MAVEN_SIZE_AFTER=$(get_dir_size "$MAVEN_REPO")
    print_success "Maven cleanup completed. New size: $MAVEN_SIZE_AFTER"

    # Uncomment the following lines if you want to completely remove the Maven repository
    # WARNING: This will require re-downloading all dependencies
    # read -p "Do you want to completely remove Maven repository? (y/N): " -n 1 -r
    # echo
    # if [[ $REPLY =~ ^[Yy]$ ]]; then
    #     rm -rf "$MAVEN_REPO"
    #     print_success "Maven repository completely removed"
    # fi
else
    print_warning "Maven repository not found at $MAVEN_REPO"
fi

echo ""

# Docker cleanup section
print_status "Starting Docker cleanup..."

# Check if Docker is installed
if command -v docker &> /dev/null; then
    print_status "Docker found. Checking Docker service status..."

    # Check if Docker service is running
    if $SUDO_CMD systemctl is-active --quiet docker; then
        print_status "Docker service is running. Proceeding with cleanup..."

        # Get Docker disk usage before cleanup
        print_status "Docker disk usage before cleanup:"
        $SUDO_CMD docker system df || true
        echo ""

        # Stop all running containers (optional - uncomment if needed)
        # print_status "Stopping all running containers..."
        # $SUDO_CMD docker stop $($SUDO_CMD docker ps -q) 2>/dev/null || true

        # Remove stopped containers
        print_status "Removing stopped containers..."
        $SUDO_CMD docker container prune -f

        # Remove unused images
        print_status "Removing unused images..."
        $SUDO_CMD docker image prune -a -f

        # Remove unused volumes
        print_status "Removing unused volumes..."
        $SUDO_CMD docker volume prune -f

        # Remove unused networks
        print_status "Removing unused networks..."
        $SUDO_CMD docker network prune -f

        # Remove build cache
        print_status "Removing build cache..."
        $SUDO_CMD docker builder prune -a -f

        # Complete system cleanup (most aggressive)
        print_status "Performing complete Docker system cleanup..."
        $SUDO_CMD docker system prune -a -f --volumes

        echo ""
        print_status "Docker disk usage after cleanup:"
        $SUDO_CMD docker system df || true

        print_success "Docker cleanup completed"

    else
        print_warning "Docker service is not running. Starting Docker service..."
        $SUDO_CMD systemctl start docker
        if $SUDO_CMD systemctl is-active --quiet docker; then
            print_success "Docker service started successfully"
            # Repeat cleanup commands here if needed
        else
            print_error "Failed to start Docker service"
        fi
    fi
else
    print_warning "Docker is not installed on this system"
fi

echo ""

# Additional system cleanup (optional)
print_status "Performing additional system cleanup..."

# Clean package cache
print_status "Cleaning apt package cache..."
$SUDO_CMD apt-get clean
$SUDO_CMD apt-get autoremove -y
$SUDO_CMD apt-get autoclean

# Clean logs (optional - uncomment if needed)
# print_status "Cleaning old log files..."
# $SUDO_CMD journalctl --vacuum-time=7d

# Clean temporary files
print_status "Cleaning temporary files..."
$SUDO_CMD rm -rf /tmp/* 2>/dev/null || true
rm -rf ~/.cache/* 2>/dev/null || true

print_success "Additional system cleanup completed"

echo ""
print_status "Final disk usage: $(get_disk_usage)"
echo ""
print_success "Cleanup script completed successfully!"

# Optional: Show largest directories to help identify other cleanup opportunities
echo ""
print_status "Top 10 largest directories in home folder:"
du -h "$HOME" 2>/dev/null | sort -hr | head -10 || true

echo ""
echo "========================================"
echo "  Cleanup Summary"
echo "========================================"
echo "✓ Maven temporary files cleaned"
echo "✓ Docker containers, images, volumes, networks pruned"
echo "✓ Docker build cache cleared"
echo "✓ System package cache cleaned"
echo "✓ Temporary files removed"
echo ""
echo "Note: To completely reset Maven, uncomment the"
echo "repository removal section in the script."