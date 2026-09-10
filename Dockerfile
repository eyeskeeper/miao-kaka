# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
# 先拉依赖，利用 Docker 层缓存
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre
# 业务切日按北京时间
ENV TZ=Asia/Shanghai
WORKDIR /app
COPY --from=build /build/target/miao-kaka-*.jar app.jar
EXPOSE 18089
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
