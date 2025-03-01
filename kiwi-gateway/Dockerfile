FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ENV KIWI_ENC_PASSWORD=$KIWI_ENC_PASSWORD
ADD kiwi-gateway-1.0.jar app.jar
EXPOSE 9991
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+12 -Dspring.profiles.active=prod /app.jar > /logs/gate.log
# ENTRYPOINT java -jar -Xmx128m -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9008,suspend=n -Dspring.profiles.active=prod /app.jar > /logs/gate.log