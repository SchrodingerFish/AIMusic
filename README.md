# 🎵 曲中人 (AIMusic)

> 初听不识曲中意，再听已是曲中人

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## 📖 项目简介

曲中人（For the Person in the Song）是一个基于人工智能的音乐推荐和问答系统。用户可以提出关于人生、情感、生活的问题，系统会通过AI分析并推荐相关的歌曲，用音乐和歌词来回答用户的疑问。

## ✨ 主要功能

- 🤖 **智能问答**：基于GPT-4的AI问答系统
- 🎵 **音乐推荐**：根据问题内容推荐相关歌曲
- 🌍 **多语言支持**：支持中文和英文界面切换
- 🎨 **现代化UI**：响应式设计，支持移动端
- 📊 **API文档**：集成Knife4j提供完整的API文档
- 🔒 **安全防护**：集成Spring Security安全框架
- 📈 **监控管理**：Spring Boot Actuator健康检查和缓存监控
- ⚡ **缓存优化**：Caffeine缓存提升性能
- 🛡️ **异常处理**：全局异常处理和智能错误过滤
- 🔧 **配置管理**：灵活的配置系统和环境管理

## 🛠️ 技术栈

### 后端技术
- **Spring Boot 3.3.5** - 主框架
- **Spring Web** - Web开发
- **Spring Security** - 安全框架
- **Spring Cache** - 缓存抽象
- **Thymeleaf** - 模板引擎
- **Caffeine** - 高性能缓存
- **Jackson** - JSON处理
- **HttpClient5** - HTTP客户端
- **Knife4j 4.3.0** - API文档
- **Spring Boot Actuator** - 监控管理

### 前端技术
- **HTML5/CSS3** - 页面结构和样式
- **JavaScript** - 交互逻辑
- **Thymeleaf** - 服务端渲染
- **响应式设计** - 移动端适配

### 开发工具
- **Java 21** - 编程语言
- **Maven** - 项目管理
- **Docker** - 容器化部署
- **Spring Boot DevTools** - 开发工具
- **Mockito** - 单元测试

### 部署与运维
- **Docker** - 容器化
- **Docker Compose** - 多容器编排
- **多环境配置** - dev/prod/local环境分离
- **健康检查** - 应用状态监控
- **日志管理** - 结构化日志输出

## 🚀 快速开始

### 环境要求

- Java 21+
- Maven 3.6+
- 网络连接（用于AI服务调用）

### 安装步骤

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd AIMusic
   ```

2. **配置应用**
   
   项目支持多环境配置，包含以下配置文件：
   - `application.yml` - 基础配置
   - `application-dev.yml` - 开发环境配置
   - `application-prod.yml` - 生产环境配置
   - `application-local.yml` - 本地开发配置（不提交到git）
   
   **开发环境（默认）**：
   ```bash
   # 使用开发环境配置
   mvn spring-boot:run
   ```
   
   **本地开发**：
   复制 `application-dev.yml` 为 `application-local.yml`，并配置你的API密钥：
   ```yaml
   ai:
     base-url: https://api.openai.com/v1
     api-key: your-openai-api-key-here
     model-name: gpt-4
     timeout: 30
   ```
   
   **生产环境**：
   使用环境变量配置敏感信息：
   ```bash
   export AI_BASE_URL=https://api.openai.com/v1
   export AI_API_KEY=your-api-key-here
   export AI_MODEL_NAME=gpt-4
   mvn spring-boot:run -Pprod
   ```

3. **编译项目**
   ```bash
   mvn clean compile
   ```

4. **运行应用**
   ```bash
   mvn spring-boot:run
   ```
   
   或者打包后运行：
   ```bash
   mvn clean package -DskipTests
   java -jar target/aimusic-0.0.1-SNAPSHOT.jar
   ```

5. **访问应用**
   - 主页：http://localhost:8080
   - Knife4j API文档：http://localhost:8080/doc.html
   - Swagger UI：http://localhost:8080/swagger-ui.html
   - 健康检查：http://localhost:8080/actuator/health
   - 缓存监控：http://localhost:8080/api/cache/stats

## 🐳 Docker部署

### 构建Docker镜像

项目使用多阶段构建，可以直接从源码构建：

```bash
# 直接从源码构建（推荐）
docker build -t aimusic:latest .

