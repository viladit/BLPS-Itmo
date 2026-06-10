FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM tomcat:10.1-jdk21-temurin

ENV SPRING_PROFILES_ACTIVE=docker
ENV APP_SECURITY_SEED_ENABLED=true

RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/ozon-seller-backend-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
