# rabbitmq
docker run -d -m 200M --hostname my-rabbit --name some-rabbit -p 15555:15672 rabbitmq:management

# nginx
docker run -p 16818:80 -v /root/docker/ui/dist/:/usr/share/nginx/html -v /root/docker/ui/nginx/:/etc/nginx --name=kiwi-ui -it kiwi-ui:1.0

