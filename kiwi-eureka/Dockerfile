FROM openjdk:8-jdk-alpine
RUN RUN mkdir -p /logs && chown 1000:1000 /logs && chmod 775 /logs
ADD kiwi-eureka-2.0.jar app.jar
EXPOSE 8762
