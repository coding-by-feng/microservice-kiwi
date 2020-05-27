#!/bin/bash

killall autoCheckService.sh

cd /root/microservice-kiwi/

git reset --hard master
git pull https://github.com/coding-by-feng/microservice-kiwi.git/

mvn clean install -Dmaven.test.skip=true

mv -f kiwi-eureka/Dockerfile /root/docker/kiwi/eureka/
mv -f kiwi-config/Dockerfile /root/docker/kiwi/config/
mv -f kiwi-upms/kiwi-upms-biz/Dockerfile /root/docker/kiwi/upms/
mv -f kiwi-word/kiwi-word-biz/Dockerfile /root/docker/kiwi/word/
mv -f kiwi-word/kiwi-word-crawler/Dockerfile /root/docker/kiwi/crawler/
mv -f kiwi-auth/Dockerfile /root/docker/kiwi/auth/
mv -f kiwi-gateway/Dockerfile /root/docker/kiwi/gate/

mv -f kiwi-eureka/target/kiwi-eureka-1.0-SNAPSHOT.jar /root/docker/kiwi/eureka/
mv -f kiwi-config/target/kiwi-config-1.0-SNAPSHOT.jar /root/docker/kiwi/config/
mv -f kiwi-upms/kiwi-upms-biz/target/kiwi-upms-biz-1.0-SNAPSHOT.jar /root/docker/kiwi/upms/
mv -f kiwi-auth/target/kiwi-auth-1.0-SNAPSHOT.jar /root/docker/kiwi/auth/
mv -f kiwi-gateway/target/kiwi-gateway-1.0-SNAPSHOT.jar /root/docker/kiwi/gate/
mv -f kiwi-word/kiwi-word-biz/target/kiwi-word-biz-1.0-SNAPSHOT.jar /root/docker/kiwi/word/
mv -f kiwi-word/kiwi-word-crawler/target/kiwi-word-crawler-1.0-SNAPSHOT.jar /root/docker/kiwi/crawler/

/root/docker/kiwi/autoDeploy.sh

./autoCheckService.sh