#FROM openjdk:8-jdk-alpine
FROM blackbelt/oraclejdk8

# RUN apk add --no-cache tini
# Tini is now available at /sbin/tini
# ENTRYPOINT ["/sbin/tini", "--"]

RUN mkdir /logs
RUN mkdir /wordTmp
ADD kiwi-word-biz-2.0.jar app.jar
COPY gcp-credentials.json /root/gcp-credentials.json

# EXPOSE 5001
# ENTRYPOINT java -jar -Xmx256m -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005,suspend=n /app.jar > /logs/word.log
# ENTRYPOINT java -jar -Xmx512m -Dspring.application.name=kiwi-word-biz -Dspring.profiles.active=prod -Dserver.port=5001 -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder /app.jar > /logs/word-biz.log
ENTRYPOINT java -jar -Xmx1024m -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-word-biz -Dspring.profiles.active=prod -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5004,suspend=n /app.jar > /logs/word-biz.log