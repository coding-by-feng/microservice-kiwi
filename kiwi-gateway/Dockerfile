FROM openjdk:8-jdk-alpine
ENV KIWI_ENC_PASSWORD=coding-by-feng
RUN mkdir /logs
RUN echo "http://mirrors.aliyun.com/alpine/v3.8/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/v3.8/community" >> /etc/apk/repositories \
    && apk update upgrade \
    && apk --no-cache add ttf-dejavu fontconfig
ADD kiwi-gateway-1.0.jar app.jar
EXPOSE 9991
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+08 -Dspring.profiles.active=prod /app.jar > /logs/gate.log
# ENTRYPOINT java -jar -Xmx128m -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=9008,suspend=n -Dspring.profiles.active=prod /app.jar > /logs/gate.log