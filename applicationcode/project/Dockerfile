# Step 1: Build stage (use Maven to package the app)
FROM maven:3.9.9-eclipse-temurin-22 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the JAR
RUN mvn clean package -DskipTests

# Step 2: Runtime stage (use lightweight JRE image)
FROM eclipse-temurin:22-jre-alpine

WORKDIR /app

# Copy the jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Environment variables (optional)
ENV AWS_REGION=ca-central-1

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
