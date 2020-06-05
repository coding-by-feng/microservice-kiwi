FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-auth-1.0-SNAPSHOT.jar app.jar
EXPOSE 3001
ENTRYPOINT java -jar -Xmx128m -Dspring.profiles.active=prod /app.jar > /logs/auth.log