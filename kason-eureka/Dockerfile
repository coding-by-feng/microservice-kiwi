FROM eclipse-temurin:8-jdk
RUN mkdir /logs
ADD kason-eureka-2.0.jar app.jar
EXPOSE 8762
ENTRYPOINT ["java", "-jar", "-Xmx512m", "-Duser.timezone=GMT+12", "/app.jar"]