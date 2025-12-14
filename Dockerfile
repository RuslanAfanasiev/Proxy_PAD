# Multi-stage build for Proxy PAD Application
# This Dockerfile builds both the proxy and movie_api services
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copy all project files
COPY . .

# Build common module first
WORKDIR /build/common
RUN mvn clean install -DskipTests

# Build movie_api
WORKDIR /build/movie_api
RUN mvn clean package -DskipTests

# Build proxy
WORKDIR /build/proxy
RUN mvn clean package -DskipTests

# Runtime stage - Proxy (main entry point)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JARs
COPY --from=build /build/proxy/target/*.jar /app/proxy.jar
COPY --from=build /build/movie_api/target/*.jar /app/movie_api.jar

# Install supervisor to run multiple services
RUN apk add --no-cache supervisor

# Create supervisor configuration
RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisord.conf

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup && \
    chown -R appuser:appgroup /app /var/log/supervisor

USER appuser

# Expose ports
EXPOSE 8080 9001 9002

# Start supervisor
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisord.conf"]
