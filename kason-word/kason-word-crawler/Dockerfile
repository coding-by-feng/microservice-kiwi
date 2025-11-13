FROM eclipse-temurin:8-jdk

RUN mkdir /logs
RUN mkdir /crawlerTmp
# Install curl for healthchecks and readiness scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ADD kason-word-crawler-2.0.jar app.jar

ENTRYPOINT ["java", "-jar", "-Xmx2000m", "-Duser.timezone=GMT+12", "-Xmn300m", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/crawler.log", "/app.jar"]