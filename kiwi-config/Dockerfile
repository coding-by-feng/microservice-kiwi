FROM openjdk:8-jdk-alpine
ENV KIWI_ENC_PASSWORD=coding-by-feng
RUN mkdir /logs
ADD kiwi-config-1.0.jar app.jar
EXPOSE 7771
ENTRYPOINT java -jar -Xmx128m -Duser.timezone=GMT+08 /app.jar > /logs/config.log