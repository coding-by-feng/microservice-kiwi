FROM openjdk:8-jdk-alpine
ENV KIWI_ENC_PASSWORD=coding-by-feng
RUN mkdir /logs
ADD kiwi-eureka-1.0.jar app.jar
EXPOSE 8762
ENTRYPOINT java -jar -Xmx512m -Duser.timezone=GMT+08 /app.jar > /logs/eureka.log