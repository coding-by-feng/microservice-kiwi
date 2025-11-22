# Use Eclipse Temurin JDK 8 (Ubuntu/Debian based)
FROM eclipse-temurin:8-jdk

WORKDIR /app

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

ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m venv $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

RUN pip3 install --upgrade pip && pip3 install yt-dlp

COPY kiwi-ai-biz-2.0.jar /app/app.jar

ENV JAVA_OPTS="-Xms384m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-ai-batch -Dspring.profiles.active=prod -Dlogging.file.name=/logs/ai-batch.log"

EXPOSE 18016

ENTRYPOINT ["bash", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
