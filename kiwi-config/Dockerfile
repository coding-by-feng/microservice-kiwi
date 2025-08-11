FROM eclipse-temurin:8-jdk
RUN mkdir /logs
ADD kiwi-config-2.0.jar app.jar
EXPOSE 7771
ENTRYPOINT ["java", "-jar", "-Xmx128m", "-Duser.timezone=GMT+12", "/app.jar"]