FROM eclipse-temurin:8-jdk

RUN mkdir /logs
RUN mkdir /wordTmp
# Install curl for readiness checks and procps for pgrep healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl procps \
    && rm -rf /var/lib/apt/lists/*
ADD kason-word-biz-2.0.jar app.jar


ENTRYPOINT ["java", "-jar", "-Xmx1024m", "-Duser.timezone=GMT+12", "-Dspring.application.name=kason-word-biz", "-Dspring.profiles.active=prod", "-Dcom.sun.management.autodiscovery=true", "-Dlogging.file.name=/logs/word-biz.log", "/app.jar"]