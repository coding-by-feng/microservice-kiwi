FROM eclipse-temurin:8-jre
RUN mkdir -p /logs
RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
       fonts-dejavu \
       curl \
    && rm -rf /var/lib/apt/lists/*
COPY kiwi-gateway-2.0.jar /app.jar
EXPOSE 9991
ENTRYPOINT ["java", "-jar", "-Xmx512m", "-Duser.timezone=GMT+12", "-Djava.awt.headless=true", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/gate.log", "/app.jar"]
