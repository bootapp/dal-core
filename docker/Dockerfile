FROM openjdk:8-jre-alpine
RUN apk --no-cache add ca-certificates
ADD app.jar /app.jar
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JAVA_OPTS=""
CMD java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar /app.jar

EXPOSE 9080
