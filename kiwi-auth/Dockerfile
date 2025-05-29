FROM arm64v8/openjdk:8-jdk

# Check user (should be root)
RUN whoami

# Create logs directory with full permissions
RUN mkdir -m 777 /logs && ls -ld /logs

# Copy the JAR file
ADD kiwi-auth-2.0.jar app.jar

# Expose the port
EXPOSE 3001

# Start the application and log output to /logs/auth.log
ENTRYPOINT ["java", "-jar", "-Xmx256m", "-Duser.timezone=GMT+12", "-Dspring.profiles.active=prod", "-Dlogging.file.name=/logs/auth.log", "/app.jar"]