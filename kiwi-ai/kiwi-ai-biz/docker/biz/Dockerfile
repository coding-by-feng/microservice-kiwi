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
        procps \
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

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV TZ=Pacific/Auckland

# Create volume mount points
VOLUME ["/logs", "/ai-tmp"]

# Expose port
EXPOSE 18015

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]