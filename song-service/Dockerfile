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
EXPOSE 8081
# The log4j2.xml will be mounted at /app/config/log4j2.xml
CMD ["java", "-Dlog4j.configurationFile=/app/config/log4j2.xml", "-jar", "app.jar"]