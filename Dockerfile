# 使用官方的OpenJDK 21运行时作为基础镜像
# 第一阶段：构建阶段
FROM maven:3.9.5-openjdk-21-slim AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 构建应用（跳过测试以加快构建速度）
RUN mvn clean package -DskipTests -Pprod

# 第二阶段：运行阶段
FROM openjdk:21-jre-slim

# 安装curl用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/target/aimusic-*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 创建非root用户
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# 暴露应用端口
EXPOSE 8080

# 设置JVM参数和启动应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1