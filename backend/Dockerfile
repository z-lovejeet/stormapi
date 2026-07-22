# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy Maven wrapper files and pom.xml
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Make mvnw executable and resolve dependencies offline
RUN chmod +x mvnw && (./mvnw dependency:go-offline -B || true)

# Copy source files and build application jar
COPY src ./src
RUN ./mvnw package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user storm
RUN addgroup -S storm && adduser -S storm -G storm

WORKDIR /app

# Copy application jar from build stage
COPY --from=build --chown=storm:storm /app/target/stormapi-backend-0.1.0.jar /app/app.jar

USER storm

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-Xms512m", "-Xmx2g", "-XX:MaxMetaspaceSize=256m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
