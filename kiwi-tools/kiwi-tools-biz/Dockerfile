FROM eclipse-temurin:8-jdk

RUN mkdir /logs
# Install curl for healthchecks and readiness scripts, and procps for pgrep
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl procps \
    && rm -rf /var/lib/apt/lists/*
ADD kiwi-tools-biz-2.0.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "-Xmx700m", "-Duser.timezone=GMT+12", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/tools-biz.log", "/app.jar"]