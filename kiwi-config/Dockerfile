FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ENV KIWI_ENC_PASSWORD=$KIWI_ENC_PASSWORD
ADD kiwi-config-1.0.jar app.jar
EXPOSE 7771
ENTRYPOINT java -jar -Xmx128m -Duser.timezone=GMT+12 /app.jar > /logs/config.log