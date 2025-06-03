# Command 1 variables
SOURCE_PATH_1="~/Documents/myDocument/idea-project/importance-back-up/kiwi-db.sql"
REMOTE_USER="kason"
REMOTE_HOST="kason-server.local"

# Command 2 variables
SOURCE_PATH_2="/Users/zhanshifeng/Documents/myDocument/webstorm-projects/kiwi-ui-dev/dist.zip"

# Then use them in your scp commands
scp -r "$SOURCE_PATH_1" "$REMOTE_USER@$REMOTE_HOST":~/docker/mysql
scp -r "$SOURCE_PATH_2" "$REMOTE_USER@$REMOTE_HOST":~