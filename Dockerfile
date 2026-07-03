# Sử dụng môi trường Java 21 siêu nhẹ
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy trực tiếp file jar đã build từ thư mục target vào container
COPY target/*.jar app.jar

# Mở cổng 8080
EXPOSE 8080

# Lệnh khởi chạy Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]