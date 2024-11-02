FROM eclipse-temurin:17-jdk-focal

VOLUME /tmp

ENV TZ=Asia/Tehran

RUN  mkdir -p /var/log/notice-api
RUN  chmod -R 777 /var/log/notice-api

COPY target/*.jar notice-api-1.0.10-SNAPSHOT.jar
ENTRYPOINT ["java","-Xdebug","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1717","-jar","/notice-api-1.0.10-SNAPSHOT.jar"]
