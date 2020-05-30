#!/bin/bash

killall autoCheckService.sh

cd /root/microservice-kiwi/

git reset --hard master
git pull https://github.com/coding-by-feng/microservice-kiwi.git/

mvn clean install -Dmaven.test.skip=true

mv -f /root/microservice-kiwi/kiwi-eureka/Dockerfile /root/docker/kiwi/eureka/
mv -f /root/microservice-kiwi/kiwi-config/Dockerfile /root/docker/kiwi/config/
mv -f /root/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile /root/docker/kiwi/upms/
mv -f /root/microservice-kiwi/kiwi-word/kiwi-word-biz/Dockerfile /root/docker/kiwi/word/
mv -f /root/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile /root/docker/kiwi/crawler/
mv -f /root/microservice-kiwi/kiwi-auth/Dockerfile /root/docker/kiwi/auth/
mv -f /root/microservice-kiwi/kiwi-gateway/Dockerfile /root/docker/kiwi/gate/

mv -f /root/microservice-kiwi/kiwi-eureka/target/kiwi-eureka-1.0-SNAPSHOT.jar /root/docker/kiwi/eureka/
mv -f /root/microservice-kiwi/kiwi-config/target/kiwi-config-1.0-SNAPSHOT.jar /root/docker/kiwi/config/
mv -f /root/microservice-kiwi/kiwi-upms/kiwi-upms-biz/target/kiwi-upms-biz-1.0-SNAPSHOT.jar /root/docker/kiwi/upms/
mv -f /root/microservice-kiwi/kiwi-auth/target/kiwi-auth-1.0-SNAPSHOT.jar /root/docker/kiwi/auth/
mv -f /root/microservice-kiwi/kiwi-gateway/target/kiwi-gateway-1.0-SNAPSHOT.jar /root/docker/kiwi/gate/
mv -f /root/microservice-kiwi/kiwi-word/kiwi-word-biz/target/kiwi-word-biz-1.0-SNAPSHOT.jar /root/docker/kiwi/word/
mv -f /root/microservice-kiwi/kiwi-word/kiwi-word-crawler/target/kiwi-word-crawler-1.0-SNAPSHOT.jar /root/docker/kiwi/crawler/

/root/docker/kiwi/autoDeploy.sh

nohup /root/autoCheckService.sh  >~/autoCheck.log 2>&1 &