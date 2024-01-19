# syntax=docker/dockerfile:1
FROM --platform=$BUILDPLATFORM postgres:13.3-alpine AS builder
ARG TARGETPLATFORM
ARG BUILDPLATFORM
RUN echo "Running on $BUILDPLATFORM, building EHRbase for $TARGETPLATFORM" > /log

# SHOW POSTGRES SERVER AND CLIENT VERSION
RUN postgres -V && \
    psql -V

# SET POSTGRES DATA DIRECTORY TO CUSTOM FOLDER
# CREATE CUSTOM DATA DIRECTORY AND CHANGE OWNERSHIP TO POSTGRES USER
# INITIALIZE DB IN CUSTOM DATA DIRECTORY
# NOTE: default data directory is /var/lib/postgresql/data and the
#       approach of this multi stage dockerfile build does not work with it!
ENV PGDATA="/var/lib/postgresql/pgdata"
RUN mkdir -p ${PGDATA}; \
    chown postgres: ${PGDATA}; \
    chmod 0700 ${PGDATA}; \
    su - postgres -c "initdb -D ${PGDATA}"

# COPY DB SETUP SCRIPT
# START DB AND LET THE SCRIPT DO ALL REQUIRED CONFIGURATION
COPY base/db-setup/createdb.sql /postgres/createdb.sql
RUN su - postgres -c "pg_ctl -D ${PGDATA} -w start" && \
    su - postgres -c "psql < /postgres/createdb.sql" && \
    su - postgres -c "pg_ctl -D ${PGDATA} -w stop"

# INSTALL JAVA 17 JDK
#FIXME version is fixed to 17.0.6_p10-r0 because 17.0.7 is currently broken (https://packages.adoptium.net/ui/native/apk/alpine/main/x86_64/)
ENV JAVA_HOME /usr/lib/jvm/java-17-temurin
ENV PATH $JAVA_HOME/bin:$PATH
RUN wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk && \
    echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories && \
    su -c "apk add temurin-17=17.0.6_p10-r0" && \
    java --version

# INSTALL MAVEN
ENV MAVEN_VERSION 3.8.6
ENV MAVEN_HOME /usr/lib/mvn
ENV PATH $MAVEN_HOME/bin:$PATH
RUN wget http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    tar -zxvf apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    rm apache-maven-$MAVEN_VERSION-bin.tar.gz && \
    mv apache-maven-$MAVEN_VERSION /usr/lib/mvn && \
    mvn --version

# COPY POMs
RUN ls -la
COPY ./pom.xml ./pom.xml
COPY ./api/pom.xml ./api/pom.xml
COPY ./application/pom.xml ./application/pom.xml
COPY ./base/pom.xml ./base/pom.xml
COPY ./jooq-pq/pom.xml ./jooq-pq/pom.xml
COPY ./rest-ehr-scape/pom.xml ./rest-ehr-scape/pom.xml
COPY ./rest-openehr/pom.xml ./rest-openehr/pom.xml
COPY ./service/pom.xml ./service/pom.xml
COPY ./test-coverage/pom.xml ./test-coverage/pom.xml
COPY ./plugin/pom.xml ./plugin/pom.xml
COPY ./bom/pom.xml ./bom/pom.xml

# COPY SOURCEFILES
COPY ./api/src ./api/src
COPY ./application/src ./application/src
COPY ./base/src ./base/src
COPY ./jooq-pq/src ./jooq-pq/src
COPY ./rest-ehr-scape/src ./rest-ehr-scape/src
COPY ./rest-openehr/src ./rest-openehr/src
COPY ./service/src ./service/src
COPY ./plugin/src ./plugin/src

# START DB AND PACKAGE EHRBASE .JAR
RUN ls -la; \
    su - postgres -c "pg_ctl -D ${PGDATA} -w start" && \
    mvn package -Dmaven.javadoc.skip=true -Djacoco.skip=true -Dmaven.test.skip && \
    su - postgres -c "pg_ctl -D ${PGDATA} -w stop"

# WRITE EHRBASE VERSION TO A FILE
# MOVE EHRBASE.jar TO /tmp FOLDER
RUN ls -la; \
    EHRBASE_VERSION=$(mvn -q -Dexec.executable="echo" \
                             -Dexec.args='${project.version}' \
                             --non-recursive exec:exec) && \
    echo ${EHRBASE_VERSION} > /tmp/ehrbase_version && \
    cp application/target/application-${EHRBASE_VERSION}.jar /tmp/ehrbase.jar





# FINAL EHRBASE IMAGE WITH JRE AND JAR ONLY
FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jre-ubi9-minimal AS final
COPY --from=builder /tmp/ehrbase.jar .
COPY --from=builder /tmp/ehrbase_version .
COPY .docker_scripts/docker-entrypoint.sh .
RUN chmod +x ./docker-entrypoint.sh; \
    echo "EHRBASE_VERSION: $(cat ehrbase_version)"

# SET DEFAULT ENVS (CAN BE OVERRITEN FROM CLI VIA --build-arg FLAG)
ARG DB_URL=jdbc:postgresql://ehrdb:5432/ehrbase
ARG DB_USER="ehrbase"
ARG DB_PASS="ehrbase"
ARG SERVER_NODENAME=local.ehrbase.org

# THESE ENVIRONMENT VARIABLES ARE ALSO APPLIED TO STARTUP OF THE CONTAINER
# AND CAN BE OVERWRITTEN WITH THE '-e' FLAG ON 'docker run' COMMAND
ENV EHRBASE_VERSION=${EHRBASE_VERSION}
ENV DB_USER=${DB_USER}
ENV DB_PASS=${DB_PASS}
ENV DB_URL=${DB_URL}
ENV SERVER_NODENAME=${SERVER_NODENAME}

# SECURITY ENVs
ENV SECURITY_AUTHTYPE="NONE"
ENV SECURITY_AUTHUSER="ehrbase-user"
ENV SECURITY_AUTHPASSWORD="SuperSecretPassword"
ENV SECURITY_AUTHADMINUSER="ehrbase-admin"
ENV SECURITY_AUTHADMINPASSWORD="EvenMoreSecretPassword"
ENV SECURITY_OAUTH2USERROLE="USER"
ENV SECURITY_OAUTH2ADMINROLE="ADMIN"
ENV SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI=""

# STATUS METRIC ENDPOINT ENVs
ENV MANAGEMENT_ENDPOINTS_WEB_EXPOSURE="env,health,info,metrics,prometheus"
ENV MANAGEMENT_ENDPOINTS_WEB_BASEPATH="/management"
ENV MANAGEMENT_ENDPOINT_ENV_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_HEALTH_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_HEALTH_DATASOURCE_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_INFO_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_METRICS_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_PROMETHEUS_ENABLED="false"
ENV MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED="true"
ENV CACHE_ENABLED="true"

EXPOSE 8080
CMD ./docker-entrypoint.sh
