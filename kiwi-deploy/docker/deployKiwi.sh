#!/bin/bash

#
#
#   Copyright [2019~2025] [codingByFeng]
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#
#

cd ~/microservice-kiwi/ || exit

echo "git pulling..."

git stash
git pull

chmod 777 ~/microservice-kiwi/kiwi-deploy/docker/*.sh

# Replace rm -rf with find -delete to handle large numbers of files
find ~/docker/kiwi/eureka/logs/ -type f -delete
find ~/docker/kiwi/config/logs/ -type f -delete
find ~/docker/kiwi/upms/logs/ -type f -delete
find ~/docker/kiwi/auth/logs/ -type f -delete
find ~/docker/kiwi/gate/logs/ -type f -delete
find ~/docker/kiwi/word/logs/ -type f -delete
find ~/docker/kiwi/word/crawlerTmp/ -type f -delete
find ~/docker/kiwi/word/bizTmp/ -type f -delete
find ~/docker/kiwi/crawler/logs/ -type f -delete

# Check if -skip-mvn parameter is provided, if not, run mvn clean install
if [ "$1" != "-skip-mvn" ]
then
  mvn clean install -Dmaven.test.skip=true -B
fi

mv -f ~/microservice-kiwi/kiwi-eureka/Dockerfile ~/docker/kiwi/eureka/
mv -f ~/microservice-kiwi/kiwi-config/Dockerfile ~/docker/kiwi/config/
mv -f ~/microservice-kiwi/kiwi-upms/kiwi-upms-biz/Dockerfile ~/docker/kiwi/upms/
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/biz/Dockerfile ~/docker/kiwi/word/biz
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-biz/docker/crawler/Dockerfile ~/docker/kiwi/word/crawler
mv -f ~/microservice-kiwi/kiwi-word/kiwi-word-crawler/Dockerfile ~/docker/kiwi/crawler/
mv -f ~/microservice-kiwi/kiwi-auth/Dockerfile ~/docker/kiwi/auth/
mv -f ~/microservice-kiwi/kiwi-gateway/Dockerfile ~/docker/kiwi/gate/

mv -f ~/.m2/repository/me/fengorz/kiwi-eureka/2.0/kiwi-eureka-2.0.jar ~/docker/kiwi/eureka/
mv -f ~/.m2/repository/me/fengorz/kiwi-config/2.0/kiwi-config-2.0.jar ~/docker/kiwi/config/
mv -f ~/.m2/repository/me/fengorz/kiwi-upms-biz/2.0/kiwi-upms-biz-2.0.jar ~/docker/kiwi/upms/
mv -f ~/.m2/repository/me/fengorz/kiwi-auth/2.0/kiwi-auth-2.0.jar ~/docker/kiwi/auth/
mv -f ~/.m2/repository/me/fengorz/kiwi-gateway/2.0/kiwi-gateway-2.0.jar ~/docker/kiwi/gate/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-biz/2.0/kiwi-word-biz-2.0.jar ~/docker/kiwi/word/
mv -f ~/.m2/repository/me/fengorz/kiwi-word-crawler/2.0/kiwi-word-crawler-2.0.jar ~/docker/kiwi/crawler/

~/microservice-kiwi/kiwi-deploy/docker/autoDeploy.sh $1

echo sleep 200
sleep 200s

nohup ~/microservice-kiwi/kiwi-deploy/docker/autoCheckService.sh  >~/autoCheck.log 2>&1 &