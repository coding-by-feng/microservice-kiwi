FROM eclipse-temurin:8-jdk

RUN mkdir /logs
ADD kiwi-tools-biz-2.0.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "-Xmx700m", "-Duser.timezone=GMT+12", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/tools-biz.log", "/app.jar"]