# 构建时指定环境
docker build --build-arg SPRING_PROFILES_ACTIVE=prod -t aimusic:prod .
```

**构建优势**：
- 无需预编译，直接从源码构建
- 镜像更小（使用JRE而非JDK）
- 更安全（非root用户运行）
- 构建优化（.dockerignore排除不必要文件）
- 健康检查内置

### 环境变量配置

复制环境变量模板：
```bash
cp .env.example .env
```

编辑 `.env` 文件配置必要参数：
```bash
# AI服务配置
AI_API_KEY=your-actual-api-key
AI_BASE_URL=https://api.openai.com/v1
AI_MODEL_NAME=gpt-4

# 安全配置
SECURITY_USER_NAME=admin
SECURITY_USER_PASSWORD=your-secure-password

# 代理配置（可选）
PROXY_ENABLED=false
```

### 运行容器

**基本运行**：
```bash
docker run -d \
  --name aimusic \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  --env-file .env \
  -v $(pwd)/logs:/app/logs \
  aimusic:latest
```

**开发模式运行**：
```bash
docker run -d \
  --name aimusic-dev \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e AI_API_KEY=your-dev-key \
  aimusic:latest
```

### 使用Docker Compose（推荐）

项目已包含 `docker-compose.yml` 文件，直接使用：

```bash
# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f aimusic

# 重启服务
docker-compose restart aimusic

# 停止服务
docker-compose down

# 重新构建并启动
docker-compose up -d --build
```

**Docker Compose特性**：
- 自动重启策略
- 健康检查监控
- 日志卷挂载
- 网络隔离
- 环境变量管理

### 生产环境部署建议

1. **资源限制**：
```yaml
services:
  aimusic:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

2. **日志管理**：
```yaml
services:
  aimusic:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

3. **安全加固**：
```bash
# 使用非特权端口
docker run -p 8080:8080 --user 1001:1001 aimusic:latest

# 只读根文件系统
docker run --read-only --tmpfs /tmp aimusic:latest
```

## 📁 项目结构

```
AIMusic/
├── src/
│   ├── main/
│   │   ├── java/com/aimusic/
│   │   │   ├── config/                    # 配置类
│   │   │   │   ├── AiConfig.java          # AI服务配置
│   │   │   │   ├── AppConfig.java         # 应用配置
│   │   │   │   ├── CacheConfig.java       # 缓存配置
│   │   │   │   ├── HttpClientConfig.java  # HTTP客户端配置
│   │   │   │   ├── InternationalizationConfig.java # 国际化配置
│   │   │   │   ├── Knife4jConfig.java     # API文档配置
│   │   │   │   ├── LoggingConfig.java     # 日志配置
│   │   │   │   ├── MonitoringConfig.java  # 监控配置
│   │   │   │   ├── ProxyConfig.java       # 代理配置
│   │   │   │   └── SecurityConfig.java    # 安全配置
│   │   │   ├── controller/                # 控制器
│   │   │   │   ├── CacheController.java   # 缓存监控控制器
│   │   │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│   │   │   │   ├── HealthController.java  # 健康检查控制器
│   │   │   │   └── MainController.java    # 主控制器
│   │   │   ├── service/                   # 服务层
│   │   │   │   ├── AiService.java         # AI服务实现
│   │   │   │   ├── IAiService.java        # AI服务接口
│   │   │   │   ├── IMusicService.java     # 音乐服务接口
│   │   │   │   └── MusicService.java      # 音乐服务实现
│   │   │   ├── dto/                       # 数据传输对象
│   │   │   │   ├── AnswerResponse.java    # 回答响应
│   │   │   │   ├── ApiResponse.java       # API响应
│   │   │   │   ├── MusicInfo.java         # 音乐信息
│   │   │   │   └── QuestionRequest.java   # 问题请求
│   │   │   ├── exception/                 # 异常处理
│   │   │   │   └── BusinessException.java # 业务异常
│   │   │   └── AimusicApplication.java    # 主启动类
│   │   └── resources/
│   │       ├── application.yml            # 基础配置
│   │       ├── application-dev.yml        # 开发环境配置
│   │       ├── application-prod.yml       # 生产环境配置
│   │       ├── application-local.yml      # 本地配置（不提交）
│   │       ├── application-test.yml       # 测试环境配置
│   │       ├── messages/                  # 国际化文件
│   │       │   ├── messages_zh_CN.properties # 中文
│   │       │   └── messages_en_US.properties # 英文
│   │       ├── static/                    # 静态资源
│   │       │   ├── css/style.css          # 样式文件
│   │       │   ├── js/                    # JavaScript文件
│   │       │   └── favicon.ico            # 网站图标
│   │       └── templates/                 # 模板文件
│   │           └── index.html             # 主页模板
│   └── test/                              # 测试代码
│       ├── java/com/aimusic/
│       │   ├── CacheTest.java             # 缓存测试
│       │   └── ...                        # 其他测试类
│       └── resources/
│           └── application-test.yml       # 测试配置
├── target/                                # 编译输出
├── logs/                                  # 日志文件
│   └── aimusic.log                        # 应用日志
├── .dockerignore                          # Docker忽略文件
├── .env.example                           # 环境变量模板
├── .gitignore                             # Git忽略文件
├── Dockerfile                             # Docker构建文件
├── docker-compose.yml                     # Docker Compose配置
├── cache-test-guide.md                    # 缓存测试指南
├── pom.xml                                # Maven配置
└── README.md                              # 项目说明
```

## 🔧 配置说明

### 多环境配置

项目支持多环境配置管理：

- **application.yml** - 基础配置，包含通用设置
- **application-dev.yml** - 开发环境配置
- **application-prod.yml** - 生产环境配置
- **application-local.yml** - 本地开发配置（不提交到git）

### AI服务配置

**开发环境** (`application-dev.yml`)：
```yaml
ai:
  base-url: https://api.openai.com/v1
  api-key: your-development-api-key
  model-name: gpt-4
  timeout: 30
