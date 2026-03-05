# Multi-stage Dockerfile for Todo Service
# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS build

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Download dependencies (for caching)
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src/ src/

# Build the JAR
RUN ./gradlew bootJar --no-daemon

# Stage 2: Runtime with slim JRE
FROM eclipse-temurin:25-jre-ubi10-minimal

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
