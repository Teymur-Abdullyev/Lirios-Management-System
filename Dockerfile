# Build stage
FROM gradle:7.6-jdk17 as builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# Runtime stage
FROM openjdk:17-slim
WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /app/LIRIOSBEAUTYBackend/build/libs/*.jar app.jar

EXPOSE 8080

# Set environment variables for deployment
ENV PORT=8080
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

CMD ["java", "-jar", "app.jar"]
