# Kiwi Monolith Dockerfile
# Build: docker build -t kiwi-monolith .
# Run: docker run -p 8088:8088 --env-file .env kiwi-monolith

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY ../pom.xml ./pom.xml
COPY ../kiwi-common ./kiwi-common
COPY ../kiwi-monolith ./kiwi-monolith

# Build the application
RUN ./mvnw clean package -pl kiwi-monolith -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S kiwi && adduser -S kiwi -G kiwi
USER kiwi

# Copy JAR from builder
COPY --from=builder /app/kiwi-monolith/target/*.jar app.jar

# Default environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    TZ=Pacific/Auckland \
    JVM_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JVM_OPTS -jar app.jar"]
