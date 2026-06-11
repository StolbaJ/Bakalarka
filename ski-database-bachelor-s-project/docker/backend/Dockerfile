# Multi-stage build pro Spring Boot aplikaci
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies
COPY Backend_ski/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY Backend_ski/ .
RUN mvn clean package -DskipTests

# Runtime stage - použít non-Alpine image kvůli Argon2 nativním knihovnám
FROM eclipse-temurin:21-jre

WORKDIR /app

# Create non-root user
RUN groupadd --system spring && \
    useradd --system spring --gid spring

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
