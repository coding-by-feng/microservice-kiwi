#FROM openjdk:8-jdk-alpine
FROM blackbelt/oraclejdk8
RUN mkdir /logs
RUN mkdir /crawlerTmp

#RUN sed -i s@/archive.ubuntu.com/@/mirrors.aliyun.com/@g /etc/apt/sources.list
#RUN sed -i 's/deb.debian.org/mirrors.ustc.edu.cn/g' /etc/apt/sources.list
#RUN apt-get clean
#RUN apt-get update
#RUN apt-get -q update && apt-get -qy install netcat

ADD kiwi-word-crawler-1.0-SNAPSHOT.jar app.jar
#EXPOSE 6001
# ENTRYPOINT java -jar -Xmx256m -Dspring.profiles.active=prod -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006,suspend=n /app.jar > /logs/crawler.log
ENTRYPOINT java -jar -Xmx512m -Dspring.profiles.active=prod -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006,suspend=n /app.jar > /logs/crawler.log
# ENTRYPOINT java -jar -Xmx256m -Dcom.sun.management.jmxremote.port=9211 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Dspring.profiles.active=prod -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5006,suspend=n /app.jar > /logs/crawler.log