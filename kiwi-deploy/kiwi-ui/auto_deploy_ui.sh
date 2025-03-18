#!/bin/bash

cd ~/docker/ui/ && mv -f /home/kiwi_s_f_zhan/dist.zip ./ && rm -rf dist __MACOSX/ && unzip dist.zip && chmod -R 777 dist && docker restart kiwi-ui && echo "Deployment completed successfully!"