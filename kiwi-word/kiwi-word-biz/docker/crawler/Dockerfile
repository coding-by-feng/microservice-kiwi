FROM openjdk:8-jdk-alpine
#FROM blackbelt/oraclejdk8
ENV KIWI_ENC_PASSWORD=coding-by-feng
ENV DB_IP=kiwi-db

# RUN apk add --no-cache tini
# Tini is now available at /sbin/tini
# ENTRYPOINT ["/sbin/tini", "--"]

RUN mkdir /logs
RUN mkdir /wordTmp
ADD kiwi-word-biz-1.0.jar app.jar
# EXPOSE 5001
# EXPOSE 5005
# ENTRYPOINT java -jar -Xmx256m -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005,suspend=n /app.jar > /logs/word.log
# ENTRYPOINT java -jar -Xmx512m -Dspring.application.name=kiwi-word-biz-crawler -Dspring.profiles.active=prod -Dserver.port=5002 -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005,suspend=n /app.jar > /logs/word-crawler.log
ENTRYPOINT java -jar -Xmx512m -Xmn200m -Duser.timezone=GMT+08 -Dspring.application.name=kiwi-word-biz-crawler -Dspring.profiles.active=prod -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005,suspend=n /app.jar > /logs/word-crawler.log