# docker

```
yum update
yum install docker

vi ~/.bashrc
alias docker='podman'
source ~/.bashrc
sudo touch /etc/containers/nodocker
```

Create a command shortcut that represent docker ps on CentOS

# 安装docker-compose

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

# Install Python
```
sudo yum install -y epel-release
sudo dnf install -y python3 python3-pip
pip3 --version
```

# Install podman-compose
```
git clone https://github.com/containers/podman-compose.git
cd podman-compose
pip3 install -r requirements.txt
python3 setup.py install --user
podman-compose --version

alias docker-compose='podman-compose'
echo "alias docker-compose='podman-compose'" >> ~/.bashrc
source ~/.bashrc
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
git remote add origin https://github.com/coding-by-feng/microservice-kiwi.git
git fetch --all
git reset --hard origin/master
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
127.0.0.1                                       kiwi-ui
127.0.0.1                                       kiwi-eureka
127.0.0.1                                       kiwi-redis
127.0.0.1                                       kiwi-rabbitmq
127.0.0.1                                       ${DB_IP}
127.0.0.1                                       kiwi-es

127.0.0.1                                     kiwi-config
127.0.0.1                                     kiwi-auth
127.0.0.1                                     kiwi-upms
127.0.0.1                                     kiwi-gate
```

注意将上面your_ecs_ip替换成fastdfs所在云服务器的外网ip

# Setup Environment Variables
For Linux:
```
export KIWI_ENC_PASSWORD="xxxx"
source ~/.bashrc
```
For My Mac:
```
vi ~/.zshrc 
export KIWI_ENC_PASSWORD="xxxx"
source ~/.zshrc
```

# mysql

```
docker pull mysql:5.7.34
# replace the wildcard(My_Password) with my password
docker run -itd --name kiwi-mysql -p 3306:3306 -v /root/docker/mysql:/mysql_tmp -e MYSQL_ROOT_PASSWORD=My_Password --net=host mysql:5.7.34
sudo docker exec -it kiwi-mysql bash
mysql -h localhost -u root -p
create database kiwi_db;
exit
# 迁移Mysql的kiwi_db表
# replace the wildcard(My_Password) with my password
mysqldump --host=cdb-0bhxucw9.gz.tencentcdb.com --port=10069 -uroot -pMy_Password -C --databases kiwi_db |mysql --host=localhost -uroot -pMy_Password kiwi_db
mysql -h localhost -u root -p
use kiwi_db
select * from star_rel_his limit 0, 100;
exit
exit
```

# redis

```
docker pull redis:latest
# replace the wildcard(My_Password) with my password
docker run -itd --name kiwi-redis -p 6379:6379 redis --requirepass "My_Password"
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

docker run -ti -d --name storage -v ~/storage_data:/fastdfs/storage/data -v ~/store_path:/fastdfs/store_path --net=host -e TRACKER_SERVER:kiwi-fastdfs:22122 season/fastdfs storage

# sudo docker exec -it storage bash

# apt-get update
# apt-get install vim

# vi /fdfs_conf/storage.conf
# 按?进入命令搜索模式，输入tracker_server，按回车，将后面的ip地址改成kiwi-fastdfs
# exit

docker container restart `docker ps -a| grep storage | awk '{print $1}' `
```

# maven 安装

```
yum install maven
```

安装之后再项目根目录执行`mvn clean install -Dmaven.test.skip=true`

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

Install Chrome Elasticvue plugin and login to Elasticsearch server

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
exit
```

在

保存之后覆盖原来的default.conf重启kiwi-ui容器

```
sudo docker exec -it kiwi-ui bash
# check nginx configuration and apt-get install vi to setup the configuration
nginx -t
# nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
# nginx: configuration file /etc/nginx/nginx.conf test is successful

# configuration begin
user nginx;
worker_processes auto;

error_log /var/log/nginx/error.log debug;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';

    access_log /var/log/nginx/access.log main;

    sendfile on;
    #tcp_nopush on;

    keepalive_timeout 65;

    gzip on;
    gzip_types text/plain text/css application/json application/javascript;
    gzip_min_length 256;

    include /etc/nginx/conf.d/*.conf;

    server {
        server_name dict.fengorz.me; # managed by Certbot

        location / {
            root /usr/share/nginx/html;
            index index.html index.htm;
        }

        location /auth {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /wordBiz {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /code {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /admin {
            proxy_pass http://kiwi-microservice:9991;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        listen 443 ssl; # managed by Certbot
        ssl_certificate /etc/letsencrypt/live/dict.fengorz.me/fullchain.pem; # managed by Certbot
        ssl_certificate_key /etc/letsencrypt/live/dict.fengorz.me/privkey.pem; # managed by Certbot
        include /etc/letsencrypt/options-ssl-nginx.conf; # managed by Certbot
        ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem; # managed by Certbot
    }

    server {
        if ($host = dict.fengorz.me) {
                return 301 https://$host$request_uri;
        } # managed by Certbot

        listen 80 ;
        server_name dict.fengorz.me;
        return 404; # managed by Certbot
    }
}
# configuration end

exit

docker container restart `docker ps -a| grep kiwi-ui | awk '{print $1}'`
```


## ssl证书免费申请、https协议配置

https://pentagonal-icecream-1a5.notion.site/Free-SSL-Certificate-on-Nginx-Godaddy-1a5a4b6391df803b98aae953c17cd1fa?pvs=4