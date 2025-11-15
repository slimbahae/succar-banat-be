# Use Eclipse Temurin (recommended OpenJDK distribution) as base image
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Create uploads directory for file uploads
RUN mkdir -p /app/uploads

# Cloud Run will provide PORT environment variable (default: 8080)
# No need to EXPOSE as Cloud Run ignores it

# Set production profile
ENV SPRING_PROFILES_ACTIVE=prod
ENV FILE_UPLOAD_DIR=/app/uploads

# Run the jar file
# Cloud Run provides PORT env var, Spring Boot will use server.port=${PORT:8080}
CMD ["java", "-Xmx512m", "-Xms256m", "-jar", "target/beauty-center-*.jar"]
