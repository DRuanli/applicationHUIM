FROM openjdk:22-jdk-slim

WORKDIR /app

# Copy application JAR
COPY target/ptk-huim-u-2.0.0-jar-with-dependencies.jar ptk-huim-u.jar

# Copy configuration files
COPY src/main/resources/logback.xml /app/config/
COPY src/main/resources/application.properties /app/config/

# Create necessary directories
RUN mkdir -p /app/data/input /app/data/output /app/logs

# Set environment variables
ENV JAVA_OPTS="-Xms1g -Xmx2g"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD test -f /app/logs/ptk-huim.log || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar ptk-huim-u.jar"]