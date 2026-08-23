FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn --batch-mode dependency:go-offline
COPY src ./src
RUN mvn --batch-mode -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S rental && adduser -S rental -G rental \
    && mkdir -p /app/uploads \
    && chown -R rental:rental /app
COPY --from=build --chown=rental:rental /workspace/target/*.jar /app/app.jar
USER rental
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
