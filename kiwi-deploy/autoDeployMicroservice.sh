#!/bin/bash

killall autoCheckService.sh

cd ~/microservice-kiwi/

#rm -rf ./*

git reset --hard master
git pull git@github.com/coding-by-feng/microservice-kiwi.git/

mvn clean install -Dmaven.test.skip=true

#cd kiwi-word
#mvn clean install -Dmaven.test.skip=true
#cd ..
#cd kiwi-eureka/
#mvn clean install -Dmaven.test.skip=true
#cd ..
#cd kiwi-config/
#mvn clean install -Dmaven.test.skip=true
#cd ..
#cd kiwi-gateway/
#mvn clean install -Dmaven.test.skip=true
#cd ..
#cd kiwi-auth/
#mvn clean install -Dmaven.test.skip=true
#cd ..
#cd kiwi-upms/
#mvn clean install -Dmaven.test.skip=true

#cd ~/microservice-kiwi/

mv -f kiwi-eureka/Dockerfile ~/docker/kiwi/eureka/
mv -f kiwi-config/Dockerfile ~/docker/kiwi/config/
mv -f kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
mv -f kiwi-word/kiwi-word-biz/Dockerfile ~/docker/kiwi/word/
mv -f kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
mv -f kiwi-auth/Dockerfile ~/docker/kiwi/auth/
mv -f kiwi-gateway/Dockerfile ~/docker/kiwi/gate/

mv -f ~/.m2/repository/me/fengorz/kiwi-eureka/1.0-SNAPSHOT/kiwi-eureka-1.0-SNAPSHOT.jar ~/docker/kiwi/eureka/
mv -f ~/.m2/repository/me/fengorz/kiwi-config/1.0-SNAPSHOT/kiwi-config-1.0-SNAPSHOT.jar ~/docker/kiwi/config/
mv -f ~/.m2/repository/me/fengorz/kiwi-upms-biz/1.0-SNAPSHOT/kiwi-upms-biz-1.0-SNAPSHOT.jar ~/docker/kiwi/upms/
mv -f ~/.m2/repository/me/fengorz/kiwi-auth/1.0-SNAPSHOT/kiwi-auth-1.0-SNAPSHOT.jar ~/docker/kiwi/auth/
mv -f ~/.m2/repository/me/fengorz/kiwi-gateway/1.0-SNAPSHOT/kiwi-gateway-1.0-SNAPSHOT.jar ~/docker/kiwi/gate/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-biz/1.0-SNAPSHOT/kiwi-word-biz-1.0-SNAPSHOT.jar ~/docker/kiwi/word/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-crawler/1.0-SNAPSHOT/kiwi-word-crawler-1.0-SNAPSHOT.jar ~/docker/kiwi/crawler/

~/autoDeploy.sh

echo sleep 300
sleep 100s

nohup ~/autoCheckService.sh  >~/autoCheck.log 2>&1 &