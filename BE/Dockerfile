# syntax=docker/dockerfile:1

# ---- Build: compile and package the Spring Boot jar with the Maven Wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Resolve dependencies in their own layer so source edits don't re-download them.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Tests run in the regular build (./mvnw test); the image only packages the app.
RUN ./mvnw -B -q package -DskipTests && cp target/*.jar app.jar

# ---- Run: slim JRE with only the jar, as a non-root user ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/app.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
