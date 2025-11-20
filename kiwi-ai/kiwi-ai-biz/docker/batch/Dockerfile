# Use Eclipse Temurin JDK 8 (Ubuntu/Debian based)
FROM eclipse-temurin:8-jdk

# Set working directory
WORKDIR /app

# Build arg for optional host-provided yt-dlp path
ARG HOST_YTDLP=yt-dlp_linux

# Create necessary directories and install dependencies including cron
RUN mkdir -p /logs /ai-tmp \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        python3-venv \
        ffmpeg \
        curl \
        wget \
        cron \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create and activate virtual environment
ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Copy host-provided yt-dlp binary if available and make it executable
COPY ${HOST_YTDLP} /usr/local/bin/yt-dlp
RUN chmod +x /usr/local/bin/yt-dlp || true

# Fallback: install yt-dlp via pip only if binary missing after copy
RUN if [ ! -f /usr/local/bin/yt-dlp ]; then \
      echo "Host yt-dlp not provided; installing via pip"; \
      pip3 install --upgrade pip && pip3 install yt-dlp; \
    else \
      echo "Using host-provided yt-dlp binary"; \
    fi

# Add auto-update and entrypoint scripts
COPY yt-dlp-auto-update.sh /usr/local/bin/yt-dlp-auto-update
COPY entrypoint-with-cron.sh /usr/local/bin/entrypoint-with-cron
RUN chmod +x /usr/local/bin/yt-dlp-auto-update /usr/local/bin/entrypoint-with-cron

# Copy the JAR file
COPY kiwi-ai-biz-2.0.jar /app/app.jar

# Set environment variables
ENV JAVA_OPTS="-Xms384m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-ai-batch -Dspring.profiles.active=prod -Dlogging.file.name=/logs/ai-batch.log" \
    TZ=Pacific/Auckland \
    YTDLP_BIN=/usr/local/bin/yt-dlp

# Expose port (if batch exposes one; keep if needed)
EXPOSE 18016

# Use cron-enabled entrypoint
ENTRYPOINT ["/usr/local/bin/entrypoint-with-cron"]
