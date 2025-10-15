# Use Maven image to build the application
FROM maven:3.9-eclipse-temurin-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use Java runtime for the final image
FROM eclipse-temurin:17-jre

# Set working directory
WORKDIR /app

# Copy the built WAR file and webapp-runner
COPY --from=build /app/target/rideshare1.war .
COPY --from=build /app/target/dependency/webapp-runner.jar .

# Expose port (Render will override with $PORT)
EXPOSE 8080

# Set default environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Start command
CMD java $JAVA_OPTS -jar webapp-runner.jar --port ${PORT:-8080} rideshare1.war
