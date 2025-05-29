FROM arm64v8/openjdk:8-jdk
RUN mkdir /logs
ADD kiwi-upms-biz-2.0.jar app.jar
EXPOSE 4001
ENTRYPOINT ["java", "-jar", "-Xmx400m", "-Duser.timezone=GMT+12", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/upms-biz.log", "/app.jar"]