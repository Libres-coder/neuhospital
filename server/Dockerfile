# syntax=docker/dockerfile:1.7

# ===== Stage 1: build the layered Spring Boot jar =====
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Pre-fetch dependencies in a separate layer so changing src/ doesn't bust the cache.
COPY pom.xml ./
RUN mvn -B -q -e -ntp -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -q -e -ntp -DskipTests package \
    && cp target/hospital-server-*.jar /workspace/app.jar

# ===== Stage 2: extract layers from the layered jar =====
FROM eclipse-temurin:17-jre AS layers
WORKDIR /workspace
COPY --from=build /workspace/app.jar /workspace/app.jar
RUN java -Djarmode=layertools -jar /workspace/app.jar extract

# ===== Stage 3: assemble the runtime image =====
FROM eclipse-temurin:17-jre AS runtime

# Run as a non-root user.
RUN groupadd -r app && useradd -r -g app -u 10001 app

ENV TZ=Asia/Shanghai \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -XX:+UseG1GC -XX:MaxRAMPercentage=75.0" \
    SERVER_PORT=8090

WORKDIR /app

# Copy layers in dependency order (least-frequently-changed first).
COPY --from=layers --chown=app:app /workspace/dependencies/ ./
COPY --from=layers --chown=app:app /workspace/spring-boot-loader/ ./
COPY --from=layers --chown=app:app /workspace/snapshot-dependencies/ ./
COPY --from=layers --chown=app:app /workspace/application/ ./

USER app

EXPOSE 8090

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -qO- "http://127.0.0.1:${SERVER_PORT}/actuator/health/liveness" || exit 1

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]