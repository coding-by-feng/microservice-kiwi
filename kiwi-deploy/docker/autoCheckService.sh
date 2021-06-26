#!/bin/bash

runningCode='200'
while true
do
#        code=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://kiwi-microservice:9991/wordBiz/word/main/query/test)
#        if [ $code != $runningCode ]
#        then
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                echo $time Word-Biz Service is stopping, code = $code
#                docker container start `docker ps -a| grep docker_kiwi-word-biz-01_1 | awk '{print $1}' `
#                docker container start `docker ps -a| grep docker_kiwi-word-biz-02_1 | awk '{print $1}' `
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                echo $time Word-Biz Service has run just now, sleep
#                sleep 80s
#        else
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                echo $time Word-Biz Service is running, code = $code
#                sleep 15s
#        fi

        crawlerCode=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://kiwi-crawler:6001/actuator/info)
        if [ $crawlerCode != $runningCode ]
        then
                time=$(date "+%Y-%m-%d %H:%M:%S")
                docker container start `docker ps -a| grep docker_kiwi-crawler_1 | awk '{print $1}' `
                echo $time Word-Crawler Service has run just now, sleep
                sleep 60s
        else
                time=$(date "+%Y-%m-%d %H:%M:%S")
                echo $time Word-Crawler Service is running, code = $code
                sleep 15s
        fi

#        gateCode=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://kiwi-gate:9001/actuator/info)
#        if [ $gateCode != $runningCode ]
#        then
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                docker container start `docker ps -a| grep docker_kiwi-gate_1 | awk '{print $1}' `
#                echo $time Word-Gate Service has run just now, sleep
#                sleep 60s
#        else
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                echo $time Word-Gate Service is running, code = $code
#                sleep 15s
#        fi
#
#        authCode=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://kiwi-auth:3001/actuator/info)
#        if [ $authCode != $runningCode ]
#        then
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                docker container start `docker ps -a| grep docker_kiwi-auth_1 | awk '{print $1}' `
#                echo $time Word-Auth Service has run just now, sleep
#                sleep 60s
#        else
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                echo $time Word-Auth Service is running, code = $code
#                sleep 15s
#        fi
#
#        upmsCode=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://kiwi-upms:4001/actuator/info)
#        if [ $upmsCode != $runningCode ]
#        then
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                docker container start `docker ps -a| grep docker_kiwi-upms_1 | awk '{print $1}' `
#                echo $time Word-Upms Service has run just now, sleep
#                sleep 60s
#        else
#                time=$(date "+%Y-%m-%d %H:%M:%S")
#                echo $time Word-Upms Service is running, code = $code
#                sleep 15s
#        fi
done