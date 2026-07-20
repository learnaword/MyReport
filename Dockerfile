# ---- Build Stage ----
FROM maven:3.9-eclipse-temurin-8 AS builder

WORKDIR /app

COPY pom.xml .
COPY lib ./lib
RUN mvn dependency:go-offline -B || true

COPY src ./src
RUN mvn package -DskipTests -B \
    && cp target/MyReport-*.jar /app/app.jar

# ---- Runtime Stage ----
FROM eclipse-temurin:8-jre

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd -r app && useradd -r -g app app

ENV TZ=Asia/Shanghai
ENV SERVER_PORT=9091

WORKDIR /app

COPY --from=builder /app/app.jar app.jar

EXPOSE 9091

USER app

ENTRYPOINT ["java", "-jar", "app.jar"]