```

**生产环境** (`application-prod.yml`)：
```yaml
ai:
  base-url: ${AI_BASE_URL}
  api-key: ${AI_API_KEY}
  model-name: ${AI_MODEL_NAME:gpt-4}
  timeout: ${AI_TIMEOUT:60}
```

### 代理配置

如果需要通过代理访问AI服务：

```yaml
proxy:
  enabled: ${PROXY_ENABLED:false}
  host: ${PROXY_HOST:127.0.0.1}
  port: ${PROXY_PORT:8080}
```

### 安全配置

**重要**：Spring Security会自动生成默认密码，生产环境必须配置自定义认证：

```yaml
spring:
  security:
    user:
      name: ${SECURITY_USER_NAME:admin}
      password: ${SECURITY_USER_PASSWORD}
      roles: ${SECURITY_USER_ROLES:ADMIN}
```

### 缓存配置

系统使用Caffeine缓存来提升性能：

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=24h,expireAfterAccess=6h
```

### 应用配置

```yaml
app:
  max-question-length: 500  # 问题最大长度限制
```

### 日志配置

**开发环境**：
```yaml
logging:
  level:
    com.aimusic: DEBUG
    org.springframework.cache: DEBUG
    root: INFO
```

**生产环境**：
```yaml
logging:
  level:
    com.aimusic: INFO
    root: WARN
  file:
    name: logs/aimusic.log
    max-size: 50MB
    max-history: 30
```

## 🌐 国际化支持

系统支持中文和英文两种语言：

- 中文：访问 `/?lang=zh_CN`
- 英文：访问 `/?lang=en_US`

国际化文件位置：
- `src/main/resources/messages/messages_zh_CN.properties`
- `src/main/resources/messages/messages_en_US.properties`

## 📊 API文档

项目集成了Knife4j，提供完整的API文档：

- **Knife4j文档**：http://localhost:8080/doc.html
- **Swagger UI**：http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**：http://localhost:8080/v3/api-docs

### 主要API端点

- `POST /api/ask` - 提交问题获取AI回答
- `GET /api/health` - 健康检查
- `GET /api/cache/stats` - 缓存统计信息
- `GET /api/cache/list` - 缓存列表
- `GET /api/cache/clear/{cacheName}` - 清空指定缓存

## 🔍 监控和健康检查

系统集成了Spring Boot Actuator和自定义监控：

### Actuator端点
- 健康检查：`GET /actuator/health`
- 应用信息：`GET /actuator/info`
- 指标监控：`GET /actuator/metrics`

### 缓存监控
- 缓存统计：`GET /api/cache/stats`
- 缓存详情：`GET /api/cache/stats/{cacheName}`
- 缓存管理：`GET /api/cache/clear/{cacheName}`

### 自定义健康检查
- AI服务状态：`GET /api/health/ai`
- 系统状态：`GET /api/health/system`

## 🧪 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=CacheTest

# 运行集成测试
mvn verify
```

### 缓存测试

项目提供了专门的缓存测试指南，详见 `cache-test-guide.md`：

```bash
# 运行缓存测试
mvn test -Dtest=CacheTest

