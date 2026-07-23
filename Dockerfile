# Multi-stage build: React UI + Spring Boot app

FROM node:20-alpine AS ui-build
WORKDIR /app/chat-ui
COPY chat-ui/package*.json ./
RUN npm install
COPY chat-ui/. .
RUN npm run build

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY --from=ui-build /app/chat-ui/build ./src/main/resources/static
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
