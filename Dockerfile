FROM docker.io/library/eclipse-temurin:17-jdk-alpine
VOLUME /tmp

COPY ./target/extracted/dependencies/ ./
COPY ./target/extracted/spring-boot-loader/ ./
COPY ./target/extracted/application/ ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
#ENTRYPOINT ["java", "-jar", "tng-key-sync-1.0-SNAPSHOT.jar"]