# Stage 1: Build the app with Maven and Java 17
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

# Build the JAR (skip test compilation and execution)
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Runtime image with Java 17
FROM eclipse-temurin:17-jre-jammy

# Copy the built JAR from the builder stage (shade plugin outputs to dist/ directory)
COPY --from=builder /app/dist/echo.jar /app/echo.jar

# Copy the properties file (will be mounted or copied from host)
COPY echo.properties /app/echo.properties

# Create an assets directory and copy any root-level .bin files there
RUN mkdir -p /app/assets
COPY assets/ /app/assets/

# Allow overriding assets directory via NOMAD_ASSETS_DIR; default is /app/assets
ENV NOMAD_ASSETS_DIR=/app/assets

# Set working directory
WORKDIR /app

# Expose sample lobby ports (adjust based on your lobbies)
EXPOSE 5730-5739

# Run the server
CMD ["java", "-jar", "echo.jar"]
