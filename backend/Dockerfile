# Multi-stage build for Scala application
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-21.0.5_11_1.10.6_3.6.2 AS builder

WORKDIR /app

# Copy build files
COPY build.sbt .
COPY project ./project

# Download dependencies (cached layer)
RUN sbt update

# Copy source code
COPY src ./src

# Build fat JAR
RUN sbt assembly

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/scala-3.6.2/url-shortener.jar ./app.jar

# Set ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8000

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8000/health || exit 1

# Run application
CMD ["java", "-jar", "app.jar"]
