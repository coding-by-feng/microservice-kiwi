FROM openjdk:8-jdk-alpine
ENV KIWI_ENC_PASSWORD=coding-by-feng
ENV DB_IP=kiwi-db
RUN mkdir /logs
ADD kiwi-upms-biz-1.0.jar app.jar
EXPOSE 4001
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+08 -Dspring.profiles.active=prod /app.jar > /logs/upms.log