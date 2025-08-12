#!/bin/bash

# Script to update nginx configuration in kiwi-ui container
# This adds the proxy configuration for microservices without SSL

echo "Updating nginx configuration in kiwi-ui container..."

# First, let's backup the current configuration
docker exec kiwi-ui cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# Install vi editor in the container if not present
docker exec kiwi-ui apt-get update
docker exec kiwi-ui apt-get install -y vim

# Create the new nginx configuration
docker exec kiwi-ui tee /etc/nginx/nginx.conf > /dev/null << 'EOF'
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log debug;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;
    sendfile on;
    keepalive_timeout 65;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
    gzip_min_length 256;

    server {
        listen 80;
        server_name localhost kason-pi kason-pi.local;

        # Serve static files
        location / {
            root /usr/share/nginx/html;
            index index.html index.htm;
        }

        # Proxy configuration for auth service
        location /auth {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Proxy configuration for word business service
        location /wordBiz {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Proxy configuration for code service
        location /code {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Proxy configuration for admin service
        location /admin {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /ai-biz {
            proxy_pass http://kiwi-microservice:9991;
	    proxy_http_version 1.1;
	    proxy_set_header Upgrade $http_upgrade;
	    proxy_set_header Connection "upgrade";
	    proxy_set_header Host $host;
	    proxy_set_header X-Real-IP $remote_addr;
	    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
	    proxy_set_header X-Forwarded-Proto $scheme;
        }

    }
}
EOF

echo "Testing nginx configuration..."
docker exec kiwi-ui nginx -t

if [ $? -eq 0 ]; then
    echo "✓ Nginx configuration test passed"
    echo "Restarting nginx service in container..."
    docker exec kiwi-ui nginx -s reload

    echo "Restarting kiwi-ui container to ensure all changes take effect..."
    docker container restart kiwi-ui

    echo "✓ Nginx configuration updated successfully!"
    echo ""
    echo "The following proxy routes are now configured:"
    echo "  - /auth      -> http://kiwi-microservice:9991"
    echo "  - /wordBiz   -> http://kiwi-microservice:9991"
    echo "  - /code      -> http://kiwi-microservice:9991"
    echo "  - /admin     -> http://kiwi-microservice:9991"
    echo "  - /health    -> Health check endpoint"
    echo ""
    echo "You can access the application at: http://localhost:80"
    echo "Or at: http://$(hostname -I | awk '{print $1}'):80"
else
    echo "✗ Nginx configuration test failed"
    echo "Restoring backup configuration..."
    docker exec kiwi-ui cp /etc/nginx/nginx.conf.backup /etc/nginx/nginx.conf
    docker exec kiwi-ui nginx -s reload
    echo "Backup configuration restored"
    exit 1
fi

echo ""
echo "To check nginx status and logs:"
echo "  docker exec kiwi-ui nginx -t                    # Test configuration"
echo "  docker exec kiwi-ui nginx -s reload             # Reload configuration"
echo "  docker logs kiwi-ui                             # View container logs"
echo "  docker exec -it kiwi-ui tail -f /var/log/nginx/access.log  # View access logs"
echo "  docker exec -it kiwi-ui tail -f /var/log/nginx/error.log   # View error logs"