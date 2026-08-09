FROM eclipse-temurin:21-jre@sha256:8cef5fc7bebe421363ab543a2f4db5caf7d119d8db67d56b0f56c485d2de4d55

ARG JAR_FILE=runner/target/runner-1.0-SNAPSHOT.jar
WORKDIR /opt/archemides
COPY --chown=10001:10001 ${JAR_FILE} application.jar

EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "application.jar"]
