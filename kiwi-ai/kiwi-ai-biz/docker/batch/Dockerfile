FROM blackbelt/oraclejdk8

USER root

# Create necessary directories and install dependencies
RUN mkdir /logs /ai-tmp \
    && apk update && apk upgrade \
    && apk add --no-cache git bash curl build-base libffi-dev openssl-dev \
                      bzip2-dev zlib-dev xz-dev readline-dev sqlite-dev tk-dev

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

# Set the entry point
ENTRYPOINT java -jar -Xmx1024m -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-ai-biz -Dspring.profiles.active=batch /app.jar > /logs/ai-batch.log