FROM openjdk:8-jdk-alpine
RUN mkdir /logs
ADD kiwi-eureka-1.0.jar app.jar
EXPOSE 8762
ENTRYPOINT java -jar -Xmx512m /app.jar > /logs/eureka.log