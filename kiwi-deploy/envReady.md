# CenterOS 7 以上
[腾讯云618秒杀价](https://cloud.tencent.com/act/618party?fromSource=gwzcw.3621912.3621912.3621912&utm_medium=cpc&utm_id=gwzcw.3621912.3621912.3621912&from=console&cps_key=28653b30444ff81cf593422221fb1ba3 "")
注意云服务的安全网络组要放开端口禁用

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

# host
```
vi /etc/hosts
```
将一下映射增加到hosts文件末尾
```
127.0.0.1                                       fastdfs.fengorz.me
127.0.0.1                                       kiwi-microservice-local
127.0.0.1                                       kiwi-microservice
127.0.0.1                                       kiwi-eureka
127.0.0.1                                       kiwi-redis
127.0.0.1                                       kiwi-rabbitmq
your_dfs_ip                                     kiwi-fastdfs
```
注意将上面your_ecs_ip替换成fastdfs所在云服务器的外网ip

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

# nginx运行前端项目
## 现将前端项目编译好上传到准备部署的目录
```
sshpass -p PASSWORD scp -r LOCAL_UI_PROJECT_BUILD_PATH root@x.x.x.x:~/docker/ui/
```
## 准备nginx环境
```
docker build -f ~/microservice-kiwi/kiwi-deploy/kiwi-ui/Dockerfile -t kiwi-ui:1.0 ~/docker/ui/
docker run -d -v ~/docker/ui/dist/:/usr/share/nginx/html --net=host --name=kiwi-ui -it kiwi-ui:1.0
```
## 更改kiwi-ui的nginx配置文件
```
sudo docker exec -it kiwi-ui bash
cp /etc/nginx/conf.d/default.conf /usr/share/nginx/html/
exit
vi docker/ui/dist/default.conf
```
在
```
location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
    }
```
下面增加配置：
```
    location /auth {
        proxy_pass http://kiwi-microservice:9991;
    }

    location /wordBiz {
        proxy_pass http://kiwi-microservice:9991;
    }

    location /code {
        proxy_pass http://kiwi-microservice:9991;
    }

    location /admin {
        proxy_pass http://kiwi-microservice:9991;
    }
```
保存之后重启kiwi-ui容器
```
docker container restart `docker ps -a| grep kiwi-ui | awk '{print $1}'`
```

# fastdfs（season/fastdfs）
```
docker run -ti -d --name tracker -v ~/tracker_data:/fastdfs/tracker/data --net=host season/fastdfs tracker

docker run -ti -d --name storage -v ~/storage_data:/fastdfs/storage/data -v ~/store_path:/fastdfs/store_path --net=host -e TRACKER_SERVER:192.168.1.2:22122 season/fastdfs storage

sudo docker exec -it storage bash

mv /etc/apt/sources.list /etc/apt/sources.list.bak && \
    echo "deb http://mirrors.163.com/debian/ jessie main non-free contrib" >/etc/apt/sources.list && \
    echo "deb http://mirrors.163.com/debian/ jessie-proposed-updates main non-free contrib" >>/etc/apt/sources.list && \
    echo "deb-src http://mirrors.163.com/debian/ jessie main non-free contrib" >>/etc/apt/sources.list && \
    echo "deb-src http://mirrors.163.com/debian/ jessie-proposed-updates main non-free contrib" >>/etc/apt/sources.list

apt-get update
apt-get install vim

vi /fdfs_conf/storage.conf
# 按?进入命令搜索模式，输入tracker_server，按回车，将后面的ip地址改成kiwi-fastdfs
exit

docker container restart `docker ps -a| grep storage | awk '{print $1}' `
```