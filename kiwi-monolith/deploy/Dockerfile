# Kiwi Monolith Dockerfile
# Build context: kiwi-monolith/ (parent of deploy/)
# Build: docker compose build kiwi-app
# Run:   docker compose up -d

FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml first to cache dependency downloads
COPY pom.xml .
RUN mvn dependency:go-offline -q || true

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl python3 nodejs ffmpeg \
    && curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp \
    && ln -s /usr/local/bin/yt-dlp /usr/local/bin/yt-dlp-tor \
    && mkdir -p /data/ytb-data \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/*.jar app.jar

ENV TZ=Pacific/Auckland \
    JVM_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dspring.profiles.active=prod -Dserver.port=8080"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
