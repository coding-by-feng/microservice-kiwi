# CenterOS 7 and Above

[Tencent Cloud 618 Flash Sale Price](https://cloud.tencent.com/act/618party?fromSource=gwzcw.3621912.3621912.3621912&utm_medium=cpc&utm_id=gwzcw.3621912.3621912.3621912&from=console&cps_key=28653b30444ff81cf593422221fb1ba3 "")
Note that the security network group of the cloud service should open ports and disable restrictions.

# docker

```
yum update
```

[https://www.runoob.com/docker/centos-docker-install.html](https://www.runoob.com/docker/centos-docker-install.html "")

## Create Docker Configuration File

```
mkdir /etc/docker
vim /etc/docker/daemon.json
{
        "exec-opts": ["native.cgroupdriver=systemd"],
        "registry-mirrors": ["https://zggyaen3.mirror.aliyuncs.com"]
}
```

# 安装docker-compose

[Reference](https://www.runoob.com/docker/docker-compose.html "")
It is recommended to download the package locally and then manually upload it to the server, as otherwise unexpected issues may arise.

```
sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
```

## Local Upload Installation

[docker-compose下载链接](https://github-releases.githubusercontent.com/15045751/e1ef3000-b16e-11eb-9df7-091c00bdf356?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20210612%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20210612T054621Z&X-Amz-Expires=300&X-Amz-Signature=4da20a27a079cd195b024bafc9f6c6200c02e47b9e45cbf862a3f00d25227ae7&X-Amz-SignedHeaders=host&actor_id=22954596&key_id=0&repo_id=15045751&response-content-disposition=attachment%3B%20filename%3Ddocker-compose-Linux-x86_64&response-content-type=application%2Foctet-stream "")

```
# First download
sshpass -p fenxxx210 scp -r ~/Downloads/docker-compose-Linux-x86_64 root@119.29.200.130:/usr/local/bin
mv docker-compose-Linux-x86_64 docker-compose
chmod +x /usr/local/bin/docker-compose
ln -s /usr/local/bin/docker-compose /usr/bin/docker-compose
docker-compose --version
```

## Install yum

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

Add the following mappings to the end of the hosts file:

```
127.0.0.1                                       fastdfs.fengorz.me
127.0.0.1                                       kiwi-microservice-local
127.0.0.1                                       kiwi-microservice
127.0.0.1                                       kiwi-ui
127.0.0.1                                       kiwi-eureka
127.0.0.1                                       kiwi-redis
127.0.0.1                                       kiwi-rabbitmq
your_dfs_ip                                     kiwi-fastdfs

127.0.0.1                                     kiwi-config
127.0.0.1                                     kiwi-auth
127.0.0.1                                     kiwi-upms
127.0.0.1                                     kiwi-gate
```

Be sure to replace your_ecs_ip with the external IP address of the cloud server where fastdfs is located.

# mysql

```
docker pull mysql:5.7.34
docker run -itd --name kiwi-mysql -p 3306:3306 -v /root/docker/mysql:/mysql_tmp -e MYSQL_ROOT_PASSWORD=fengORZ123 --net=host mysql:5.7.34
sudo docker exec -it kiwi-mysql bash
mysql -h localhost -u root -p
create database kiwi_db;
exit
# Migrate the kiwi_db table from MySQL
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
docker run -itd --name kiwi-redis -p 6379:6379 redis --requirepass "Xxxxxxx"
# Test
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

Enter command search mode by pressing ?, search for tracker_server, press Enter, and change the following IP address to kiwi-fastdfs

exit

docker container restart `docker ps -a| grep storage | awk '{print $1}' `
```

# maven installation

```
yum install maven
```

After installation, run`mvn clean install -Dmaven.test.skip=true`

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

# Automitic deployment

```
cd ~/microservice-kiwi/kiwi-deploy/docker
cp deployKiwi.sh ~
cd ~
chmod 777 deployKiwi.sh
```

# elasticsearch

```
docker run -d -p 9200:9200 -p 9300:9300 --hostname kiwi-es -e "discovery.type=single-node" -e "xpack.security.enabled=true" -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" --name kiwi-es -v es_config:/usr/share/elasticsearch/config  -v es_data:/usr/share/elasticsearch/data elasticsearch:7.16.1
 
#Enter container
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

Create index`kiwi_vocabulary`

## kibana installation

[Docker official link](https://www.elastic.co/guide/en/kibana/current/docker.html#docker "")

```
docker pull docker.elastic.co/kibana/kibana:7.6.2
docker run -d --link kiwi-es -p 5601:5601 docker.elastic.co/kibana/kibana:7.6.2
```

## Installing IK Tokenizer

```
sudo docker exec -it kiwi-es bash
# The Elasticsearch version and the IK tokenizer version must be consistent, otherwise it will fail upon restart.
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.6.2/elasticsearch-analysis-ik-7.6.2.zip
exit
docker restart kiwi-es 
```

# Running Frontend Project with Nginx

## Compile the frontend project and upload it to the directory ready for deployment

```
sshpass -p PASSWORD scp -r LOCAL_UI_PROJECT_BUILD_PATH root@x.x.x.x:~/docker/ui/
```

## Prepare Nginx Environment

```
docker pull nginx
docker build -f ~/microservice-kiwi/kiwi-deploy/kiwi-ui/Dockerfile -t kiwi-ui:1.0 ~/docker/ui/
docker run -d -v ~/docker/ui/dist/:/usr/share/nginx/html --net=host --name=kiwi-ui -it kiwi-ui:1.0
```

## Modify the Nginx Configuration File for Kiwi-UI

```
sudo docker exec -it kiwi-ui bash
cp /etc/nginx/nginx.conf /usr/share/nginx/html/
exit
vi docker/ui/dist/nginx.conf
```

Then

```
user  nginx;
worker_processes  auto;

error_log  /var/log/nginx/error.log notice;
pid        /var/run/nginx.pid;

events {
    worker_connections  1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    keepalive_timeout  65;

    #gzip  on;

    #include /etc/nginx/conf.d/*.conf;

    server {
        listen 80;
        server_name www.kiwidict.com;
        rewrite ^(.*) https://$server_name$1 permanent;
    }

    server {
        listen 443 ssl;
        server_name  www.kiwidict.com;
        ssl_certificate www.kiwidict.com_bundle.crt;
        ssl_certificate_key www.kiwidict.com.key;
        ssl_session_timeout 5m;
        ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
        ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:HIGH:!aNULL:!MD5:!RC4:!DHE;
        ssl_prefer_server_ciphers on;

        access_log  /var/log/nginx/host.access.log;

        location / {
            root   /usr/share/nginx/html;
            index  index.html index.htm;
        }

        location ~* ^.+\.(css|js|ico|gif|jpg|jpeg|png|otf|woff|woff2)$ {
            log_not_found off;
            access_log off;
            proxy_redirect off;
            proxy_set_header Host $host;
            expires 365d;
            root /usr/share/nginx/html;
        }

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
    }
}
```

After saving, overwrite the original default.conf and restart the kiwi-ui container:

```
sudo docker exec -it kiwi-ui bash
mv /usr/share/nginx/html/default.conf /etc/nginx/conf.d/default.conf
# Verify Nginx configuration
# /usr/sbin/nginx -tc /etc/nginx/conf.d/default.conf
# /sbin/nginx -t
/usr/sbin/nginx -t
exit

docker container restart `docker ps -a| grep kiwi-ui | awk '{print $1}'`
```

Upload the compiled Vue project(dist directory) to nginx html directory

## Apply for a free SSL certificate and configure HTTPS protocol

[https://cloud.tencent.com/document/product/400/35244](https://cloud.tencent.com/document/product/400/35244 "")

