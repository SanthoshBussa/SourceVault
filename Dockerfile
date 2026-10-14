# Stage 1: Build fat JAR from source
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy POMs and source trees
COPY pom.xml .
COPY sourcevault-common ./sourcevault-common
COPY website-analysis-service ./website-analysis-service
COPY resource-collection-service ./resource-collection-service
COPY archive-service ./archive-service
COPY job-service ./job-service
COPY api-gateway ./api-gateway
COPY sourcevault-server ./sourcevault-server

# Build executable production JAR (frontend assets are pre-bundled in sourcevault-server resources)
RUN mvn clean package -DskipTests

# Stage 2: Minimal, secure JRE runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create directories for database and archive storage
RUN mkdir -p /app/data /app/output/archives /app/data/collected-resources /app/data/temp-staging && \
    chmod -R 777 /app/data /app/output

# Copy fat JAR from build stage
COPY --from=builder /workspace/sourcevault-server/target/sourcevault-server-1.0.0.jar /app/sourcevault-server.jar

# Configurable environment variables for Render & Cloud
ENV PORT=10000
ENV SOURCEVAULT_STORAGE_DIR=/app/data/collected-resources
ENV SOURCEVAULT_OUTPUT_DIR=/app/output/archives

EXPOSE ${PORT}

# Run with container-aware memory and dynamic port binding
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/sourcevault-server.jar --server.port=${PORT}"]
