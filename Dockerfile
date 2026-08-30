FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN mvn -pl ${MODULE} -am -DskipTests package

FROM eclipse-temurin:17-jre
ARG MODULE
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
