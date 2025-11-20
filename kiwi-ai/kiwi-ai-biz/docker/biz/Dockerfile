# Use Eclipse Temurin JDK 8 (Ubuntu/Debian based)
FROM eclipse-temurin:8-jdk

WORKDIR /app

# Simplified dependencies (remove cron & tzdata since no scheduled updates needed)
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

# Virtual environment
ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Install / upgrade yt-dlp once at build time
RUN pip3 install --upgrade pip && pip3 install --upgrade yt-dlp

# Add cron job to upgrade yt-dlp daily at midnight (UTC / container TZ)
RUN echo "PATH=/usr/sbin:/usr/bin:/bin:/opt/venv/bin" > /etc/cron.d/yt-dlp-upgrade \
 && echo "0 0 * * * root /opt/venv/bin/pip install -U yt-dlp >> /var/log/yt-dlp-cron.log 2>&1" >> /etc/cron.d/yt-dlp-upgrade \
 && chmod 0644 /etc/cron.d/yt-dlp-upgrade \
 && touch /var/log/yt-dlp-cron.log

# Copy JAR
COPY kiwi-ai-biz-2.0.jar /app/app.jar

# Java options & timezone
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.application.name=kiwi-ai-biz -Dspring.profiles.active=prod -Dlogging.file.name=/logs/ai-biz.log" \
    TZ=Pacific/Auckland

VOLUME ["/logs", "/ai-tmp"]
EXPOSE 18015

# Direct Java entrypoint
ENTRYPOINT ["bash", "-c", "cron && java $JAVA_OPTS -jar /app/app.jar"]
