#!/bin/bash

# Command variables
SOURCE_PATH_1="/Users/zhanshifeng/Documents/myDocument/idea-project/importance-back-up/kiwi-db.sql"
SOURCE_PATH_2="/Users/zhanshifeng/Documents/myDocument/webstorm-projects/kiwi-ui-dev/dist.zip"
SOURCE_PATH_3="/Users/zhanshifeng/Documents/myDocument/webstorm-projects/kiwi-ui-dev/dist"
REMOTE_USER="kason"
REMOTE_HOST="kason-server.local"

# Function to display menu
show_menu() {
    echo "Select what to copy:"
    echo "1) SQL file (kiwi-db.sql)"
    echo "2) Existing dist.zip file"
    echo "3) Zip dist directory and copy as dist.zip"
    echo "4) Exit"
}

# Function to copy SQL file
copy_sql() {
    echo "Copying SQL file..."
    scp -r "$SOURCE_PATH_1" "$REMOTE_USER@$REMOTE_HOST":~
    if [ $? -eq 0 ]; then
        echo "SQL file copied successfully!"
    else
        echo "Failed to copy SQL file."
    fi
}

# Function to copy dist.zip (with zipping if needed)
copy_dist_zip() {
    # Check if dist.zip exists, if not create it from dist directory
    if [ ! -f "$SOURCE_PATH_2" ]; then
        echo "dist.zip not found. Creating from dist directory..."
        if [ -d "$SOURCE_PATH_3" ]; then
            cd "$(dirname "$SOURCE_PATH_3")"
            zip -r dist.zip dist/
            echo "dist.zip created successfully!"
        else
            echo "Error: dist directory not found at $SOURCE_PATH_3"
            return 1
        fi
    fi

    echo "Copying dist.zip..."
    scp -r "$SOURCE_PATH_2" "$REMOTE_USER@$REMOTE_HOST":~
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

    # Delete old dist.zip if it exists
    if [ -f "$SOURCE_PATH_2" ]; then
        echo "Deleting old dist.zip..."
        rm "$SOURCE_PATH_2"
    fi

    # Create new dist.zip from dist directory
    echo "Zipping dist directory to dist.zip..."
    cd "$(dirname "$SOURCE_PATH_3")"
    zip -r dist.zip dist/

    if [ $? -eq 0 ]; then
        echo "dist.zip created successfully!"
        echo "Copying dist.zip..."
        scp -r "$SOURCE_PATH_2" "$REMOTE_USER@$REMOTE_HOST":~
        if [ $? -eq 0 ]; then
            echo "dist.zip copied successfully!"
        else
            echo "Failed to copy dist.zip."
        fi
    else
        echo "Failed to create dist.zip."
    fi
}



# Main menu loop
while true; do
    show_menu
    read -p "Enter your choice (1-4): " choice

    case $choice in
        1)
            copy_sql
            ;;
        2)
            copy_dist_zip
            ;;
        3)
            zip_and_copy_dist
            ;;
        4)
            echo "Exiting..."
            exit 0
            ;;
        *)
            echo "Invalid option. Please select 1-4."
            ;;
    esac

    echo ""  # Add blank line for readability
done