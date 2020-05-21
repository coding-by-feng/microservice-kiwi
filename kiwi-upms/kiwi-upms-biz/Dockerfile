FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-upms-biz-1.0-SNAPSHOT.jar app.jar
EXPOSE 4001
ENTRYPOINT java -jar -Xmx64m /app.jar > /logs/upms.log