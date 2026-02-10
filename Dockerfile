# B1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
COPY . .
RUN mvn clean package -DskipTests

# B2: Chạy
FROM amazoncorretto:21-alpine-jdk
# Sửa dòng này để lấy file JAR chính xác
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
# Thêm lệnh kiểm tra nội dung folder để debug nếu vẫn lỗi
ENTRYPOINT ["java", "-jar", "app.jar"]