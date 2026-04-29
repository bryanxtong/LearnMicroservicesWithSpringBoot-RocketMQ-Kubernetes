FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system --gid 1001 app && useradd --system --uid 1001 --gid 1001 --create-home --home-dir /home/app app
COPY ./target/logs-0.0.1-SNAPSHOT.jar /usr/src/logs/
COPY opentelemetry-javaagent.jar /usr/src/logs/
RUN chown -R 1001:1001 /usr/src/logs
WORKDIR /usr/src/logs
USER 1001
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -javaagent:opentelemetry-javaagent.jar -jar logs-0.0.1-SNAPSHOT.jar"]
