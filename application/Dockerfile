FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY application/pom.xml .
COPY application/src ./src
RUN mvn package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*-SNAPSHOT.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=2s --start-period=20s --retries=20 \
  CMD wget -qO- http://localhost:8080/v3/api-docs || exit 1

ENTRYPOINT ["java","-jar","/app/app.jar"]