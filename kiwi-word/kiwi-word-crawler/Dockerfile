FROM arm64v8/openjdk:8-jdk

RUN mkdir /logs
RUN mkdir /crawlerTmp

ADD kiwi-word-crawler-2.0.jar app.jar


ENTRYPOINT java -jar -Xmx800m -Duser.timezone=GMT+12 -Xmn300m -Dspring.profiles.active=prod /app.jar > /logs/crawler.log
