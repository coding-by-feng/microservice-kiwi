FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-auth-2.0.jar app.jar
EXPOSE 3001
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+12 -Dspring.profiles.active=prod /app.jar