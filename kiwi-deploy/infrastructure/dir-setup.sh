cd ~
mkdir microservice-kiwi docker storage_data store_path tracker_data
cd docker/
mkdir kiwi ui rabbitmq mysql
cd kiwi
mkdir auth config crawler eureka gate upms word
mkdir auth/logs config/logs crawler/logs crawler/tmp eureka/logs gate/logs upms/logs word/logs word/bizTmp word/crawlerTmp word/biz word/crawler
cd ../ui
mkdir dist nginx
# download gcp key json on root directory
