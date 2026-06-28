# Stage 1: Build the JAR with Maven + JDK 17
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# Download deps first (cached layer)
RUN mvn dependency:go-offline -q
COPY src/ src/
RUN mvn package -DskipTests -q

# Stage 2: Runtime with FFmpeg
FROM eclipse-temurin:17-jdk
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=builder /build/target/blog-backend-1.0.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
