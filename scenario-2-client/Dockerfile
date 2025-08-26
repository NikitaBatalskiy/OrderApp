FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY scenario-2-client/pom.xml .
COPY scenario-2-client/src ./src
RUN mvn package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*-SNAPSHOT.jar app.jar
COPY docker/wait-for-url.sh wait-for-url.sh
RUN chmod +x /app/wait-for-url.sh

ENTRYPOINT ["sh","-c","/app/wait-for-url.sh http://order-app:8080/v3/api-docs 180 && java -Dspring.profiles.active=docker -jar /app/app.jar"]
