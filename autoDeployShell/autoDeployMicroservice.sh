#!/bin/bash

killall autoCheckService.sh

cd /root/microservice-kiwi/

rm -rf ./*

git reset --hard master
git pull https://github.com/coding-by-feng/microservice-kiwi.git/

cd kiwi-word
mvn clean install -Dmaven.test.skip=true
cd ..
cd kiwi-eureka/
mvn clean install -Dmaven.test.skip=true
cd ..
cd kiwi-config/
mvn clean install -Dmaven.test.skip=true
cd ..
cd kiwi-gateway/
mvn clean install -Dmaven.test.skip=true
cd ..
cd kiwi-auth/
mvn clean install -Dmaven.test.skip=true
cd ..
cd kiwi-upms/
mvn clean install -Dmaven.test.skip=true

cd /root/microservice-kiwi/

mv -f kiwi-eureka/Dockerfile /root/docker/kiwi/eureka/
mv -f kiwi-config/Dockerfile /root/docker/kiwi/config/
mv -f kiwi-upms/kiwi-upms-biz/Dockerfile /root/docker/kiwi/upms/
mv -f kiwi-word/kiwi-word-biz/Dockerfile /root/docker/kiwi/word/
mv -f kiwi-word/kiwi-word-crawler/Dockerfile /root/docker/kiwi/crawler/
mv -f kiwi-auth/Dockerfile /root/docker/kiwi/auth/
mv -f kiwi-gateway/Dockerfile /root/docker/kiwi/gate/

mv -f ~/.m2/repository/me/fengorz/kiwi-eureka/1.0-SNAPSHOT/kiwi-eureka-1.0-SNAPSHOT.jar /root/docker/kiwi/eureka/
mv -f ~/.m2/repository/me/fengorz/kiwi-config/1.0-SNAPSHOT/kiwi-config-1.0-SNAPSHOT.jar /root/docker/kiwi/config/
mv -f ~/.m2/repository/me/fengorz/kiwi-upms-biz/1.0-SNAPSHOT/kiwi-upms-biz-1.0-SNAPSHOT.jar /root/docker/kiwi/upms/
mv -f ~/.m2/repository/me/fengorz/kiwi-auth/1.0-SNAPSHOT/kiwi-auth-1.0-SNAPSHOT.jar /root/docker/kiwi/auth/
mv -f ~/.m2/repository/me/fengorz/kiwi-gateway/1.0-SNAPSHOT/kiwi-gateway-1.0-SNAPSHOT.jar /root/docker/kiwi/gate/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-biz/1.0-SNAPSHOT/kiwi-word-biz-1.0-SNAPSHOT.jar /root/docker/kiwi/word/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-crawler/1.0-SNAPSHOT/kiwi-word-crawler-1.0-SNAPSHOT.jar /root/docker/kiwi/crawler/

/root/docker/kiwi/autoDeploy.sh

nohup /root/autoCheckService.sh  >~/autoCheck.log 2>&1 &