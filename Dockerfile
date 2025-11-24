FROM docker.io/library/eclipse-temurin:17-jdk-alpine
VOLUME /tmp

COPY ./target/extracted/dependencies/ ./
COPY ./target/extracted/spring-boot-loader/ ./
# Skip Snapshot Dependencies because we have no
# COPY ./target/extracted/snapshot-dependencies/ ./
COPY ./target/extracted/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
#ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar tng-key-sync-1.0-SNAPSHOT.jar" ]