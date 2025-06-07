FROM eclipse-temurin:8-jdk

RUN mkdir /logs
RUN mkdir /wordTmp
ADD kiwi-word-biz-2.0.jar app.jar

ENTRYPOINT ["java", "-jar", "-Xmx1000m", "-Xmn500m", "-Duser.timezone=GMT+12", "-Dspring.application.name=kiwi-word-biz-crawler", "-Dspring.profiles.active=prod", "-Dcom.sun.management.autodiscovery=true", "-Dlogging.file.name=/logs/word-crawler.log", "/app.jar"]