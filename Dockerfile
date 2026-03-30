# Build stage
FROM gradle:7.6-jdk17 AS builder
WORKDIR /app
COPY LIRIOSBEAUTYBackend /app/LIRIOSBEAUTYBackend
WORKDIR /app/LIRIOSBEAUTYBackend
RUN gradle clean build -x test

# Runtime stage
FROM amazoncorretto:17-alpine
WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /app/LIRIOSBEAUTYBackend/build/libs/*.jar /app/app.jar

EXPOSE 8080

# Set environment variables for deployment
ENV PORT=8080
ENV SPRING_JPA_HIBERNATE_DDL_AUTO=update

CMD ["java", "-jar", "app.jar"]
