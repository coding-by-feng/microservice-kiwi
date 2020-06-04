FROM openjdk:8-jdk-alpine
RUN mkdir /logs
RUN apk add --update ttf-dejavu fontconfig && rm -rf /var/cache/apk/*
ADD kiwi-gateway-1.0-SNAPSHOT.jar app.jar
EXPOSE 9991
ENTRYPOINT java -jar -Xmx128m /app.jar > /logs/gate.log