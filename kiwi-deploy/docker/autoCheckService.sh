#!/bin/bash

runningCode='200'
while true
do

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

done