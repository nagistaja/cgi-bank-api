# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy as builder

# Set the working directory
WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew .
COPY gradle ./gradle

# Copy the build configuration files
COPY build.gradle .
COPY settings.gradle .

# Download dependencies (leverage Docker cache)
# Grant execute permission to gradlew
RUN chmod +x ./gradlew && ./gradlew dependencies

# Copy the source code
COPY src ./src

# Build the application JAR
RUN ./gradlew bootJar

# Use a smaller base image for the final stage
FROM eclipse-temurin:17-jre-jammy

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the builder stage
COPY --from=builder /app/build/libs/account-service-*.jar app.jar

# Expose the application port
EXPOSE 8080

# Define the entry point for the container
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"] 