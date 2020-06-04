FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-gateway-1.0-SNAPSHOT.jar app.jar
EXPOSE 9991
ENTRYPOINT java -jar -Xmx128m /app.jar > /logs/gate.log