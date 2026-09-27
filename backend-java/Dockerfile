FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd -m appuser
USER appuser
WORKDIR /app
COPY --from=build /app/target/avms-backend-*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/api/health/ | grep -q '"status":"ok"'
ENTRYPOINT ["java", "-jar", "app.jar"]
