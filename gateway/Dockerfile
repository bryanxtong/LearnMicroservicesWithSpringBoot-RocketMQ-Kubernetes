FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system --gid 1001 app && useradd --system --uid 1001 --gid 1001 --create-home --home-dir /home/app app
COPY ./target/gateway-0.0.1-SNAPSHOT.jar /usr/src/gateway/
COPY opentelemetry-javaagent.jar /usr/src/gateway/
RUN chown -R 1001:1001 /usr/src/gateway
WORKDIR /usr/src/gateway
USER 1001
EXPOSE 8000
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -javaagent:opentelemetry-javaagent.jar -jar gateway-0.0.1-SNAPSHOT.jar"]
