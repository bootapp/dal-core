#!/usr/bin/env bash
cd ../ && ./gradlew build && cd docker
cp ../build/libs/*.jar ./app.jar
docker build -t bootapp/dal-core .
docker push bootapp/dal-core