FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ENV KIWI_ENC_PASSWORD=$KIWI_ENC_PASSWORD
ADD kiwi-auth-1.0.jar app.jar
EXPOSE 3001
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+12 -Dspring.profiles.active=prod /app.jar > /logs/auth.log