# 查看缓存统计
curl http://localhost:8080/api/cache/stats
```

## 🛡️ 安全特性

### Spring Security配置
- CSRF保护
- 安全头配置（防点击劫持、HSTS等）
- CORS跨域支持

### 全局异常处理
- 智能异常过滤（忽略浏览器自动请求）
- 国际化错误消息
- 统一错误响应格式

### 输入验证
- 问题长度限制
- 参数验证
- 业务规则验证

## 📝 开发指南

### 添加新功能

1. 在 `controller` 包中添加新的控制器
2. 在 `service` 包中实现业务逻辑
3. 在 `dto` 包中定义数据传输对象
4. 更新国际化文件
5. 编写单元测试
6. 更新API文档

### 开发环境设置

1. **IDE配置**：
   - 推荐使用IntelliJ IDEA或Eclipse
   - 安装Lombok插件
   - 配置代码格式化规则

2. **本地开发**：
```bash
# 使用本地配置文件
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# 编辑本地配置
vim src/main/resources/application-local.yml

# 启动应用
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

3. **热重载开发**：
```xml
<!-- 已包含在pom.xml中 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 代码规范

- **Java版本**：使用Java 21语法特性
- **框架规范**：遵循Spring Boot最佳实践
- **代码简化**：使用Lombok减少样板代码
- **异常处理**：统一异常处理机制
- **性能优化**：合理使用缓存提升性能
- **安全规范**：敏感信息通过环境变量配置
- **文档规范**：添加适当的注释和文档
- **测试规范**：编写单元测试
- **日志规范**：使用SLF4J进行日志记录
- **API规范**：遵循RESTful API设计原则

### 项目结构规范

```
src/main/java/com/aimusic/
├── controller/     # 控制器层
├── service/        # 服务层
├── dto/           # 数据传输对象
├── exception/     # 异常处理
├── config/        # 配置类
└── AiMusicApplication.java  # 启动类
```

### Git工作流

1. **分支管理**：
```bash
# 创建功能分支
git checkout -b feature/new-feature

# 提交代码
git add .
git commit -m "feat: add new feature"

# 推送分支
git push origin feature/new-feature
```

2. **提交规范**：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式化
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 测试指南

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AiServiceTest

# 生成测试覆盖率报告
mvn jacoco:report
```

### 缓存使用

```java
@Cacheable(value = "musicCache", key = "#artist + '_' + #song")
public MusicInfo searchSong(String artist, String song) {
    // 实现逻辑
}
```

### 异常处理

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
    // 处理逻辑
}
```

## ❓ 常见问题

### 🔧 部署相关问题

**Q: Docker构建失败怎么办？**

A: 检查以下几点：
1. 确保Docker版本支持多阶段构建
2. 检查网络连接，Maven依赖下载可能较慢
3. 清理Docker缓存：`docker system prune -a`
4. 检查.dockerignore文件是否正确

**Q: 容器启动后无法访问？**

A: 排查步骤：
```bash
# 检查容器状态
docker ps -a

# 查看容器日志
docker logs aimusic

# 检查端口映射
docker port aimusic

# 进入容器调试
docker exec -it aimusic /bin/sh
```

### 🔐 安全相关问题

**Q: Spring Security默认密码在哪里？**

A: 启动日志中会显示：
```
Using generated security password: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```
生产环境必须配置自定义密码：
```yaml
spring:
  security:
    user:
      name: admin
      password: your-secure-password
```

**Q: 如何禁用Spring Security？**

A: 在配置文件中添加：
```yaml
spring:
  autoconfigure:
    exclude: org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

### 🤖 AI服务问题

**Q: AI服务调用失败怎么办？**

A: 请检查以下几点：
1. **API密钥验证**：
   ```bash
   curl -H "Authorization: Bearer $AI_API_KEY" $AI_BASE_URL/models
   ```
2. **网络连接测试**：
   ```bash
   curl -I $AI_BASE_URL
   ```
3. **代理设置检查**：确认代理配置正确
4. **服务状态确认**：检查AI服务提供商状态页面

**Q: AI响应速度慢怎么办？**

A: 优化建议：
1. 调整超时时间：`ai.timeout=60`
2. 使用更快的模型
3. 启用请求缓存
4. 配置合适的代理服务器

### 🔄 缓存相关问题

**Q: 缓存不生效怎么办？**

A: 检查以下配置：
1. **注解检查**：确保`@Cacheable`注解正确
2. **配置验证**：
   ```yaml
   spring:
     cache:
       type: caffeine
   ```
