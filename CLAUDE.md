# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 技术栈

- Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6
- 数据库：MySQL（生产）/ H2（测试）
- 缓存：StringRedisTemplate 手动控制（本地无 Redis 时自动降级为内存缓存）
- 消息队列：RabbitMQ（可选，默认排除自动配置）
- 构建：Maven

## 编译与运行

```bash
# 编译
mvn compile -f pom.xml

# 运行（需先创建 blogsyetem 数据库）
mvn spring-boot:run -f pom.xml

# 测试（使用 H2 内存库）
mvn test -f pom.xml -Dspring.profiles.active=test
```

## 项目架构

```
com.blog
├── entity/          # 实体类，映射 article 表
├── mapper/          # Mapper 接口，继承 BaseMapper<Article>
├── service/         # 接口继承 IService<Article>，只声明自定义方法
├── service/impl/    # 实现继承 ServiceImpl<ArticleMapper, Article>
├── controller/      # REST 控制器
├── dto/             # 查询 DTO、统一响应 Result<T>
├── config/          # MyBatisPlusConfig（分页插件）、RabbitMQConfig（条件加载）
├── handler/         # MyMetaObjectHandler（自动填充时间）、GlobalExceptionHandler
└── event/           # ArticleEventListener（条件加载）
```

## 关键设计决策

### Service 层继承 ServiceImpl 模式

`ArticleService extends IService<Article>`，`ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>`。

- 标准 CRUD（save、getById、updateById、removeById、list、page）全部从父类继承，无需手写
- `ArticleService` 接口只声明 3 个自定义方法：`page(ArticleQueryDTO)`、`patch()`、`categoryStatistics()`
- `ArticleServiceImpl` 重写 `getById`/`save`/`updateById`/`removeById` 叠加了缓存和 MQ 事件逻辑，内部通过 `super.xxx()` 委托父类

### 缓存：StringRedisTemplate 手动控制

不使用 Spring Cache 注解，直接注入 `StringRedisTemplate` 手动操作 Redis。

- `@Autowired(required = false)` — 本地无 Redis 时为 null，所有缓存方法判空跳过
- 缓存 key：`article::{id}`（单篇）、`categoryStats`（分类统计）
- TTL 统一 30 分钟
- 序列化用 Spring 自带的 Jackson `ObjectMapper`，支持 LocalDateTime

### RabbitMQ：可选，通过条件开关控制

- `application.yml` 中排除了 `RabbitAutoConfiguration`
- `RabbitMQConfig` 和 `ArticleEventListener` 用 `@ConditionalOnProperty(name = "app.rabbitmq-enabled")` 条件加载
- `RabbitTemplate` 注入为 `@Autowired(required = false)`，为 null 时 `sendEvent()` 静默跳过

### MyBatis-Plus 配置

- 分页插件：`PaginationInnerInterceptor(DbType.MYSQL)`
- 自动填充：`MyMetaObjectHandler` 在 insert/update 时自动设置 `createdAt`/`updatedAt`
- 下划线映射：`map-underscore-to-camel-case: true`
- SQL 日志：`StdOutImpl` 输出到控制台

## REST API 端点

所有接口统一返回 `Result<T>`（code + message + data）。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/articles` | 创建文章 |
| GET | `/api/articles?page=&size=&category=&status=&keyword=` | 分页查询 |
| GET | `/api/articles/{id}` | 查单篇 |
| PUT | `/api/articles/{id}` | 完整更新 |
| PATCH | `/api/articles/{id}` | 部分更新 |
| DELETE | `/api/articles/{id}` | 删除 |
| GET | `/api/articles/statistics/category` | 分类文章数量统计 |

## 本地开发环境切换

Redis/RabbitMQ 已做降级处理，本地开发无需安装：

- **缓存**：`StringRedisTemplate` 为 null，所有 `getCache`/`setCache`/`deleteCache` 跳过，直接查库
- **MQ**：`RabbitTemplate` 为 null，`sendEvent()` 跳过

如需启用，修改 `application.yml`：
- 取消 `spring.data.redis` 注释（提供 Redis 连接信息）
- 删除 `spring.autoconfigure.exclude` 中的 RabbitMQ 排除项，设置 `app.rabbitmq-enabled: true`
