FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ENV KIWI_ENC_PASSWORD=$KIWI_ENC_PASSWORD
ADD kiwi-upms-biz-1.0.jar app.jar
EXPOSE 4001
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+12 -Dspring.profiles.active=prod /app.jar > /logs/upms.log