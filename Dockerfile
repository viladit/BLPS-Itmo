FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -P wildfly -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -P wildfly -DskipTests package

FROM quay.io/wildfly/wildfly:35.0.1.Final-jdk21

ENV WILDFLY_HOME=/opt/jboss/wildfly
ENV SPRING_PROFILES_ACTIVE=wildfly
ENV APP_SECURITY_SEED_ENABLED=true

COPY --chown=jboss:root --from=build /root/.m2/repository/org/postgresql/postgresql/*/postgresql-*.jar /tmp/postgresql.jar
COPY --chown=jboss:root docker/wildfly/configure-datasources.cli /tmp/configure-datasources.cli

RUN "${WILDFLY_HOME}/bin/jboss-cli.sh" --file=/tmp/configure-datasources.cli \
    && rm -f /tmp/configure-datasources.cli /tmp/postgresql.jar \
    && rm -rf "${WILDFLY_HOME}/standalone/configuration/standalone_xml_history"

COPY --from=build /workspace/target/ozon-seller-backend-0.0.1-SNAPSHOT.war "${WILDFLY_HOME}/standalone/deployments/ROOT.war"

EXPOSE 8080
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
