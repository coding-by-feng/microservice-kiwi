#!/bin/bash

# Script to update nginx configuration in kiwi-ui container
# This adds the proxy configuration for microservices without SSL

read -p "Enter the microservice host (e.g., kiwi-microservice): " MICROSERVICE_HOST

if [ -z "$MICROSERVICE_HOST" ]; then
    echo "✗ No host provided. Exiting."
    exit 1
fi

echo "Updating nginx configuration in kiwi-ui container..."
echo "Using microservice host: $MICROSERVICE_HOST"

# Backup current configuration
docker exec kiwi-ui cp /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# Install vi editor in the container if not present
docker exec kiwi-ui apt-get update
docker exec kiwi-ui apt-get install -y vim

# Create the new nginx configuration
cat <<EOF | docker exec -i kiwi-ui tee /etc/nginx/nginx.conf > /dev/null
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;  # Changed from debug to warn for better performance
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

    # Global gzip settings
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_comp_level 6;
    gzip_types
        text/plain
        text/css
        text/xml
        text/javascript
        application/javascript
        application/x-javascript
        application/json
        application/xml
        application/rss+xml
        application/atom+xml
        font/truetype
        font/opentype
        application/vnd.ms-fontobject
        image/svg+xml;

    server {
        listen 80;
        server_name localhost kason-pi kason-pi.local;
        root /usr/share/nginx/html;
        index index.html;

        # Security headers
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-XSS-Protection "1; mode=block" always;

        # Optimize static asset caching (JS, CSS, Images)
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
            add_header Vary Accept-Encoding;

            # Try file, then fallback to index.html for SPA
            try_files $uri $uri/ @fallback;
        }

        # Font handling with proper CORS headers
        location ~* \.(woff|woff2|ttf|eot)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
            add_header Access-Control-Allow-Origin "*";
            add_header Access-Control-Allow-Methods "GET, OPTIONS";
            add_header Access-Control-Allow-Headers "Range";

            # Try file, then fallback to index.html for SPA
            try_files $uri $uri/ @fallback;
        }

        # Special handling for external assets
        location /assets/external/ {
            expires 1y;
            add_header Cache-Control "public, immutable";
            add_header Access-Control-Allow-Origin "*";

            try_files $uri $uri/ @fallback;
        }

        # Handle WebSocket connections for AI features
        location /ai-biz {
            proxy_pass http://192.168.1.120:9991;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_read_timeout 3600s;
            proxy_send_timeout 3600s;
        }

        # API proxy configurations
        location /auth {
            proxy_pass http://192.168.1.120:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /wordBiz {
            proxy_pass http://192.168.1.120:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /code {
            proxy_pass http://192.168.1.120:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /admin {
            proxy_pass http://192.168.1.120:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # SPA fallback - this is the KEY fix for your 404 issues
        location @fallback {
            rewrite ^.*$ /index.html last;
        }

        # Main location block for SPA routing
        location / {
            try_files $uri $uri/ @fallback;

            # Cache HTML files for a short time
            location ~* \.html$ {
                expires 1h;
                add_header Cache-Control "public, must-revalidate";
            }
        }

        # Error pages
        error_page 404 /index.html;
        error_page 500 502 503 504 /index.html;
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
    echo "The following proxy routes are now configured with host: $MICROSERVICE_HOST"
    echo "  - /auth      -> http://$MICROSERVICE_HOST:9991"
    echo "  - /wordBiz   -> http://$MICROSERVICE_HOST:9991"
    echo "  - /code      -> http://$MICROSERVICE_HOST:9991"
    echo "  - /admin     -> http://$MICROSERVICE_HOST:9991"
    echo ""
    echo "You can access the application at: http://localhost:80"
    echo "Or at: http://\$(hostname -I | awk '{print \$1}'):80"
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