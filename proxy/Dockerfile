# Proxy Dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy and build proxy
WORKDIR /app/proxy
COPY proxy/pom.xml .
RUN mvn dependency:go-offline

COPY proxy/src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/proxy/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

EXPOSE 8080

ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
