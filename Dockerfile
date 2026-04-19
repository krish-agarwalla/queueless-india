# Stage 1: Build the Java application using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight image to run the app
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built .jar file from Stage 1
COPY --from=build /app/target/*.jar app.jar

# Expose the port Spring Boot uses
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]