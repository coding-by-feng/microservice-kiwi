FROM openjdk:8-jdk-alpine
RUN mkdir /logs
RUN mkdir /root/wordTmp
ADD kiwi-word-biz-1.0-SNAPSHOT.jar app.jar
EXPOSE 5001
ENTRYPOINT java -jar -Xmx256m -Dspring.profiles.active=prod /app.jar > /logs/word.log