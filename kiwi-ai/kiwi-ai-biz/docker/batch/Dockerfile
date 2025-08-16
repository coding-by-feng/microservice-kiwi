# Use Eclipse Temurin JDK 8 (Ubuntu/Debian based)
FROM eclipse-temurin:8-jdk

# Set working directory
WORKDIR /app

# Create necessary directories and install minimal dependencies for yt-dlp
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

# Install yt-dlp from PyPI (matches container arch)
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir yt-dlp

# Copy the JAR file (single copy, consistent path)
COPY kiwi-ai-biz-2.0.jar /app/app.jar

# Set the entry point (fix jar path ordering)
ENTRYPOINT ["java", "-Xmx600m", "-Duser.timezone=GMT+12", "-Dspring.application.name=kiwi-ai-batch", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/ai-batch.log", "-jar", "/app/app.jar"]
