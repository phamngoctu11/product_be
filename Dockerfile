# STAGE 1: Sử dụng Maven để build ra file .jar
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build bỏ qua test để tăng tốc độ đóng gói
RUN mvn clean package -DskipTests

# STAGE 2: Chạy ứng dụng với JRE nhẹ
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
