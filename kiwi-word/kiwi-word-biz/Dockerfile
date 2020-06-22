FROM openjdk:8-jdk-alpine
RUN mkdir /logs
RUN mkdir /root/wordTmp
ADD kiwi-word-biz-1.0-SNAPSHOT.jar app.jar
EXPOSE 5001
EXPOSE 5005
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-jar", "-Xmx256m", "/app.jar", "> /logs/word.log"]
#ENTRYPOINT java -jar -Xmx256m -Dspring.profiles.active=prod -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 /app.jar > /logs/word.log