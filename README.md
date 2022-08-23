# CenterOS 7 以上

[腾讯云618秒杀价](https://cloud.tencent.com/act/618party?fromSource=gwzcw.3621912.3621912.3621912&utm_medium=cpc&utm_id=gwzcw.3621912.3621912.3621912&from=console&cps_key=28653b30444ff81cf593422221fb1ba3 "")
注意云服务的安全网络组要放开端口禁用

# docker

```
yum update
```

[https://www.runoob.com/docker/centos-docker-install.html](https://www.runoob.com/docker/centos-docker-install.html "")

## 创建docker配置文件

```
mkdir /etc/docker
vim /etc/docker/daemon.json
{
        "exec-opts": ["native.cgroupdriver=systemd"],
        "registry-mirrors": ["https://zggyaen3.mirror.aliyuncs.com"]
}
```

# 安装docker-compose

[参考](https://www.runoob.com/docker/docker-compose.html "")
建议本机下载包，再手动上传到服务器，不然可能会出现莫名其妙的问题

```
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

## 本地上传安装

[docker-compose下载链接](https://github-releases.githubusercontent.com/15045751/e1ef3000-b16e-11eb-9df7-091c00bdf356?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20210612%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20210612T054621Z&X-Amz-Expires=300&X-Amz-Signature=4da20a27a079cd195b024bafc9f6c6200c02e47b9e45cbf862a3f00d25227ae7&X-Amz-SignedHeaders=host&actor_id=22954596&key_id=0&repo_id=15045751&response-content-disposition=attachment%3B%20filename%3Ddocker-compose-Linux-x86_64&response-content-type=application%2Foctet-stream "")

```
# 先下载
sshpass -p fenxxx210 scp -r ~/Downloads/docker-compose-Linux-x86_64 root@119.29.200.130:/usr/local/bin
mv docker-compose-Linux-x86_64 docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
docker-compose --version
```

## yum安装

```
yum install docker-compose
```

# directory

```
cd ~
mkdir microservice-kiwi docker storage_data store_path tracker_data
cd docker/
mkdir kiwi ui rabbitmq mysql
cd kiwi
mkdir auth config crawler eureka gate upms word
mkdir auth/logs config/logs crawler/logs crawler/tmp eureka/logs gate/logs upms/logs word/logs word/bizTmp word/crawlerTmp word/biz word/crawler
cd ../ui
mkdir dist nginx
```

# git pull

```
yum install git
cd ~/microservice-kiwi/
git init
git pull https://github.com/coding-by-feng/microservice-kiwi.git/
git remote add gitee https://gitee.com/fengorz/microservice-kiwi.git
git fetch --all
git reset --hard gitee/master
git pull
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

127.0.0.1                                     kiwi-config
127.0.0.1                                     kiwi-auth
127.0.0.1                                     kiwi-upms
127.0.0.1                                     kiwi-gate
```

注意将上面your_ecs_ip替换成fastdfs所在云服务器的外网ip

# mysql

```
docker pull mysql:5.7.34
docker run -itd --name kiwi-mysql -p 3306:3306 -v /root/docker/mysql:/mysql_tmp -e MYSQL_ROOT_PASSWORD=fengORZ123 --net=host mysql:5.7.34
sudo docker exec -it kiwi-mysql bash
mysql -h localhost -u root -p
create database kiwi_db;
exit
# 迁移Mysql的kiwi_db表
mysqldump --host=cdb-0bhxucw9.gz.tencentcdb.com --port=10069 -uroot -pfengORZ123 -C --databases kiwi_db |mysql --host=localhost -uroot -pfengORZ123 kiwi_db
mysql -h localhost -u root -p
use kiwi_db
select * from star_rel_his limit 0, 100;
exit
exit
```

# redis

```
docker pull redis:latest
docker run -itd --name kiwi-redis -p 6379:6379 redis --requirepass "fengORZ123"
# 测试
docker exec -it kiwi-redis /bin/bash
redis-cli
keys *
exit
```

# rabbitmq

```
docker pull rabbitmq:management
# docker run -d --hostname kiwi-rabbit --name kiwi-rabbit -p 15555:15672 rabbitmq:management
docker run -d --hostname kiwi-rabbit -v ~/docker/rabbitmq:/tmp --name kiwi-rabbit --net=host rabbitmq:management
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

# maven 安装

```
yum install maven
```

安装之后再项目根目录执行`mvn clean install -Dmaven.test.skip=true`

- ~~先注释掉microservice-kiwi和kiwi-cloud-service的pom.xml所有子模块依赖，然后分别执行mvn clean install -Dmaven.test.skip=true~~
- ~~再放开所有注释在microservice-kiwi和kiwi-cloud-service下mvn clean install -Dmaven.test.skip=true~~
- ~~分别在kiwi-common、kiwi-bdf、kiwi-upms、kiwi-word执行mvn clean install -Dmaven.test.skip=true（如果报错同样需要先注释子模块依赖）~~

## maven settings.xml

```
cd ~/.m2
vi settings.xml
```

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <pluginGroups />
    <proxies />
    <servers />
    
    <localRepository>/root/.m2/repository</localRepository>
    
    <mirrors>
        <mirror>
            <id>alimaven</id>
            <mirrorOf>central</mirrorOf>
            <name>aliyun maven</name>
            <url>http://maven.aliyun.com/nexus/content/repositories/central/</url>
        </mirror>
        <mirror>
            <id>alimaven</id>
            <name>aliyun maven</name>
            <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
        <mirror>
            <id>central</id>
            <name>Maven Repository Switchboard</name>
            <url>http://repo1.maven.org/maven2/</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
        <mirror>
            <id>repo2</id>
            <mirrorOf>central</mirrorOf>
            <name>Human Readable Name for this Mirror.</name>
            <url>http://repo2.maven.org/maven2/</url>
        </mirror>
        <mirror>
            <id>ibiblio</id>
            <mirrorOf>central</mirrorOf>
            <name>Human Readable Name for this Mirror.</name>
            <url>http://mirrors.ibiblio.org/pub/mirrors/maven2/</url>
        </mirror>
        <mirror>
            <id>jboss-public-repository-group</id>
            <mirrorOf>central</mirrorOf>
            <name>JBoss Public Repository Group</name>
            <url>http://repository.jboss.org/nexus/content/groups/public</url>
        </mirror>
        <mirror>
            <id>google-maven-central</id>
            <name>Google Maven Central</name>
            <url>https://maven-central.storage.googleapis.com
            </url>
            <mirrorOf>central</mirrorOf>
        </mirror>
        <mirror>
            <id>maven.net.cn</id>
            <name>oneof the central mirrors in china</name>
            <url>http://maven.net.cn/content/groups/public/</url>
            <mirrorOf>central</mirrorOf>
        </mirror>
    </mirrors>
</settings>
```

# 自动部署

```
cd ~/microservice-kiwi/kiwi-deploy/docker
cp deployKiwi.sh ~
cd ~
chmod 777 deployKiwi.sh
```

# elasticsearch

```
docker run -d -p 9200:9200 -p 9300:9300 --hostname kiwi-es -e "discovery.type=single-node" -e "xpack.security.enabled=true" -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" --name kiwi-es -v es_config:/usr/share/elasticsearch/config  -v es_data:/usr/share/elasticsearch/data elasticsearch:7.16.1
 
#进入容器
/elasticsearch-setup-passwords auto
 
docker pull docker.elastic.co/elasticsearch/elasticsearch:7.6.2
docker run -d --name kiwi-es -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.6.2
curl http://localhost:9200

elasticsearch-users useradd xxsuperuser -r superuser

curl -XPUT -u xxsuperuser:xxxxx http://xxxxx:9200/_xpack/security/user/xxxxxxxusername/_password -H 
"Content-Type: application/json" -d '
{
  "password": "xxxx"
}'

```

安装完了注意创建index，名为`kiwi_vocabulary`

## kibana安装

[Docker 官方](https://www.elastic.co/guide/en/kibana/current/docker.html#docker "")

```
docker pull docker.elastic.co/kibana/kibana:7.6.2
docker run -d --link kiwi-es -p 5601:5601 docker.elastic.co/kibana/kibana:7.6.2
```

## 安装ik分词器

```
sudo docker exec -it kiwi-es bash
# elasticsearch的版本和ik分词器的版本需要保持一致，不然在重启的时候会失败。
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.6.2/elasticsearch-analysis-ik-7.6.2.zip
exit
docker restart kiwi-es 
```

# nginx运行前端项目

## 现将前端项目编译好上传到准备部署的目录

```
sshpass -p PASSWORD scp -r LOCAL_UI_PROJECT_BUILD_PATH root@x.x.x.x:~/docker/ui/
```

## 准备nginx环境

```
docker pull nginx
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
server {
    listen 80;
    server_name www.kiwidict.com;
    rewrite ^(.*) https://$server_name$1 permanent;
}

server {
    listen 443 ssl;
    server_name  www.kiwidict.com;
    ssl_certificate 1_kiwidict.com_bundle.crt;
    ssl_certificate_key 2_kiwidict.com.key;
    ssl_session_timeout 5m;
    ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;
    ssl_prefer_server_ciphers on;

    access_log  /var/log/nginx/host.access.log  main;

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
    }
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

打开nginx请求日志记录：

```
access_log  /var/log/nginx/host.access.log  main;
```

保存之后覆盖原来的default.conf重启kiwi-ui容器

```
sudo docker exec -it kiwi-ui bash
mv /usr/share/nginx/html/default.conf /etc/nginx/conf.d/default.conf
# 验证nginx配置
# /usr/sbin/nginx -tc /etc/nginx/conf.d/default.conf
# /sbin/nginx -t
/usr/sbin/nginx -t
exit

docker container restart `docker ps -a| grep kiwi-ui | awk '{print $1}'`
```

## 上传Vue编译后的项目

WebStorm执行编译命令生成静态文件。

## ssl证书免费申请、https协议配置

[https://cloud.tencent.com/document/product/400/35244](https://cloud.tencent.com/document/product/400/35244 "")

## 首页加载提速

[前端项目时因chunk-vendors过大导致首屏加载太慢的优化](https://blog.csdn.net/qq_31677507/article/details/102742196 "")