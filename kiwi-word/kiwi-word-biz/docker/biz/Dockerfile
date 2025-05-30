FROM eclipse-temurin:8-jdk

RUN mkdir /logs
RUN mkdir /wordTmp
ADD kiwi-word-biz-2.0.jar app.jar
COPY gcp-credentials.json /root/gcp-credentials.json


ENTRYPOINT ["java", "-jar", "-Xmx1024m", "-Duser.timezone=GMT+12", "-Dspring.application.name=kiwi-word-biz", "-Dspring.profiles.active=prod", "-Dcom.sun.management.autodiscovery=true", "-Dlogging.file.name=/logs/word-biz.log", "/app.jar"]