3. **方法调用**：确保通过Spring代理调用
4. **缓存清理**：手动清理缓存测试

**Q: 如何监控缓存状态？**

A: 使用Actuator端点：
```bash
# 查看缓存统计
curl http://localhost:8080/actuator/caches

# 清理特定缓存
curl -X DELETE http://localhost:8080/actuator/caches/musicCache
```

### 🌐 网络相关问题

**Q: 页面显示乱码怎么办？**

A: 确保以下设置正确：
1. **浏览器编码**：设置为UTF-8
2. **服务器配置**：
   ```yaml
   server:
     servlet:
       encoding:
         charset: UTF-8
         force: true
   ```
3. **响应头检查**：确认Content-Type正确

**Q: API文档无法访问？**

A: 确认以下几点：
1. **应用状态**：检查应用是否正常启动
2. **端口检查**：`netstat -tlnp | grep 8080`
3. **防火墙设置**：确保端口开放
4. **路径确认**：访问`/swagger-ui.html`或`/actuator/health`

### 📊 性能相关问题

**Q: 应用启动慢怎么办？**

A: 优化建议：
1. **JVM参数调优**：
   ```bash
   java -Xms512m -Xmx1g -XX:+UseG1GC -jar app.jar
   ```
2. **依赖优化**：移除不必要的依赖
3. **配置调整**：禁用不需要的自动配置

**Q: 内存使用过高怎么办？**

A: 排查步骤：
1. **内存分析**：使用JProfiler或VisualVM
2. **缓存配置**：调整缓存大小限制
3. **GC调优**：选择合适的垃圾收集器
4. **资源限制**：在Docker中设置内存限制

## 🔄 版本更新

### v2.0.0 (2025-06-21) - 当前版本
- ✨ **新增功能**：
  - 多环境配置支持（dev/prod/local）
  - Docker多阶段构建
  - 完整的Docker Compose部署方案
  - 环境变量配置模板
  - 增强的安全配置
- 🔧 **改进优化**：
  - 优化项目结构和配置管理
  - 完善文档和部署指南
  - 增强错误处理和日志记录
  - 性能优化和缓存改进
- 🐛 **问题修复**：
  - 修复Docker构建问题
  - 解决配置文件加载问题
  - 优化资源使用和内存管理

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 贡献指南

我们欢迎所有形式的贡献！请遵循以下步骤：

### 贡献流程

1. **Fork项目**
   ```bash
   git clone https://github.com/your-username/aimusic.git
   cd aimusic
   ```

2. **创建功能分支**
   ```bash
   git checkout -b feature/amazing-feature
   ```

3. **提交更改**
   ```bash
   git commit -m 'feat: add amazing feature'
   ```

4. **推送分支**
   ```bash
   git push origin feature/amazing-feature
   ```

5. **创建Pull Request**

### 贡献类型

- 🐛 **Bug修复**：修复现有功能的问题
- ✨ **新功能**：添加新的功能特性
- 📚 **文档改进**：完善文档和注释
- 🎨 **代码优化**：改进代码结构和性能
- 🧪 **测试增强**：添加或改进测试用例
- 🔧 **配置优化**：改进配置和部署流程

### 代码审查标准

- 代码符合项目规范
- 包含适当的测试用例
- 文档更新完整
- 通过所有CI检查
- 功能完整且稳定

## 📞 联系方式

如有问题或建议，请通过以下方式联系：

- 📧 **邮箱**：your-email@example.com
- 🐛 **问题反馈**：[GitHub Issues](https://github.com/your-username/aimusic/issues)
- 💬 **功能建议**：[GitHub Discussions](https://github.com/your-username/aimusic/discussions)
- 📖 **文档问题**：[Wiki页面](https://github.com/your-username/aimusic/wiki)

## 🙏 致谢

感谢以下开源项目和贡献者：

- [Spring Boot](https://spring.io/projects/spring-boot) - 应用框架
- [Thymeleaf](https://www.thymeleaf.org/) - 模板引擎
- [Caffeine](https://github.com/ben-manes/caffeine) - 缓存库
- [Docker](https://www.docker.com/) - 容器化平台
- 所有贡献者和用户的支持

---

⭐ 如果这个项目对你有帮助，请给我们一个星标！

🔗 **项目链接**：[https://github.com/your-username/aimusic](https://github.com/your-username/aimusic)

---

**让音乐为你的人生指路** 🎵

*Built with ❤️ using Spring Boot and AI*