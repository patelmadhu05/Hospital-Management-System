# Step 1: Build the application using Maven and Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .

# Step into your actual project folder before building
WORKDIR /app/backend/backend
RUN mvn clean package -DskipTests

# Step 2: Run the application using a lightweight Java 21 runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
# Pull the built jar file from the correct nested target directory
COPY --from=build /app/backend/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]