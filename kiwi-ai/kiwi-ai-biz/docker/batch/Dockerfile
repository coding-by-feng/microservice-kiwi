# Use Eclipse Temurin JDK 8 (Ubuntu/Debian based)
FROM eclipse-temurin:8-jdk

# Set working directory
WORKDIR /app

# Create necessary directories and install dependencies including yt-dlp
RUN mkdir -p /logs /ai-tmp \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        python3-venv \
        ffmpeg \
        curl \
        wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create and activate virtual environment
ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

# Install yt-dlp via pip3 and upgrade pip
RUN pip3 install --upgrade pip \
    && pip3 install yt-dlp

# Copy the JAR file
COPY kiwi-ai-biz-2.0.jar /app/app.jar

# Set the entry point
ENTRYPOINT ["java", "-jar", "-Xmx600m", "-Duser.timezone=GMT+12", "-Dspring.application.name=kiwi-ai-batch", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/ai-batch.log", "/app/app.jar"]
