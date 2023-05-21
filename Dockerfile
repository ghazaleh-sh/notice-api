FROM adoptopenjdk/openjdk11

VOLUME /tmp

ENV TZ=Asia/Tehran

RUN  mkdir -p /var/log/notice-api
RUN  chmod -R 777 /var/log/notice-api

COPY target/*.jar notice-api-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-Xdebug","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1717","-jar","/notice-api-0.0.1-SNAPSHOT.jar"]
