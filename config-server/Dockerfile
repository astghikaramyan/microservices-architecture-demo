FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Install curl for Docker healthcheck
RUN apk add --no-cache curl
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8888
CMD ["java", "-jar", "app.jar"]