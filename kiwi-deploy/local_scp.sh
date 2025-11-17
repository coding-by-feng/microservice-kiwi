#!/bin/bash

# Environment selection (default test)
ENVIRONMENT="test"

# Command variables (common sources)
SOURCE_PATH_1="/Users/zhanshifeng/Documents/myDocument/idea-project/importance-back-up/kiwi-db.sql"
SOURCE_PATH_2="/Users/zhanshifeng/Documents/myDocument/webstorm-projects/kiwi-ui-dev/dist.zip"
SOURCE_PATH_3="/Users/zhanshifeng/Documents/myDocument/webstorm-projects/kiwi-ui-dev/dist"

# Remote defaults (test env)
REMOTE_USER_TEST="kason"
REMOTE_HOST_TEST="kason-pi.local"

# Production remote
REMOTE_USER_PROD="ubuntu"
REMOTE_HOST_PROD="ec2-3-102-115-54.ap-southeast-6.compute.amazonaws.com"
PEM_KEY="$HOME/Kason.pem"

# SCP options (array)
SCP_OPTS=()

configure_env() {
  if [ "$ENVIRONMENT" = "prod" ]; then
    REMOTE_USER="$REMOTE_USER_PROD"
    REMOTE_HOST="$REMOTE_HOST_PROD"
    if [ ! -f "$PEM_KEY" ]; then
      echo "[ERROR] Expected PEM key not found at $PEM_KEY" >&2
      return 1
    fi
    chmod 600 "$PEM_KEY" 2>/dev/null || true
    # Use array to preserve quoting for -i path
    SCP_OPTS=( -i "$PEM_KEY" -o StrictHostKeyChecking=no )
  else
    REMOTE_USER="$REMOTE_USER_TEST"
    REMOTE_HOST="$REMOTE_HOST_TEST"
    SCP_OPTS=( -o PreferredAuthentications=password -o PubkeyAuthentication=no )
  fi
}

show_env_menu() {
  echo "Select environment:";
  echo "e) Toggle environment (current: $ENVIRONMENT)";
  echo "t) Force test";
  echo "p) Force prod";
}

# Function to display menu
show_menu() {
    echo "Select what to copy (env: $ENVIRONMENT => $REMOTE_USER@$REMOTE_HOST):"
    echo "1) SQL file (kiwi-db.sql)"
    echo "2) Existing dist.zip file"
    echo "3) Zip dist directory and copy as dist.zip"
    echo "4) Exit"
    echo "e) Change environment (test/prod)"
}

# Function to copy SQL file
copy_sql() {
    echo "Copying SQL file to $REMOTE_USER@$REMOTE_HOST ..."
    scp "${SCP_OPTS[@]}" "$SOURCE_PATH_1" "$REMOTE_USER@$REMOTE_HOST":~
    if [ $? -eq 0 ]; then
        echo "SQL file copied successfully!"
    else
        echo "Failed to copy SQL file."
    fi
}

# Function to copy dist.zip (with zipping if needed)
copy_dist_zip() {
    if [ ! -f "$SOURCE_PATH_2" ]; then
        echo "dist.zip not found. Creating from dist directory..."
        if [ -d "$SOURCE_PATH_3" ]; then
            (cd "$(dirname "$SOURCE_PATH_3")" && zip -r dist.zip dist/)
            echo "dist.zip created successfully!"
        else
            echo "Error: dist directory not found at $SOURCE_PATH_3"
            return 1
        fi
    fi

    echo "Copying dist.zip to $REMOTE_USER@$REMOTE_HOST ..."
    scp "${SCP_OPTS[@]}" "$SOURCE_PATH_2" "$REMOTE_USER@$REMOTE_HOST":~
    if [ $? -eq 0 ]; then
        echo "dist.zip copied successfully!"
    else
        echo "Failed to copy dist.zip."
    fi
}

# Function to zip dist directory and copy as dist.zip
zip_and_copy_dist() {
    if [ ! -d "$SOURCE_PATH_3" ]; then
        echo "Error: dist directory not found at $SOURCE_PATH_3"
        return 1
    fi
    echo "Preparing to zip dist directory..."
    if [ -f "$SOURCE_PATH_2" ]; then
        echo "Deleting old dist.zip..."
        rm "$SOURCE_PATH_2"
    fi
    echo "Zipping dist directory to dist.zip..."
    (cd "$(dirname "$SOURCE_PATH_3")" && zip -r dist.zip dist/)
    if [ $? -eq 0 ]; then
        echo "dist.zip created successfully!"
        echo "Copying dist.zip to $REMOTE_USER@$REMOTE_HOST ..."
        scp "${SCP_OPTS[@]}" "$SOURCE_PATH_2" "$REMOTE_USER@$REMOTE_HOST":~
        if [ $? -eq 0 ]; then
            echo "dist.zip copied successfully!"
        else
            echo "Failed to copy dist.zip."
        fi
    else
        echo "Failed to create dist.zip."
    fi
}

change_environment() {
  echo "--- Environment Selection ---"
  echo "Current: $ENVIRONMENT"
  echo "1) test"
  echo "2) prod"
  read -p "Choose (1/2): " env_choice
  case $env_choice in
    1) ENVIRONMENT="test" ;;
    2) ENVIRONMENT="prod" ;;
    *) echo "Invalid choice, staying in $ENVIRONMENT" ;;
  esac
  configure_env || echo "Environment configuration failed; check key path for prod."
}

# Initial configuration
configure_env || true

while true; do
    show_menu
    read -p "Enter your choice (1-4 or e): " choice
    case $choice in
        1) copy_sql ;;
        2) copy_dist_zip ;;
        3) zip_and_copy_dist ;;
        4) echo "Exiting..."; exit 0 ;;
        e|E) change_environment ;;
        t|T) ENVIRONMENT="test"; configure_env; echo "Switched to test." ;;
        p|P) ENVIRONMENT="prod"; configure_env; echo "Switched to prod." ;;
        *) echo "Invalid option. Please select 1-4 or e." ;;
    esac
    echo ""  # blank line
done

