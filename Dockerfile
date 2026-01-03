# We will use multi stage build for a smaller image size

# Stage1: Build the application
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy maven wrapper and pom.xml first (for dependency caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached if pom.xml doesnt change)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application (Skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Stage 2: Create runtime image
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port 8080
expose 8080

ENTRYPOINT ["java", "-jar", "app.jar"]