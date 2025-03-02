FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-eureka-2.0.jar app.jar
EXPOSE 8762
ENTRYPOINT java -jar -Xmx512m -Duser.timezone=GMT+12 /app.jar > /logs/eureka.log