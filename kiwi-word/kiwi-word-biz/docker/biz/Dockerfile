FROM blackbelt/oraclejdk8

RUN mkdir /logs
RUN mkdir /wordTmp
ADD kiwi-word-biz-2.0.jar app.jar
COPY gcp-credentials.json /root/gcp-credentials.json

ENTRYPOINT java -jar -Xmx1024m -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-word-biz -Dspring.profiles.active=prod -Dcom.sun.management.autodiscovery=true -XX:+UnlockCommercialFeatures -XX:+FlightRecorder -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5004,suspend=n /app.jar > /logs/word-biz.log
