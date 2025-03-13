FROM openjdk:8-jdk-alpine

RUN mkdir /logs
RUN mkdir /crawlerTmp

ADD kiwi-word-crawler-2.0.jar app.jar
COPY gcp-credentials.json /root/gcp-credentials.json

ENTRYPOINT java -jar -Xmx800m -Duser.timezone=GMT+12 -Xmn300m -Dspring.profiles.active=prod -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006,suspend=n /app.jar > /logs/crawler.log