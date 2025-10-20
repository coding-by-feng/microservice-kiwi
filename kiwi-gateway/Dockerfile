FROM openjdk:8-jdk-alpine
RUN mkdir /logs
RUN apk update && apk upgrade
RUN apk add --no-cache ttf-dejavu
# Install curl for healthchecks and readiness scripts
RUN apk add --no-cache curl
ADD kiwi-gateway-2.0.jar app.jar
EXPOSE 9991
ENTRYPOINT ["java", "-jar", "-Xmx512m", "-Duser.timezone=GMT+12", "-Djava.awt.headless=true", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/gate.log", "/app.jar"]
