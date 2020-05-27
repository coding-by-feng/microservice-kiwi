#!/bin/bash

runningCode='200'
while true
do
        code=$(curl -I -m 10 -o /dev/null -s -w %{http_code} http://39.107.142.173:9991/wordBiz/word/main/query/test)
        if [ $code != $runningCode ]
        then
                time=$(date "+%Y-%m-%d-%H:%M:%S")
                echo $time Word-Biz Service is stopping, code = $code
                docker container start `docker ps -a| grep microservice-kiwi_kiwi-word_1 | awk '{print $1}' `
                time=$(date "+%Y-%m-%d-%H:%M:%S")
                echo $time Word-Biz Service has run, sleep
                sleep 80s
                docker container start `docker ps -a| grep microservice-kiwi_kiwi-crawler_1 | awk '{print $1}' `
                time=$(date "+%Y-%m-%d-%H:%M:%S")
                echo $time Word-Crawler Service has run, sleep
                sleep 60s
        else
                time=$(date "+%Y-%m-%d-%H:%M:%S")
                echo $time Word-Biz Service is running, code = $code
                sleep 10s
        fi
done