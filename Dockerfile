# Multi-stage build for smaller final image
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/beauty-center-*.jar app.jar

# Create uploads directory for file uploads
RUN mkdir -p /app/uploads

# Set production profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV FILE_UPLOAD_DIR=/app/uploads

# Run the application
CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
