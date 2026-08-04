FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY worker/build.gradle worker/build.gradle
RUN chmod +x gradlew && ./gradlew dependencies :worker:dependencies --no-daemon || true
COPY src src
COPY worker/src worker/src
RUN ./gradlew bootJar :worker:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/promsearch-api.jar api.jar
COPY --from=build /app/worker/build/libs/promsearch-worker.jar worker.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:8080/test/health-check || exit 1
ENTRYPOINT ["java", "-jar"]
CMD ["api.jar"]
