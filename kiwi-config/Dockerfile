FROM openjdk:8-jdk-alpine
RUN mkdir /logs

ADD kiwi-config-1.0-SNAPSHOT.jar app.jar
EXPOSE 7771
ENTRYPOINT java -jar -Xmx64m /app.jar > /logs/config.log