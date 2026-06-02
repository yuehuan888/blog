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
├── entity/          # 4 个实体：Article, ArticleLike, ArticleFavorite, ArticleRead
├── mapper/          # 4 个 Mapper，继承 BaseMapper，自定义 SQL 用注解
├── service/         # 4 个 Service 接口
├── service/impl/    # 4 个 Service 实现，继承 ServiceImpl<Mapper, Entity>
├── controller/      # ArticleController（13 个端点）
├── dto/             # Result<T>, ArticleQueryDTO, ToggleResult, HotArticleDTO
├── config/          # MyBatisPlusConfig, RabbitMQConfig(条件), AsyncConfig
├── handler/         # MyMetaObjectHandler, GlobalExceptionHandler
├── event/           # ArticleEventListener(MQ), ArticleReadEventListener(@Async), ReadEvent
└── task/            # HotArticleScheduler（定时重建热榜 ZSET）
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

### 点赞/收藏：Redis Set + DB 双写

- Redis Set `article:like:{id}` / `article:favorite:{id}` 存储 userId 字符串，O(1) 判断状态
- Toggle 模式：`SISMEMBER` 查 → `SADD` + DB 插 + 计数 +1（点赞）或 `SREM` + DB 删 + 计数 -1（取消）
- 降级：Redis 不可用时 catch 异常走 DB 查询 `article_like`/`article_favorite` 表
- 原子计数：`ArticleMapper` 的 `@Update` SQL 直接 `SET like_count = like_count +/- 1`
- `@Transactional` 保证 DB 记录 + 计数的原子性

### 阅读统计：去重 + ZSET + 异步事件

- **去重**：`SET NX EX 1800` → key=`article:read:{articleId}:{userId}`（同一用户 30min 内不重复计数）
- **热榜 ZSET**：去重通过后 `ZINCRBY article:hot:7` / `article:hot:30`
- **异步持久化**：`ApplicationEventPublisher.publishEvent(ReadEvent)` → `@Async("readTaskExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)` → 写 `article_read` 表 + `UPDATE article SET read_count = read_count + 1`
- **热门查询**：`ZREVRANGE article:hot:{days}` 分页取 ID → `ArticleMapper.selectByIds` 批量查文章；Redis 不可用时降级 DB `COUNT(*) ... GROUP BY article_id`
- 去重和 ZSET 操作同步（不阻塞主响应），DB 写异步；Redis 不可用时跳过去重和 ZSET 更新

### 定时任务：HotArticleScheduler

- `@Scheduled(cron = "0 0 * * * ?")` 每小时整点执行
- 从 `article_read` 表聚合近 N 天数据（`countReadsByArticleInRange`）
- `ZADD` 全量重建 `article:hot:7` / `article:hot:30`，TTL 2h（校正实时 ZINCRBY 的偏差）
- Redis 不可用时静默跳过

### MyBatis-Plus 配置

- 分页插件：`PaginationInnerInterceptor(DbType.MYSQL)`
- 自动填充：`MyMetaObjectHandler` 在 insert/update 时自动设置 `createdAt`/`updatedAt`
- 下划线映射：`map-underscore-to-camel-case: true`
- SQL 日志：`StdOutImpl` 输出到控制台

## REST API 端点

所有接口统一返回 `Result<T>`（code + message + data）。用户身份通过请求头 `X-User-Id` 传入（默认值 1）。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/articles` | 创建文章 |
| POST | `/api/articles/batch` | 批量创建 |
| GET | `/api/articles?page=&size=&category=&status=&keyword=` | 分页查询 |
| GET | `/api/articles/{id}` | 查单篇（自动触发阅读记录） |
| PUT | `/api/articles/{id}` | 完整更新 |
| PATCH | `/api/articles/{id}` | 部分更新 |
| DELETE | `/api/articles/{id}` | 删除 |
| DELETE | `/api/articles?ids=1,2,3` | 批量删除 |
| GET | `/api/articles/statistics/category` | 分类文章数量统计 |
| POST | `/api/articles/{id}/like` | 点赞/取消点赞（toggle，Header: X-User-Id） |
| POST | `/api/articles/{id}/favorite` | 收藏/取消收藏（toggle，Header: X-User-Id） |
| GET | `/api/articles/{id}/stats` | 文章统计（likeCount, favoriteCount, readCount） |
| GET | `/api/articles/hot?days=7&page=1&size=10` | 热门文章排行 |

## Redis Key 设计总览

| Key | 类型 | 用途 | TTL |
|-----|------|------|-----|
| `article::{id}` | String (JSON) | 文章缓存 | 30 min |
| `categoryStats` | String (JSON) | 分类统计缓存 | 30 min |
| `article:like:{id}` | Set | 点赞用户集合 (userId) | 无 |
| `article:favorite:{id}` | Set | 收藏用户集合 (userId) | 无 |
| `article:read:{id}:{userId}` | String | 阅读去重标记 | 30 min |
| `article:hot:7` | ZSET | 7 天热榜 (articleId → readCount) | 2 h |
| `article:hot:30` | ZSET | 30 天热榜 (articleId → readCount) | 2 h |

## 数据库表

4 张表：`article`（含 like_count / favorite_count / read_count）、`article_like`、`article_favorite`、`article_read`。Schema 定义见 `src/main/resources/schema.sql`。

## 本地开发环境切换

Redis/RabbitMQ 已做降级处理，本地开发无需安装：

- **缓存**：`StringRedisTemplate` 为 null，所有 `getCache`/`setCache`/`deleteCache` 跳过，直接查库
- **MQ**：`RabbitTemplate` 为 null，`sendEvent()` 跳过

如需启用，修改 `application.yml`：
- 取消 `spring.data.redis` 注释（提供 Redis 连接信息）
- 删除 `spring.autoconfigure.exclude` 中的 RabbitMQ 排除项，设置 `app.rabbitmq-enabled: true`
