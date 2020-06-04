# CenterOS 7 以上
[腾讯云618秒杀价](https://cloud.tencent.com/act/618party?fromSource=gwzcw.3621912.3621912.3621912&utm_medium=cpc&utm_id=gwzcw.3621912.3621912.3621912&from=console&cps_key=28653b30444ff81cf593422221fb1ba3 "")

# docker
```
yum update
```
[https://www.runoob.com/docker/centos-docker-install.html](https://www.runoob.com/docker/centos-docker-install.html "")

# directory
```
cd ~
mkdir microservice-kiwi docker storage_data store_path tracker_data
cd docker/
mkdir kiwi ui
cd kiwi
mkdir auth config crawler eureka gate upms word
mkdir auth/logs config/logs crawler/logs eureka/logs gate/logs upms/logs word/logs
cd ../ui
mkdir dist nginx
```

# git pull
```
yum install git
cd ~/microservice-kiwi/
git init
git pull https://github.com/coding-by-feng/microservice-kiwi.git/
```

# redis
```
docker pull redis:latest
docker run -itd --name kiwi-redis -p 6379:6379 redis
# 测试
docker exec -it kiwi-redis /bin/bash
redis-cli
keys *
exit
```

# rabbitmq
```
docker run -d --hostname kiwi-rabbit --name some-rabbit -p 15555:15672 rabbitmq:management
```

# nginx
## 现将前端项目编译好上传到准备部署的目录
```
sshpass -p PASSWORD scp -r LOCAL_UI_PROJECT_BUILD_PATH root@x.x.x.x:/root/docker/ui/
```
## 准备nginx环境
```
docker build -f ~/microservice-kiwi/kiwi-deploy/Dockerfile -t kiwi-ui:1.0 ~/docker/ui/
docker run -v /root/docker/ui/dist/:/usr/share/nginx/html -v /root/docker/ui/nginx/:/etc/nginx --net=host --name=kiwi-ui -it kiwi-ui:1.0
```

