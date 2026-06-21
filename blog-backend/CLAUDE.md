# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

个人博客系统后端 API，支持文章管理、点赞收藏、阅读统计与热门排行、标签系统、评论系统、用户认证与权限管理。采用 Spring Boot + MyBatis-Plus + JWT 构建，**Redis 为必需依赖**（用于缓存、热榜、阅读去重、点赞/收藏状态）。

## 技术栈

- Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6
- 数据库：MySQL（生产）/ H2（测试）
- 缓存：Redis（StringRedisTemplate 手动控制），不再支持无 Redis 模式
- 消息队列：RabbitMQ（可选，默认排除自动配置）
- 构建：Maven

## 编译与运行

```bash
# 编译
mvn compile -f pom.xml

# 运行（需先创建 blogsystem 数据库，表结构自动建）
mvn spring-boot:run -f pom.xml

# 测试（使用 H2 内存库）
mvn test -f pom.xml -Dspring.profiles.active=test
```

> 首次运行：`mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS blogsystem DEFAULT CHARSET utf8mb4;"` 建库即可，`spring.sql.init.mode=always` 会自动执行 `schema.sql` 建表。`CREATE TABLE IF NOT EXISTS` 确保重复执行不会报错。

## 项目架构

```
com.blog
├── entity/          # 12 个实体：Article, ArticleLike, ArticleFavorite, ArticleRead, Tag, ArticleTag, ArticleHistory, Comment, CommentLike, User, Follow
├── mapper/          # 11 个 Mapper，继承 BaseMapper，自定义 SQL 用注解
├── service/         # 10 个 Service 接口
├── service/impl/    # 10 个 Service 实现，继承 ServiceImpl<Mapper, Entity>
├── controller/      # ArticleController（15 个端点）+ TagController（4 个端点）+ CommentController（6 个端点）+ AuthController（3 个端点）+ UserController（4 个端点）+ UploadController（1 个端点）
├── dto/             # Result<T>, ArticleQueryDTO, ToggleResult, HotArticleDTO, TagCloudItem, CommentDTO, LoginRequest, LoginResponse, UserDTO
├── config/          # MyBatisPlusConfig, RabbitMQConfig(条件), AsyncConfig, AuthInterceptor, WebMvcConfig
├── handler/         # MyMetaObjectHandler, GlobalExceptionHandler
├── event/           # ArticleEventListener(MQ), ArticleReadEventListener(@Async), ReadEvent
├── task/            # HotArticleScheduler, TagStatsScheduler, HistoryCleanupScheduler
└── util/            # JwtUtil, AuthContext (ThreadLocal)
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
- **异步持久化**：`ApplicationEventPublisher.publishEvent(ReadEvent)` → `@Async("readTaskExecutor")` + `@EventListener` → 写 `article_read` 表 + `UPDATE article SET read_count = read_count + 1`
- **热门查询**：`ZREVRANGE article:hot:{days}` 分页取 ID → `ArticleMapper.selectByIds` 批量查文章；Redis 不可用时降级 DB `COUNT(*) ... GROUP BY article_id`
- 去重和 ZSET 操作在主请求中同步完成，DB 写入由异步线程池 `readTaskExecutor`（core=5, max=10）处理
- **降级行为**：Redis 不可用时，去重跳过（每次访问都计数），ZSET 热榜不更新，热门查询自动降级为 DB 聚合

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

## 鉴权规则（2026-06-16 更新）

| 请求方法 | 鉴权要求 | 说明 |
|---------|---------|------|
| GET | **可选** | 携带有效 token 则识别用户身份，无 token 以匿名访问 |
| POST/PUT/PATCH/DELETE | **强制** | 必须携带有效 JWT，否则返回 401 |
| `/api/auth/**` | 无需 | 注册、登录、登出接口免鉴权 |

### 权限模型（Service 层校验）
| 操作 | 权限 |
|------|------|
| 删除文章 | 作者本人 或 管理员（`AuthContext.isAdmin()`） |
| 设置文章标签 | 作者本人 或 管理员（不再仅限管理员） |
| 创建/编辑/删除标签 | 仅管理员 |
| 管理评论 | 仅管理员 |

### CORS 配置
- `WebMvcConfig.addCorsMappings()` 允许所有来源跨域访问 `/api/**`
- 支持 GET/POST/PUT/PATCH/DELETE/OPTIONS 方法
- 开发环境前端 `localhost:3000` → 后端 `localhost:8080`

## 文章删除（级联清理）

`ArticleServiceImpl.removeById()` 删除文章时**完整清理所有关联数据**：

1. 删除 article_image 记录 + 服务器图片文件
2. 删除 article_tag 关联 + 更新 tag.article_count
3. 删除 article_like 全部记录
4. 删除 article_favorite 全部记录
5. 删除 article_read 全部记录
6. 删除 comment_like（通过 commentId）+ 删除 comment
7. 删除 article_history 全部记录
8. 删除 article 本身
9. 清理 Redis：article 缓存、like/favorite Set、hot ZSET、categoryStats、tag cloud

新增 Mapper 方法：`ArticleImageMapper.deleteByArticleId`/`selectByArticleId`、`ArticleLikeMapper.deleteByArticleId`、`ArticleFavoriteMapper.deleteByArticleId`、`CommentMapper.deleteByArticleId`/`selectIdsByArticleId`、`CommentLikeMapper.deleteByCommentIds`

## 查询参数

`ArticleQueryDTO` 支持以下筛选参数：
- `page`, `size`：分页（默认 1/10）
- `category`, `status`, `keyword`：分类/状态/关键词过滤
- `tagId`：按标签筛选（JOIN article_tag 表）
- `authorId`：按作者筛选（用于个人主页）

## 标签系统变更

- `ArticleTagServiceImpl.setTags()`：权限放宽为「作者本人或管理员」
- `TagServiceImpl.getCloud()`：**直接查库，不使用缓存**（保证数据实时准确）
- `HotArticleScheduler`：新增 `@PostConstruct` 启动时全量重建 ZSET
- `ArticleReadEventListener`：异步写 DB 后删除文章缓存，保证阅读数及时更新

## 评论系统变更

- `CommentDTO` 新增 `liked` 字段，`toDTO()` 自动判断当前用户是否已点赞该评论

## REST API 端点

所有接口统一返回 `Result<T>`（code + message + data）。

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
| POST | `/api/articles/{id}/like` | 点赞/取消点赞（toggle） |
| POST | `/api/articles/{id}/favorite` | 收藏/取消收藏（toggle） |
| GET | `/api/articles/{id}/stats` | 文章统计（likeCount, favoriteCount, readCount） |
| GET | `/api/articles/hot?days=7&page=1&size=10` | 热门文章排行 |
| GET | `/api/articles?tagId=xxx` | 按标签筛选文章 |
| PUT | `/api/articles/{id}/tags` | 设置文章标签（管理员） |
| GET | `/api/articles/{id}/tags` | 查看文章标签 |
| POST | `/api/tags` | 创建标签（管理员） |
| PUT | `/api/tags/{id}` | 更新标签（管理员） |
| DELETE | `/api/tags/{id}` | 删除标签（管理员） |
| GET | `/api/tags/cloud?sort=count\|hot` | 标签云 |
| GET | `/api/articles/{id}/history?page=&size=` | 版本历史列表 |
| GET | `/api/articles/{id}/history/{historyId}` | 版本详情 |
| POST | `/api/articles/{id}/rollback/{historyId}` | 回滚到指定版本（管理员） |
| POST | `/api/comments` | 发表评论 |
| GET | `/api/articles/{id}/comments?sort=time\|like` | 顶级评论（含 3 条子回复） |
| GET | `/api/comments/{id}/replies` | 子回复列表 |
| POST | `/api/comments/{id}/like` | 评论点赞 toggle |
| DELETE | `/api/comments/{id}` | 删除评论（本人软删/管理员硬删） |
| PUT | `/api/comments/{id}/hide` | 管理员隐藏评论 |
| POST | `/api/auth/register` | 注册（可选 nickname, avatar） |
| POST | `/api/auth/login` | 登录，返回 JWT + nickname + avatar |
| POST | `/api/auth/logout` | 登出，token 加入黑名单 |
| GET | `/api/users/{id}` | 用户公开资料（含关注状态和计数） |
| POST | `/api/users/{id}/follow` | 关注/取消关注 toggle |
| GET | `/api/users/{id}/followers?page=&size=` | 粉丝列表 |
| GET | `/api/users/{id}/following?page=&size=` | 关注列表 |
| POST | `/api/upload/avatar` | 上传头像（免鉴权，max 2MB） |
| POST | `/api/upload/article-image` | 上传文章配图（免鉴权，max 5MB） |

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
| `tag::{id}` | String (JSON) | 标签缓存 | 30 min |
| `tag::cloud:sort=count` | String (JSON) | 标签云（按文章数排序） | 5 min |
| `tag::cloud:sort=hot` | String (JSON) | 标签云（按热度排序） | 5 min |

## 标签系统

### 数据模型

- `tag`：标签主表，含 `name`（UNIQUE）、`article_count`（关联文章数）、`hot_score`（关联文章总阅读量）
- `article_tag`：多对多中间表，UNIQUE(article_id, tag_id)，防止重复关联

### article_count 维护

- **实时维护**：`ArticleTagServiceImpl.setTags()` 中通过原子 SQL（`UPDATE tag SET article_count = article_count +/- 1`）维护
- **定时校正**：`TagStatsScheduler` 每 6 小时从 `article_tag` 表重算真实数量并修正偏差
- **删除标签**：Controller 先调 `articleTagService.deleteByTagId()` 清理关联，再删 tag 本身

### hot_score 计算

- 查询时通过 LEFT JOIN `article_tag` + `article` 实时计算：`SUM(article.read_count) GROUP BY tag.id`
- 标签云缓存 5 分钟 TTL，避免高频查询
- 写操作（文章增删改、标签关联变更）主动失效标签云缓存

### 管理员校验

- 项目通过 JWT 拦截器 + ThreadLocal（`AuthContext`）管理用户身份
- 管理员通过 `AuthContext.isAdmin()` 判断（role=admin）
- 校验在 Service 层完成，抛出 `RuntimeException("Admin access required")`

### 限制

- 每篇文章最多 5 个标签，在 `ArticleTagServiceImpl.setTags()` 中校验
- 标签名全局唯一，通过 `tag.name` UNIQUE 约束 + Service 层查重保证

## 用户认证

### JWT 认证流程
- `/api/auth/**` 免认证，其余 `/api/**` 均需携带 `Authorization: Bearer <token>`
- `AuthInterceptor` 拦截请求，解析 JWT 后将 userId/username/role 存入 `AuthContext`（ThreadLocal）
- Controller 和 Service 通过 `AuthContext.getUserId()` / `AuthContext.isAdmin()` 获取当前用户
- Token 过期时间默认 24 小时（`app.jwt.expiration` 配置）

### 登出与黑名单
- 登出时将 token 加入 Redis 黑名单（key: `token:blacklist:<token>`，TTL 与 token 有效期一致）
- 拦截器每次校验 token 是否在黑名单中
- Redis 不可用时跳过黑名单校验（降级）

### 权限判断
- 旧：`@RequestHeader("X-User-Id")` + `userId == 1`
- 新：`AuthContext.getUserId()` + `AuthContext.isAdmin()`

### 文章所有权
- 创建文章时自动设置 `author_id` 为当前用户
- 编辑/删除文章：作者本人或管理员可操作

## 用户资料与关注系统（2026-06-18 新增）

### 用户表扩展

- `user` 表新增 `nickname VARCHAR(50)` 和 `avatar VARCHAR(500)` 字段
- 注册接口 `/api/auth/register` 可选接收 `nickname` 和 `avatar` 参数
- 登录接口 `/api/auth/login` 返回 `nickname` 和 `avatar`（连同 token、userId、username、role）

### 关注系统

**数据表：** `user_follow` — `follower_id` + `following_id`，UNIQUE(follower_id, following_id)，双向索引

**端点（`UserController`）：**

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/{id}` | 用户公开信息（含 followerCount, followingCount, articleCount, followed） |
| POST | `/api/users/{id}/follow` | 关注/取消关注 toggle，返回 `{liked, count}` |
| GET | `/api/users/{id}/followers?page=&size=` | 粉丝列表（分页） |
| GET | `/api/users/{id}/following?page=&size=` | 关注列表（分页） |

**关注逻辑：**
- Toggle 模式：已关注则删除记录 → `followed=false`，未关注则插入记录 → `followed=true`
- `FollowServiceImpl.toggle()` 中先查是否已关注（`selectByFollowerAndFollowing`），再决定 INSERT 或 DELETE
- 返回当前最新 followerCount（`countFollowers`）
- `UserDTO.followed` 字段仅对已登录用户有意义：根据当前登录者是否关注了该用户来填充

### 头像上传

**端点：** `POST /api/upload/avatar`（免鉴权，已加入 `excludePathPatterns`）

**校验规则：**
- 文件非空
- Content-Type 必须为 `image/*`
- 大小不超过 2MB（配合 `spring.servlet.multipart.max-file-size`）

**文件存储：**
- 保存目录：`{app.upload.dir}/avatars/`（`app.upload.dir` 使用绝对路径 `D:/vibecoding1/uploads`，避免 Tomcat temp 目录问题）
- 文件名：`avatar_{UUID前8位}{原扩展名}`
- 返回：`{"url": "/uploads/avatars/avatar_xxx.png"}`

**静态资源映射：**
- `WebMvcConfig.addResourceHandlers()`：`/uploads/**` → `file:{uploadDir}/`
- 前端通过 `http://localhost:8080` + 相对路径访问头像图片

### 评论用户信息

- `CommentDTO` 新增 `userNickname`、`userAvatar` 字段
- `CommentServiceImpl.toDTO()` 接收 `Map<Long, User>`，根据 `comment.userId` 查找填充
- `batchLoadUsers(List<Long> userIds)` 使用 `userMapper.selectBatchIds()` 批量查询，避免 N+1
- 顶级评论和子回复均传递 userMap

## 数据库表

10 张表 + 1 张关注表，Schema 定义见 `src/main/resources/schema.sql`，应用启动时自动执行：

| 表 | 用途 | 关键字段/约束 |
|----|------|-------------|
| `article` | 文章主表 | like_count, favorite_count, read_count 为冗余计数器 |
| `article_like` | 用户-文章点赞关联 | UNIQUE(user_id, article_id) |
| `article_favorite` | 用户-文章收藏关联 | UNIQUE(user_id, article_id) |
| `article_read` | 阅读记录 | INDEX(article_id, created_at), INDEX(created_at) |
| `tag` | 标签主表 | UNIQUE(name), article_count, hot_score |
| `article_tag` | 文章-标签多对多 | UNIQUE(article_id, tag_id) |
| `article_history` | 文章版本历史 | INDEX(article_id, version_no), change_type |
| `article_image` | 文章配图 | INDEX(article_id), url + sort_order 排序 |
| `comment` | 评论 | parent_id/reply_to 嵌套，status 状态控制，INDEX(article_id, parent_id) |
| `comment_like` | 评论点赞 | UNIQUE(comment_id, user_id) |
| `user` | 用户表 | UNIQUE(username), nickname, avatar, role 区分 admin/user |
| `user_follow` | 用户关注 | UNIQUE(follower_id, following_id), INDEX(follower), INDEX(following) |

## 文章版本历史

### 触发条件

- 文章状态为 `published` 且 title/content/category 任一变化时，自动保存历史快照
- 草稿（draft）状态不生成历史
- PUT 和 PATCH 均触发同一检查逻辑（封装在 `saveHistoryIfNeeded` 私有方法中）
- 仅改 status（draft→published）不记录历史

### 版本策略

- 每篇文章独立递增版本号（version_no）
- 最多保留 20 个版本，超过后自动删除最旧版本
- `change_type`：`UPDATE`（普通编辑）或 `ROLLBACK`（回滚操作）

### 回滚

- 将当前文章内容覆盖为目标版本内容
- 回滚前自动保存当前状态为一条新历史（change_type=ROLLBACK）
- 需管理员权限（role=admin）

### 文章列表

- `GET /api/articles` 默认只返回 `status=published` 的文章
- 可通过 `?status=draft` 显式查询草稿

## 评论系统

### 数据结构
- `parent_id`：父评论 ID，NULL 为顶级评论
- `reply_to`：被回复的评论 ID
- `status`：visible（正常）/ hidden（管理员隐藏）/ deleted（用户自行删除）

### 嵌套加载
- 顶级评论分页查询时，每条预加载最新 3 条子回复（`replyCount` 表示总回复数）
- 更多子回复通过 GET /api/comments/{id}/replies 懒加载

### 删除策略
- 评论作者：status → 'deleted'，内容保留，子回复不受影响
- 管理员（userId=1）：递归硬删除该评论及所有子孙回复
- 管理员标记违规：status → 'hidden'，不对外展示

### 敏感词
- 配置在 application.yml 的 `app.sensitive-words`
- 发布时 content 命中任一敏感词则拒绝（400）

### 计数维护
- article 表 comment_count 原子更新
- 新增/删除时实时维护

## 本地开发环境切换

Redis/RabbitMQ 已做降级处理，本地开发无需安装，直接启动即可：

- **缓存（Redis）**：若未配置 Redis 连接，`StringRedisTemplate` 为 null，所有 `getCache`/`setCache`/`deleteCache` 跳过，直接查库。若 Redis 依赖在 classpath 上但服务未运行，连接异常被 catch 后降级
- **阅读去重**：Redis 不可用时，每次访问都计为新阅读（去重失效但不影响功能）
- **热榜 ZSET**：Redis 不可用时 ZSET 不更新，热门查询自动降级为查 `article_read` 表聚合
- **MQ**：`RabbitTemplate` 为 null，`sendEvent()` 跳过

如需启用，修改 `application.yml`：
- 取消 `spring.data.redis` 注释（提供 Redis 连接信息）
- 删除 `spring.autoconfigure.exclude` 中的 RabbitMQ 排除项，设置 `app.rabbitmq-enabled: true`
