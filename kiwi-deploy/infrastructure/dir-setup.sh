cd ~
mkdir -p microservice-kiwi docker
cd docker/
mkdir -p kiwi ui rabbitmq mysql
cd kiwi
mkdir -p auth config crawler eureka gate upms word
mkdir -p auth/logs config/logs crawler/logs crawler/tmp eureka/logs gate/logs upms/logs word/logs word/bizTmp word/crawlerTmp word/biz word/crawler
cd ../ui
mkdir -p dist nginx
# download gcp key json on root directory
