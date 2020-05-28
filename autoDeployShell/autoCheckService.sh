#!/bin/bash

runningCode='200'
while true
do
        code=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://39.107.142.173:9991/wordBiz/word/main/query/test)
        if [ $code != $runningCode ]
        then
                time=$(date "+%Y-%m-%d %H:%M:%S")
                echo $time Word-Biz Service is stopping, code = $code
                docker container start `docker ps -a| grep microservice-kiwi_kiwi-word_1 | awk '{print $1}' `
                time=$(date "+%Y-%m-%d %H:%M:%S")
                echo $time Word-Biz Service has run just now, sleep
                sleep 80s
        else
                time=$(date "+%Y-%m-%d %H:%M:%S")
                echo $time Word-Biz Service is running, code = $code
                sleep 15s
        fi

        crawlerCode=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://172.31.182.58:6001/actuator/info)
        if [ $crawlerCode != $runningCode ]
        then
                time=$(date "+%Y-%m-%d %H:%M:%S")
                docker container start `docker ps -a| grep microservice-kiwi_kiwi-crawler_1 | awk '{print $1}' `
                echo $time Word-Crawler Service has run just now, sleep
                sleep 60s
        else
                time=$(date "+%Y-%m-%d %H:%M:%S")
                echo $time Word-Crawler Service is running, code = $code
                sleep 15s
        fi
done