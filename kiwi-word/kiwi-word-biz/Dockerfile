FROM openjdk:8-jdk-alpine
RUN mkdir /root/crawlerTmp
ADD kiwi-word-biz-1.0-SNAPSHOT.jar app.jar
EXPOSE 5001
ENTRYPOINT java -jar /app.jar