# Multi-stage Docker build for SourceVault
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create directories for persistent database and archive storage
RUN mkdir -p /app/data /app/output/archives /app/data/collected-resources /app/data/temp-staging && \
    chmod -R 777 /app/data /app/output

# Copy pre-packaged all-in-one executable jar (includes Spring Boot backend + bundled React frontend)
COPY sourcevault-server/target/sourcevault-server-1.0.0.jar /app/sourcevault-server.jar

# Configurable environment variables for Render
ENV PORT=10000
ENV SOURCEVAULT_STORAGE_DIR=/app/data/collected-resources
ENV SOURCEVAULT_OUTPUT_DIR=/app/output/archives

EXPOSE ${PORT}

# Run with container-aware memory and dynamic port binding
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -jar /app/sourcevault-server.jar --server.port=${PORT}"]
