FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY target/identity-service-0.0.1-SNAPSHOT.jar app.jar

USER spring

EXPOSE 8100

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
  CMD bash -ec 'exec 3<>/dev/tcp/localhost/8100; printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3; grep -q "\"status\":\"UP\"" <&3'

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
