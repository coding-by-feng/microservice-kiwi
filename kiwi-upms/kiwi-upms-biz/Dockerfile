FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-upms-biz-1.0.jar app.jar
EXPOSE 4001
ENTRYPOINT java -jar -Xmx256m -Dspring.profiles.active=prod /app.jar > /logs/upms.log