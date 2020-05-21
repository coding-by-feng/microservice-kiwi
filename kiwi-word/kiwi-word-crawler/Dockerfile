FROM openjdk:8-jdk-alpine
RUN mkdir /logs
RUN mkdir /root/crawlerTmp
ADD kiwi-word-crawler-1.0-SNAPSHOT.jar app.jar
EXPOSE 6001
ENTRYPOINT java -jar -Xmx128m /app.jar > /logs/crawler.log