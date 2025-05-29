FROM arm64v8/openjdk:8-jdk

RUN mkdir /logs \
    && apt-get update && apt-get upgrade -y \
    && apt-get install -y fonts-dejavu-core

ADD kiwi-gateway-2.0.jar app.jar
EXPOSE 9991
ENTRYPOINT java -jar -Xmx512m -Duser.timezone=GMT+12 -Djava.awt.headless=true -Dspring.profiles.active=prod /app.jar > /logs/gate.log
