# Stage 1: build the jar with Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Stage 2: run the jar with a small Java runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/lostfound-simple-1.0.0.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
