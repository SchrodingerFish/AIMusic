# 使用官方的OpenJDK 21 运行时作为基础镜像
# 第一阶段：构建阶段
FROM maven:3.9.5 AS builder

# 设置工作目录
WORKDIR /app

# 复制pom.xml文件.  先复制pom.xml, 可以利用docker缓存. 如果pom.xml没变, 这层会直接使用缓存，跳过maven依赖下载
COPY pom.xml .

# 下载 Maven 依赖. 这一步会下载所有依赖，如果pom.xml没变，会直接使用缓存
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用（跳过测试以加快构建速度）
# 使用 -Dspring.profiles.active=prod  指定profile
RUN mvn clean package -DskipTests -Pprod -Dspring.profiles.active=prod

# 第二阶段：运行阶段
FROM openjdk:21-slim-buster AS production

# 安装curl用于健康检查，并清理apt缓存
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 创建非root用户和组
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 设置jar文件的所有者
RUN chown appuser:appuser app.jar

# 创建日志目录并设置所有者
RUN mkdir -p /app/logs && chown -R appuser:appuser /app/logs

# 切换到非root用户
USER appuser

# 暴露应用端口
ENV SERVER_PORT=8080
EXPOSE ${SERVER_PORT}

# 设置JVM参数和启动应用
# 添加 G1GC 垃圾回收器，通常更适合微服务
# 调整堆大小.  根据你的应用实际情况调整. 建议根据应用实际的内存使用情况进行调整
ENV JAVA_OPTS="-XX:+UseG1GC -Xmx512m -Xms256m"

# 使用 exec java... 可以让应用更容易接收到docker stop信号
ENTRYPOINT exec java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -Djava.security.egd=file:/dev/./urandom -jar app.jar

# 健康检查
# 增加健康检查的日志输出，方便排错
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:${SERVER_PORT}/actuator/health || (echo "Health check failed"; exit 1)
