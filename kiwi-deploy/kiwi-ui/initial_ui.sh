#!/bin/bash

# Script to update nginx configuration in kiwi-ui container
# Generates the new nginx.conf (SPA + caching + proxy + websockets) with dynamic backend

set -euo pipefail

# --- Input: dynamic backend ---
read -r -p "Enter the microservice host [127.0.0.1]: " MICROSERVICE_HOST
MICROSERVICE_HOST=${MICROSERVICE_HOST:-127.0.0.1}
read -r -p "Enter the microservice port [9991]: " MICROSERVICE_PORT
MICROSERVICE_PORT=${MICROSERVICE_PORT:-9991}

KIWI_CONTAINER_NAME=${KIWI_CONTAINER_NAME:-kiwi-ui}

if ! docker ps --format '{{.Names}}' | grep -q "^${KIWI_CONTAINER_NAME}$"; then
  echo "✗ Container '${KIWI_CONTAINER_NAME}' not found or not running."
  echo "  Tip: docker ps --format 'table {{.Names}}\t{{.Status}}'"
  exit 1
fi

echo "Updating nginx configuration in container: ${KIWI_CONTAINER_NAME}"
BACKEND_URL="http://${MICROSERVICE_HOST}:${MICROSERVICE_PORT}"
echo "Using backend -> ${BACKEND_URL}"

# Backup current configuration (best-effort)
docker exec "${KIWI_CONTAINER_NAME}" sh -c 'cp -f /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup 2>/dev/null || true'

# Write the new nginx configuration using a quoted heredoc to preserve $nginx_vars
cat <<'EOF' | docker exec -i "${KIWI_CONTAINER_NAME}" tee /etc/nginx/nginx.conf >/dev/null
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
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

    # Fix for proxy headers hash optimization warning
    proxy_headers_hash_max_size 1024;
    proxy_headers_hash_bucket_size 128;

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
            proxy_pass @BACKEND@;
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
            proxy_pass @BACKEND@;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /wordBiz {
            proxy_pass @BACKEND@;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /code {
            proxy_pass @BACKEND@;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /admin {
            proxy_pass @BACKEND@;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /tools {
            proxy_pass @BACKEND@;
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

# Replace placeholder with actual backend URL (inside the container)
docker exec "${KIWI_CONTAINER_NAME}" sh -c \
  "sed -i 's|@BACKEND@|${BACKEND_URL//|/\\|}|g' /etc/nginx/nginx.conf"

# Validate and reload
set +e
docker exec "${KIWI_CONTAINER_NAME}" nginx -t
NGINX_TEST_RC=$?
set -e

if [ $NGINX_TEST_RC -eq 0 ]; then
  echo "✓ Nginx configuration test passed"
  echo "Reloading nginx and restarting container to apply changes..."
  docker exec "${KIWI_CONTAINER_NAME}" nginx -s reload || true
  docker container restart "${KIWI_CONTAINER_NAME}" >/dev/null
  echo "✓ Nginx configuration updated successfully!"
  echo "Proxy routes now point to: ${BACKEND_URL}"
  echo "  - /ai-biz (WebSocket-enabled)"
  echo "  - /auth"
  echo "  - /wordBiz"
  echo "  - /code"
  echo "  - /admin"
  echo "  - /tools"
else
  echo "✗ Nginx configuration test failed"
  echo "Restoring backup configuration..."
  docker exec "${KIWI_CONTAINER_NAME}" sh -c 'cp -f /etc/nginx/nginx.conf.backup /etc/nginx/nginx.conf 2>/dev/null || true'
  docker exec "${KIWI_CONTAINER_NAME}" nginx -s reload || true
  echo "Backup configuration restored"
  exit 1
fi

cat <<HINT

To check nginx status and logs:
  docker exec ${KIWI_CONTAINER_NAME} nginx -t                    # Test configuration
  docker exec ${KIWI_CONTAINER_NAME} nginx -s reload             # Reload configuration
  docker logs ${KIWI_CONTAINER_NAME}                             # View container logs
  docker exec -it ${KIWI_CONTAINER_NAME} tail -f /var/log/nginx/access.log  # Access logs
  docker exec -it ${KIWI_CONTAINER_NAME} tail -f /var/log/nginx/error.log   # Error logs

HINT
