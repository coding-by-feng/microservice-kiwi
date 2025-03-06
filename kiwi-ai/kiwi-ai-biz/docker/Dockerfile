FROM blackbelt/oraclejdk8

RUN mkdir /logs
RUN mkdir /ai-tmp
ADD kiwi-ai-biz-2.0.jar app.jar

ENTRYPOINT java -jar -Xmx1024m -Duser.timezone=GMT+12 -Dspring.application.name=kiwi-ai-biz -Dspring.profiles.active=prod /app.jar > /logs/ai-biz.log