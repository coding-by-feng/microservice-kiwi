FROM openjdk:8-jdk-alpine
ENV KIWI_ENC_PASSWORD=coding-by-feng
ENV DB_IP=172.17.0.2
RUN mkdir /logs
ADD kiwi-auth-1.0.jar app.jar
EXPOSE 3001
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+08 -Dspring.profiles.active=prod /app.jar > /logs/auth.log