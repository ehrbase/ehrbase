#
# Builder
#
FROM maven:3-eclipse-temurin-21-alpine as build

COPY ./application/target/ehrbase.jar /usr/src/ehrbase.jar
WORKDIR /usr/src

# unpack jar and run jdeps + jlink
RUN jar xf ehrbase.jar
RUN jdeps \
    --ignore-missing-deps -q  \
    --recursive  \
    --multi-release 21  \
    --print-module-deps  \
    --class-path 'BOOT-INF/lib/*'  \
    ehrbase.jar > deps.info
RUN jlink \
    --add-modules $(cat deps.info) \
    --strip-debug \
    --compress 2 \
    --no-header-files \
    --no-man-pages \
    --output /jre
# extract layers
RUN java -Djarmode=layertools -jar ehrbase.jar extract --destination /usr/src/extract

#
# EHRbase Image
#
FROM alpine:3.19.1

# define jre
ENV JAVA_HOME /opt/jre
ENV PATH $JAVA_HOME/bin:$PATH

# switch to nion root user
RUN addgroup -S ehrbase && adduser -S ehrbase -G ehrbase
USER ehrbase

# copy custom jre
COPY --from=build /jre $JAVA_HOME

# copy and merge layer BOOT-INF dirs to create a single app
COPY --from=build /usr/src/extract/dependencies/ /opt/ehrbase
COPY --from=build /usr/src/extract/spring-boot-loader/ /opt/ehrbase
COPY --from=build /usr/src/extract/snapshot-dependencies/ /opt/ehrbase
COPY --from=build /usr/src/extract/application/ /opt/ehrbase

# switch workspace
WORKDIR /opt/ehrbase

# run using spring JarLauncher
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=docker org.springframework.boot.loader.launch.JarLauncher"]
