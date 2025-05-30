FROM eclipse-temurin:8-jdk

USER root

# Create necessary directories and install dependencies
RUN mkdir /logs /ai-tmp \
    && apt-get update && apt-get upgrade -y \
    && apt-get install -y git bash curl build-essential libffi-dev libssl-dev \
                         libbz2-dev zlib1g-dev liblzma-dev libreadline-dev \
                         libsqlite3-dev tk-dev libncurses5-dev libncursesw5-dev \
                         xz-utils ca-certificates ffmpeg

# Install Pyenv
ENV PYENV_ROOT="/root/.pyenv"
ENV PATH="$PYENV_ROOT/bin:$PATH"

RUN curl -fsSL https://pyenv.run | bash \
    && echo 'export PYENV_ROOT="/root/.pyenv"' >> /root/.bashrc \
    && echo 'export PATH="$PYENV_ROOT/bin:$PATH"' >> /root/.bashrc \
    && echo 'eval "$(pyenv init --path)"' >> /root/.bashrc \
    && echo 'eval "$(pyenv init -)"' >> /root/.bashrc

# Install Python via Pyenv
RUN /bin/bash -c "source /root/.bashrc && pyenv install -v 3.9.21"
RUN pyenv global 3.9

# Install yt-dlp
ADD yt-dlp_linux yt-dlp_linux

RUN chmod 777 yt-dlp_linux

# Add application JAR file
ADD kiwi-ai-biz-2.0.jar app.jar
COPY gcp-credentials.json /root/gcp-credentials.json

# Set the entry point
ENTRYPOINT ["java", "-jar", "-Xmx512m", "-Duser.timezone=GMT+12", "-Dspring.application.name=kiwi-ai-biz", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/ai-biz.log", "/app.jar"]