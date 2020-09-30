FROM openjdk:8-jdk-alpine

# RUN apk add --no-cache tini
# Tini is now available at /sbin/tini
# ENTRYPOINT ["/sbin/tini", "--"]

RUN mkdir /logs
RUN mkdir /wordTmp
ADD kiwi-word-biz-1.0-SNAPSHOT.jar app.jar
EXPOSE 5001
EXPOSE 5005
ENTRYPOINT java -jar -Xmx256m -Dspring.profiles.active=prod -Dcom.sun.management.jmxremote.port=9210 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005,suspend=n /app.jar > /logs/word.log