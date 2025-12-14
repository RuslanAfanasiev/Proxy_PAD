# Movie API Dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy common module first
COPY common /app/common
WORKDIR /app/common
RUN mvn clean install -DskipTests

# Copy and build movie_api
WORKDIR /app/movie_api
COPY movie_api/pom.xml .
RUN mvn dependency:go-offline

COPY movie_api/src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /app/movie_api/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

USER appuser

EXPOSE 9001

ENV SPRING_PROFILES_ACTIVE=default
ENV SERVER_PORT=9001

ENTRYPOINT ["java", "-jar", "app.jar"]
