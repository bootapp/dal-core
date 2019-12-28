#!/usr/bin/env bash
cd ../ && ./gradlew build && cd docker
cp ../build/libs/*.jar ./app.jar
docker login --username=bootapp@1044358033708111 -pabcd1234 registry.cn-beijing.aliyuncs.com
docker build -t registry.cn-beijing.aliyuncs.com/bootapp/dal-core .
docker push registry.cn-beijing.aliyuncs.com/bootapp/dal-core