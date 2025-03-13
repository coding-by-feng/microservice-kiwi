FROM openjdk:8-jdk-alpine
RUN mkdir /logs
RUN echo "http://mirrors.aliyun.com/alpine/v3.8/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/v3.8/community" >> /etc/apk/repositories \
    && apk update upgrade \
    && apk --no-cache add ttf-dejavu fontconfig
ADD kiwi-gateway-2.0.jar app.jar
EXPOSE 9991
ENTRYPOINT java -jar -Xmx256m -Duser.timezone=GMT+12 -Dspring.profiles.active=prod /app.jar