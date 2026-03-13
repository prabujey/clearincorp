# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/llc-api-1.0-SNAPSHOT.jar app.jar

# Railway provides PORT env variable
ENV PORT=8080
EXPOSE ${PORT}

# JVM options for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Start the application with proper JVM options
CMD java ${JAVA_OPTS} -jar -Dspring.profiles.active=railway -Dserver.port=${PORT} app.jar
