#!/bin/bash

killall autoCheckService.sh

echo "stop container beginning"

docker stop $(docker ps -a| grep kiwi-crawler | awk '{print $1}' )
docker stop $(docker ps -a| grep kiwi-word-biz | awk '{print $1}' )
docker stop $(docker ps -a| grep kiwi-upms | awk '{print $1}' )
docker stop $(docker ps -a| grep kiwi-auth | awk '{print $1}' )
docker stop $(docker ps -a| grep kiwi-gate | awk '{print $1}' )
docker stop $(docker ps -a| grep kiwi-config | awk '{print $1}' )
docker stop $(docker ps -a| grep kiwi-eureka | awk '{print $1}' )

echo "stop container success"
