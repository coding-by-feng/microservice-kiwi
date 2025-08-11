FROM eclipse-temurin:8-jdk

RUN mkdir /logs
RUN mkdir /crawlerTmp

ADD kiwi-word-crawler-2.0.jar app.jar

ENTRYPOINT ["java", "-jar", "-Xmx2000m", "-Duser.timezone=GMT+12", "-Xmn300m", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/crawler.log", "/app.jar"]