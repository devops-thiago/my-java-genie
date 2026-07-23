# Multi-stage build for Java RAG System

# Stage 1: Build React UI
FROM node:20-alpine AS ui-build
WORKDIR /app/chat-ui

# Copy package files and install dependencies
COPY chat-ui/package*.json ./
RUN npm install

# Copy UI source and build
COPY chat-ui/. .
RUN npm run build

# Stage 2: Build Java Application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy UI build output to static resources
COPY --from=ui-build /app/chat-ui/build ./src/main/resources/static

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl for healthchecks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Create directories for logs and docs
RUN mkdir -p /app/logs /app/docs

# Expose application port
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
