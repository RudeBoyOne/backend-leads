FROM gradle:9.3.0-jdk25 AS build
WORKDIR /app
# O .dockerignore preserva gradle/ e gradlew, necessarios para o wrapper
COPY . .
RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
# Nome exato: build/libs/ contem tambem backend-leads-0.0.1-SNAPSHOT-plain.jar
COPY --from=build /app/build/libs/backend-leads-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
