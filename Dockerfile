# Stage 1: build the jar with Maven (Java 21, matching pom.xml)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# Stage 2: run the jar with a Java 21 runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/lostfound-simple-1.0.0.jar app.jar
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
