FROM eclipse-temurin:8-jdk

RUN mkdir /logs
# Install curl for healthchecks and readiness scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
ADD kiwi-upms-biz-2.0.jar app.jar
EXPOSE 4001
ENTRYPOINT ["java", "-jar", "-Xmx400m", "-Duser.timezone=GMT+12", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/upms-biz.log", "/app.jar"]