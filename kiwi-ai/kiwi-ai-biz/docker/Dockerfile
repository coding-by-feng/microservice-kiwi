FROM blackbelt/oraclejdk8

RUN mkdir /logs
RUN mkdir /ai-tmp

# Install Python and dependencies using yum
RUN yum install -y epel-release && \
    yum install -y python3 python3-pip ffmpeg && \
    ln -s /usr/bin/python3 /usr/bin/python && \
    yum clean all

# Install yt-dlp
RUN pip3 install --no-cache-dir yt-dlp

# Verify installations
RUN python --version && \
    yt-dlp --version && \
    java -version

ADD kiwi-ai-biz-2.0.jar app.jar

ENTRYPOINT java -jar -Xmx1024m -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-ai-biz -Dspring.profiles.active=prod /app.jar > /logs/ai-biz.log