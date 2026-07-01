# jaravel-demo v0.1.0

展示 jaravel 全部能力的 Laravel 风格 SpringBoot 示例项目。引入 `starter` 依赖即可获得 Laravel 全部风格功能，本项目演示了路由、ORM、认证、缓存、事件、Artisan 命令、定时任务、队列、微信 SDK 和插件系统等完整能力。

## 核心特性

- **Eloquent 合并 Model**：单一类同时承担实体定义与查询能力（`new User().save()` / `User.find(1L)` / `User.all()`），无需拆分 Entity 和 Model
- **Laravel 风格目录结构**：`config/`、`routes/`、`database/` 与 `app/` 同级，对齐 Laravel 项目结构
- **路由系统**：`routes/Api.java` 和 `routes/Web.java` 分别定义 API 和 Web 路由，由 `RouteServiceProvider` 加载
- **全局中间件**：`RouteServiceProvider.boot()` 注册系统中间件（Spring `@Component` 无状态单例），对齐 Laravel 的 `RouteServiceProvider`
- **多 Guard / 多 Provider 认证**：支持 JWT 和 Session 两种 Guard 驱动，可注册多个 Provider；Auth 以主键比对，密码校验在应用层
- **JWT 续期与登出**：Token 自动续期（可选，默认启用）、登出黑名单（基于 Cache，支持 array/file 存储）
- **多数据库支持**：`@DataSource` 注解指定 Model 使用的数据源，支持多 SQLite/MySQL 数据库
- **事件系统**：`EventDispatcher` 支持 per-listener 队列（`ShouldQueue` 接口），多命名队列独立线程池，失败自动重试
- **Artisan CLI**：Laravel 风格命令行工具，支持参数解析、签名定义，通过 `--artisan` 参数运行
- **代码生成（make:xxx）**：`make:controller`/`make:model`/`make:migration` 等 8 个命令一键生成代码骨架，`make:all` 一键生成全套
- **P2SP 树形拓扑**：远程插件执行支持树形中继转发，节点本地无插件时自动转发给子节点，三重防环机制
- **定时任务**：Cron 表达式、固定间隔任务，支持 Redis 分布式锁，多实例防重复执行
- **队列任务**：异步监听器（ShouldQueue）、数据库队列持久化、自动重试机制
- **微信 SDK**：公众号与小程序 API 封装，access_token 自动管理与缓存
- **Request 全格式支持**：自动解析 JSON、form-urlencoded、multipart 文件上传，Header 大小写不敏感、多值自动转数组
- **迁移系统**：Laravel 风格的 Schema Builder，支持 MySQL/SQLite/H2/SQL Server 多数据库迁移，一次 up() 可处理多张表
- **缓存系统**：Array/File/Redis 多驱动，支持 put/get/has/increment/remember/forget
- **Blade 模板引擎**：`@if`/`@foreach`/`@extends`/`@yield` 等指令，对齐 Laravel Blade
- **插件系统**：JAR 插件与 Java 文件插件，支持动态加载、卸载和热更新
- **验证码**：图片数字、算术、滑动、旋转四种验证码，滑动/旋转支持轨迹行为分析防自动化，可配置水印与自定义背景

## 目录结构

```
jaravel/
├── src/main/java/com/weacsoft/jaravel/
│   ├── JaravelApplication.java          # 应用入口
│   ├── config/                          # 配置（对齐 config/，与 app/ 同级）
│   │   ├── App.java                     # 引导配置（全局中间件已移至 RouteServiceProvider.boot()）
│   │   ├── Database.java                # 数据库配置（多数据源）
│   │   ├── View.java                    # 视图配置
│   │   └── WechatConfig.java            # 微信 SDK 配置
│   ├── routes/                          # 路由（对齐 routes/，与 app/ 同级）
│   │   ├── Api.java                     # API 路由（对齐 routes/api.php）
│   │   └── Web.java                     # Web 路由 + 文档页面路由（对齐 routes/web.php）
│   ├── database/                        # 数据库迁移（对齐 database/，与 app/ 同级）
│   │   └── migration/
│   │       ├── Migration_2024_01_01_CreateUsersTable.java    # 用户表迁移（多表）
│   │       └── Migration_2024_01_02_CreateProductsTable.java # 产品表迁移（多表+第二数据源）
│   └── app/                             # 应用代码（对齐 app/）
│       ├── model/                       # 数据模型
│       │   ├── User.java                # 用户模型（主数据源）
│       │   └── Product.java             # 产品模型（@DataSource 第二数据源）
│       ├── service/                     # 业务服务
│       │   └── UserService.java         # 用户服务（含事件分发）
│       ├── console/                     # 命令行与定时任务（对齐 app/Console/）
│       │   ├── HelloCommand.java        # Artisan 命令：hello
│       │   ├── CreateUserCommand.java   # Artisan 命令：user:create
│       │   └── ScheduleConfig.java       # 定时任务配置（缓存清理 + 日报）
│       ├── job/                         # 队列任务（对齐 app/Jobs/）
│       │   ├── NotificationEvent.java   # 通知事件
│       │   └── SendNotificationJob.java # 发送通知队列任务（实现 ShouldQueue）
│       ├── http/                        # HTTP 层
│       │   ├── controller/              # 控制器
│       │   │   ├── AuthController.java
│       │   │   ├── UserController.java
│       │   │   ├── WelcomeController.java
│       │   │   ├── PluginDemoController.java
│       │   │   └── CaptchaController.java
│       │   └── middleware/              # 自定义中间件
│       │       └── OrderTestMiddleware.java
│       └── provider/                    # 服务提供者
│           ├── AuthServiceProvider.java # 认证配置（注册 Guard）
│           ├── AppEventServiceProvider.java # 事件监听器注册
│           └── RouteServiceProvider.java    # 路由加载
├── src/main/resources/
│   ├── application.yml                  # 应用配置
│   └── templates/                       # Blade 模板（.blade.java 后缀，IDE 可识别）
│       ├── welcome.blade.java           # 欢迎页模板
│       └── docs/                        # 纯前端文档包（jblade 模板）
│           ├── layout.blade.java        # 文档布局模板（侧边栏 + 内容区）
│           ├── index.blade.java         # 文档首页
│           ├── installation.blade.java  # 安装指南
│           ├── routing.blade.java       # 路由
│           ├── eloquent.blade.java      # Eloquent ORM
│           ├── auth.blade.java          # 认证
│           ├── cache.blade.java         # 缓存
│           ├── events.blade.java        # 事件系统
│           ├── artisan.blade.java       # Artisan CLI
│           ├── schedule.blade.java      # 定时任务
│           ├── queue.blade.java         # 队列
│           └── plugins.blade.java       # 插件系统
├── plugins-java/                        # Java 文件插件目录
│   └── demo-greeting/GreetingPlugin.java
└── pom.xml
```

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+

### 1. 构建 jaravel-vendor

```bash
cd jaravel-vendor
mvn install -DskipTests
```

### 2. 运行 jaravel 模板项目

```bash
cd jaravel
mvn spring-boot:run
```

应用启动后自动执行：
1. **数据库迁移**：创建 `users` 表（主数据库）和 `products` 表（第二数据库）
2. **启动 Web 服务**：监听 `http://localhost:8080`

### 3. 验证 API

```bash
# Hello World
curl http://localhost:8080/api/hello

# 注册（触发 UserRegisteredEvent 事件，返回 token + refresh_token）
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"alice","number":"alice001","password":"secret123","email":"alice@test.com"}'

# 登录（返回 token + refresh_token）
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"number":"alice001","password":"secret123"}'

# 用户列表（需 Bearer token）
curl http://localhost:8080/api/users \
  -H "Authorization: Bearer <token>"

# 获取当前用户信息（含自动续期的新 token）
curl http://localhost:8080/api/auth/profile \
  -H "Authorization: Bearer <token>"

# JWT token 刷新（用 refresh_token 换取新 access token）
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refresh_token":"<refresh_token>"}'

# 登出（JWT guard 将 token 加入黑名单）
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer <token>"

# 多 Guard 测试（检查指定 guard 登录态）
curl http://localhost:8080/api/guard/api \
  -H "Authorization: Bearer <token>"

# 多数据库测试（Product 使用第二数据源）
curl http://localhost:8080/api/products

# 中间件顺序测试（洋葱模型）
curl http://localhost:8080/api/middleware-test

# Cache 功能演示（put/get/has/increment/remember/forget）
curl http://localhost:8080/api/cache-demo

# View (Blade模板) 演示
curl http://localhost:8080/api/view-demo

# Artisan 命令演示（列出命令并执行 hello）
curl http://localhost:8080/api/artisan/demo

# 定时任务状态查看
curl http://localhost:8080/api/schedule/status

# 队列任务演示（分发通知事件，异步执行）
curl http://localhost:8080/api/queue/demo

# 队列任务演示（自定义参数）
curl "http://localhost:8080/api/queue/demo?type=sms&recipient=13800138000&content=验证码123456"

# 微信 access_token 演示
curl http://localhost:8080/api/wechat/token

# 验证码：查看支持的类型
curl http://localhost:8080/api/captcha/types

# 验证码：生成数字验证码（默认）
curl http://localhost:8080/api/captcha/generate

# 验证码：生成算术验证码
curl "http://localhost:8080/api/captcha/generate?type=arithmetic"

# 验证码：生成滑动验证码
curl "http://localhost:8080/api/captcha/generate?type=slider"

# 验证码：生成旋转验证码
curl "http://localhost:8080/api/captcha/generate?type=rotate"

# 验证码：校验用户输入（数字/算术）
curl -X POST http://localhost:8080/api/captcha/verify \
  -H "Content-Type: application/json" \
  -d '{"captchaKey":"<captchaKey>","input":"AB23","type":"number"}'
```

### 4. 访问文档页面

应用启动后，浏览器打开 http://localhost:8080/docs 即可访问项目文档。

文档页面列表：

| 路径 | 说明 |
|------|------|
| `/docs` | 文档首页（项目介绍、特性列表、快速开始） |
| `/docs/installation` | 安装指南（前置要求、构建步骤、运行方法） |
| `/docs/routing` | 路由（路由定义、分组、中间件、RESTful 示例） |
| `/docs/eloquent` | Eloquent ORM（Model 定义、CRUD、查询构造器、多数据源） |
| `/docs/auth` | 认证（多 Guard/Provider、JWT、Session、中间件） |
| `/docs/cache` | 缓存（基本操作、remember、驱动配置） |
| `/docs/events` | 事件系统（事件定义、监听器、异步队列、重试） |
| `/docs/artisan` | Artisan CLI（命令定义、签名解析、运行方式） |
| `/docs/schedule` | 定时任务（任务注册、Cron 表达式、Redis 分布式锁） |
| `/docs/queue` | 队列（异步监听器、数据库队列、重试机制） |
| `/docs/plugins` | 插件系统（JAR 插件、Java 文件插件、热更新） |

### 5. 运行 Artisan 命令

```bash
# 列出所有可用命令
java -jar target/jaravel-demo-0.1.0.jar --artisan list

# 执行 hello 命令
java -jar target/jaravel-demo-0.1.0.jar --artisan hello
java -jar target/jaravel-demo-0.1.0.jar --artisan hello Alice

# 创建用户
java -jar target/jaravel-demo-0.1.0.jar --artisan user:create 1001 Alice
java -jar target/jaravel-demo-0.1.0.jar --artisan user:create 1001 Alice --email=alice@test.com
```

## Eloquent Model（合并模式）

单一类同时承担实体定义与查询能力，对齐 Laravel Eloquent：

```java
@Data
@Repository
@Table(name = "users")
public class User extends BaseModel<User, Long> implements Authenticatable {

    @Primary @Column(name = "id")   private Long id;
    @Column(name = "name")          private String name;
    @Column(name = "number")        private String number;
    @Column(name = "password")      private String password;
    @Column(name = "email")         private String email;

    // 静态查询方法（委托给 BaseModel）
    public static User find(Long id)                  { return BaseModel.find(User.class, id); }
    public static List<User> all()                    { return BaseModel.all(User.class); }
    public static QueryBuilder<User, Long> query()    { return BaseModel.query(User.class); }
}
```

### 多数据库 Model

通过 `@DataSource` 注解指定 Model 使用的数据源 Bean 名称，对齐 Laravel Model 的 `$connection` 属性：

```java
@Data
@Repository
@DataSource("secondaryGaarasonDataSource")  // 使用第二数据源
@Table(name = "products")
public class Product extends BaseModel<Product, Long> {
    @Primary @Column(name = "id")   private Long id;
    @Column(name = "name")          private String name;
    @Column(name = "price")         private Double price;

    public static Product find(Long id) { return BaseModel.find(Product.class, id); }
    public static List<Product> all()   { return BaseModel.all(Product.class); }
}
```

## 路由系统

`routes/Api.java` 和 `routes/Web.java` 定义路由，由 `RouteServiceProvider` 加载，对齐 Laravel 的 `routes/api.php` 和 `routes/web.php`：

```java
@Component
public class Api {
    public void register(Router router, ApplicationContext context) {
        AuthController auth = context.getBean(AuthController.class);

        router.group(Map.of(Route.Group.PREFIX, "api"), api -> {
            // 公开路由
            api.get("/hello", welcomeController::hello);
            api.post("/auth/register", auth::register);

            // 需要认证的路由（默认 Guard）
            api.group(Map.of(), authGroup -> {
                authGroup.get("/auth/me", auth::me);
                authGroup.get("/users", userController::list);
            }).middleware(new Authenticate());

            // 指定 Guard 的路由（对齐 Laravel auth:web）
            api.get("/guard/api", handler).middleware(new Authenticate("api"));
            api.get("/guard/web", handler).middleware(new Authenticate("web"));

            // 中间件链（洋葱模型）
            api.get("/middleware-test", handler)
               .middleware(new OrderTestMiddleware("A"))
               .middleware(new OrderTestMiddleware("B"))
               .middleware(new OrderTestMiddleware("C"));
        });
    }
}
```

## 全局中间件

系统中间件已标注 `@Component`，由 Spring 容器管理为无状态单例。在 `RouteServiceProvider.boot()` 中统一注册（对齐 Laravel 在 RouteServiceProvider 注册系统中间件的做法）：

```java
@Configuration
public class RouteServiceProvider extends ServiceProvider {

    @Autowired
    private GlobalMiddlewareRegistry globalMiddlewareRegistry;

    @Override
    public void boot() {
        // 系统全局中间件（从 Spring 容器获取无状态单例 Bean）
        globalMiddlewareRegistry.addByType(TrimStrings.class);
        globalMiddlewareRegistry.addByType(ConvertEmptyStringsToNull.class);
    }
}
```

中间件使用规范：
- **系统全局中间件**（`TrimStrings`、`ConvertEmptyStringsToNull` 等）：标注 `@Component`，在 `RouteServiceProvider.boot()` 中通过 `addByType()` 注册
- **需要构造参数的中间件**（如 `new Authenticate("api")`、`new OrderTestMiddleware("A")`）：直接使用 `new` 创建，它们不可变、无状态，可安全并发复用
- **用户自定义无参数中间件**：标注 `@Component` 后通过 `context.getBean(XxxMiddleware.class)` 从 Spring 容器获取

内置中间件：
- `TrimStrings` - 去除输入字符串前后空白
- `ConvertEmptyStringsToNull` - 空字符串转 null（排除密码字段）
- `Authenticate` - 认证中间件，支持指定 Guard 名称

## 事件系统

### 定义事件和监听器

```java
// 事件
public class UserRegisteredEvent implements Event {
    private final Long userId;
    private final String name;
    // constructor, getters...
}

// 同步监听器
public class LogRegistrationListener implements Listener<UserRegisteredEvent> {
    @Override
    public void handle(UserRegisteredEvent event) {
        log.info("用户注册: {}", event.getName());
    }
}

// 异步监听器（实现 ShouldQueue 自动异步执行）
public class SendWelcomeEmailListener implements Listener<UserRegisteredEvent>, ShouldQueue {
    @Override
    public void handle(UserRegisteredEvent event) {
        log.info("发送欢迎邮件: {}", event.getName());
    }

    @Override
    public String queue() { return "email"; }  // 队列名称
}
```

### 注册监听器

```java
@Component
public class AppEventServiceProvider extends EventServiceProvider {
    @Override
    public void register() {
        listen(UserRegisteredEvent.class, new LogRegistrationListener());
        listen(UserRegisteredEvent.class, new SendWelcomeEmailListener());
    }
}
```

### 分发事件

```java
Dispatcher dispatcher = SpringContext.bean(Dispatcher.class);
dispatcher.dispatch(new UserRegisteredEvent(user.getId(), user.getName()));
```

### 队列配置

实现 `ShouldQueue` 的监听器将被异步分发到命名队列执行，每个队列拥有独立线程池，互不阻塞。
监听器执行失败时支持自动重试（可配置次数与间隔）。

```yaml
jaravel:
  event:
    queue-enabled: true                    # 启用异步队列分发
    queue:
      default:
        pool-size: 4                       # 默认队列线程池大小（默认 CPU 核心数）
      email:
        pool-size: 2                       # "email" 队列线程池大小覆盖
    retry:
      max-attempts: 3                      # 最大重试次数（默认 3）
      delay-ms: 1000                       # 重试间隔毫秒（默认 1000）
```

## Request 全格式支持

Request 自动解析所有常见格式，无需修改代码：

```java
// JSON body -> request.input("name")
// form-urlencoded -> request.input("name")
// multipart file -> request.file("file")

public Response requestTest(Request request) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("all_inputs", request.all());        // 所有输入
    result.put("headers", request.header());         // 所有请求头
    result.put("has_file", request.hasFile("file")); // 是否有文件
    if (request.hasFile("file")) {
        result.put("file_name", request.file("file").getOriginalFilename());
    }
    return ResponseBuilder.json(result);
}
```

特性：
- **Header 大小写不敏感**：`X-Custom-Header` 和 `x-custom-header` 视为同一个
- **Header 多值自动转数组**：同名 Header 多次提交自动合并为数组
- **Attributes**：`request.setAttribute(key, value)` / `request.getAttribute(key, Class)` 用于中间件间传递数据
- **路径参数**：`request.routeParam("id", Long.class)`
- **查询参数**：`request.query("filter")`
- **输入参数**：`request.input("name")`（自动从 JSON body 或 form 中解析）

## 多数据库配置

`config/Database.java` 配置多数据源，共享同一个 gaarason 容器：

```java
@Configuration
public class Database {
    @Bean @Primary
    public GaarasonDataSource gaarasonDataSource(Environment env, ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.url", "jdbc:sqlite:database1.sqlite"));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }

    @Bean("secondaryGaarasonDataSource")
    public GaarasonDataSource secondaryGaarasonDataSource(Environment env, ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.secondary.url", "jdbc:sqlite:database3.sqlite"));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }
}
```

迁移类通过 `getDataSourceName()` 指定使用哪个数据源：

```java
@Component
public class Migration_2024_01_02_CreateProductsTable implements Migration {
    @Override
    public String getDataSourceName() { return "secondaryGaarasonDataSource"; }

    @Override
    public void up(Schema schema) {
        schema.create("product_categories", table -> {
            table.id();
            table.string("name", 100);
            table.string("code", 50).unique();
            table.timestamps();
        });
        schema.create("products", table -> {
            table.id();
            table.string("name");
            table.doubleColumn("price");
            table.timestamps();
        });
    }
}
```

## 认证

### 多 Provider / 多 Guard

Auth 以主键比对登录态，密码校验在应用层（Service/Controller）完成，`Authenticatable` 契约不再包含 `getAuthPassword()`。

```java
@Component
public class AuthServiceProvider extends ServiceProvider {
    @Override
    public void register() {
        AuthManager authManager = Facade.resolve(AuthManager.class);
        User userModel = Facade.resolve(User.class);

        // 注册用户提供者（provider）：仅负责按主键/凭证取出用户，不校验密码
        // 凭证字段 "number" 用于 retrieveByCredentials（按工号查询）
        authManager.registerProvider("users",
            new EloquentUserProvider<>(userModel, "number"));

        // 如有 Admin 等其他用户模型，可注册更多 provider：
        // authManager.registerProvider("admins",
        //     new EloquentUserProvider<>(adminModel, "username"));

        // 注册守卫（guard）：一个 guard 绑定一个 provider
        authManager.registerGuard("api", "jwt", "users");      // JWT 驱动（API 场景，无状态）
        authManager.registerGuard("web", "session", "users");  // Session 驱动（Web 场景，有状态）
        // authManager.registerGuard("admin", "jwt", "admins"); // 管理后台

        authManager.setDefaultGuard("api");
    }
}
```

### 认证流程

```java
// 1. 应用层查询用户 + 校验密码（Service 层责任）
User user = UserService.login(number, password);

// 2. Auth 以主键登入指定 guard（不涉及密码）
Auth.login(user);                    // 登入默认 guard（api）
Auth.login(user, "web");             // 登入指定 guard

// 3. 检查登录态（以主键比对）
Auth.check();                        // 检查默认 guard
Auth.guard("api").check();           // 检查指定 guard

// 4. 获取当前用户
Auth.user();                         // 默认 guard 的当前用户
Auth.guard("api").user();            // 指定 guard 的当前用户

// 5. 登出
Auth.logout();                       // 登出默认 guard（JWT 会将 token 加入黑名单）
Auth.logout("api");                  // 登出指定 guard
```

### Authenticate 中间件

```java
// 默认 Guard
router.get("/profile", handler).middleware(new Authenticate());

// 指定 Guard（对齐 Laravel auth:api）
router.get("/api/profile", handler).middleware(new Authenticate("api"));
router.get("/admin", handler).middleware(new Authenticate("web"));
```

## JWT 认证

JWT 模块提供 token 自动续期与登出黑名单功能，对齐 Laravel jwt-auth 插件。

### 配置

```yaml
jaravel:
  jwt:
    secret: your-secret-key-must-be-32-bytes
    ttl: 3600000              # access token 有效期（毫秒，默认 1 小时）
    refresh-ttl: 604800000    # refresh token 有效期（毫秒，默认 7 天）
    refresh-enabled: true     # token 自动续期（默认启用，可禁用）
    blacklist-store: array    # 登出黑名单缓存（array 内存 / file 文件）
    blacklist-prefix: "jwt:blacklist:"
```

### Token 续期

当 `refresh-enabled=true`（默认）时，每次携带 access token 请求且 token 已过半 TTL，
`JwtGuard.user()` 会自动签发新 token，通过 `guard.token()` 获取：

```java
AuthGuard guard = Auth.guard("api");
User user = (User) guard.user();     // 自动续期发生在此处
String newToken = guard.token();     // 若已续期，返回新 token；否则返回 null
```

也可用 refresh token 主动换取新 access token：

```java
JwtGuard jwtGuard = (JwtGuard) Auth.guard("api");
String newAccessToken = jwtGuard.refresh(refreshToken);
```

### 登出（黑名单）

```java
Auth.logout();  // JWT guard 将当前 token 加入 Cache 黑名单，后续请求该 token 失效
```

黑名单存储在 Cache 中，默认使用 `array`（内存），可切换为 `file` 以支持多实例共享。

## 缓存

对齐 Laravel `Cache` 门面，支持 Array（内存）和 File（文件）两种驱动：

```java
// 基本操作
Cache.put("key", value, 60);              // 60 秒 TTL
String v = Cache.get("key");              // 取值
boolean has = Cache.has("key");           // 判断存在
Cache.forget("key");                      // 删除

// 自增
Cache.increment("counter");               // +1
Cache.increment("counter", 5);            // +5

// remember（带 TTL 的缓存闭包，对齐 Cache::remember）
Object result = Cache.remember("cfg", 300, () -> loadConfig());
```

配置：

```yaml
jaravel:
  cache:
    default-store: array    # array（内存，单机）/ file（文件，持久化）
    prefix: jaravel
```

## Blade 模板

对齐 Laravel Blade 模板引擎，支持 `@if`/`@foreach`/`@extends`/`@yield`/`@section` 等指令：

```java
// 渲染模板
return ResponseBuilder.view("welcome", Map.of(
    "title", "Hello",
    "items", List.of("a", "b", "c")
));
```

模板文件放在 `src/main/resources/templates/` 下（`.blade.html` 后缀）。

## Artisan CLI

对齐 Laravel Artisan 命令行工具。自定义命令继承 `ArtisanCommand`，实现 `signature()` 和 `handle()` 方法，标注 `@Component` 后自动注册。

```java
@Component
public class HelloCommand extends ArtisanCommand {

    @Override
    public String signature() {
        return "hello {name? : 你的名字}";
    }

    @Override
    public String description() {
        return "输出问候语";
    }

    @Override
    public int handle() {
        String name = argument("name", "World");
        info("Hello, " + name + "!");
        return 0;
    }
}
```

运行方式：

```bash
# 列出所有命令
java -jar app.jar --artisan list

# 执行命令
java -jar app.jar --artisan hello Alice
java -jar app.jar --artisan user:create 1001 Alice --email=alice@test.com
```

配置：

```yaml
jaravel:
  artisan:
    enabled: true    # 默认启用
```

本项目已注册命令：`hello`（问候语）、`user:create`（创建用户）。

## 定时任务

对齐 Laravel 任务调度框架，支持 Cron 表达式、固定间隔任务和一次性延迟任务。当 Redis 可用时，通过分布式锁确保多实例环境下同一任务只有一个实例执行。

```java
@Component
public class ScheduleConfig {

    @Autowired
    private Schedule schedule;

    @PostConstruct
    public void setup() {
        // 每分钟执行一次的缓存清理任务
        schedule.call("cleanup-expired-cache", this::cleanupExpiredCache)
                .everyMinute()
                .withDistributedLock();

        // 每天凌晨 0 点执行的日报任务
        schedule.call("daily-report", this::generateDailyReport)
                .daily()
                .withDistributedLock();
    }
}
```

配置：

```yaml
jaravel:
  schedule:
    enabled: true              # 默认启用
    # pool-size: 4              # 调度线程池大小
    # distributed-lock: true    # 启用分布式锁（Redis 可用时生效）
```

查看任务状态：`curl http://localhost:8080/api/schedule/status`

## 队列

对齐 Laravel 队列系统。实现 `ShouldQueue` 接口的监听器将被异步分发到命名队列执行，每个队列拥有独立线程池，失败自动重试。

```java
@Component
@ListensTo(NotificationEvent.class)
public class SendNotificationJob implements Listener<NotificationEvent>, ShouldQueue {

    @Override
    public void handle(NotificationEvent event) {
        // 异步处理通知
    }

    @Override
    public String queue() { return "notification"; }  // 使用 notification 队列
}
```

配置：

```yaml
jaravel:
  event:
    queue-enabled: true
    queue:
      default:
        pool-size: 4
      notification:
        pool-size: 2
    retry:
      max-attempts: 3
      delay-ms: 1000
```

队列任务演示：`curl http://localhost:8080/api/queue/demo`

## 微信 SDK

对齐 `overtrue/laravel-wechat`，封装公众号（OfficialAccount）和小程序（MiniProgram）API。`AccessTokenManager` 负责获取和缓存 access_token（支持 Redis 分布式缓存）。

```java
@Autowired
private OfficialAccountService mpService;   // 公众号服务

@Autowired
private MiniProgramService miniService;     // 小程序服务

// 获取公众号 access_token
String token = tokenManager.getToken(appId, secret);

// 获取用户信息
Map<String, Object> userInfo = mpService.getUserInfo(openid);

// 小程序登录
Map<String, Object> session = miniService.code2Session(jsCode);
```

配置：

```yaml
jaravel:
  wechat:
    enabled: true
    official-accounts:
      default:
        app-id: your-app-id
        secret: your-secret
    mini-apps:
      default:
        app-id: your-mini-app-id
        secret: your-mini-secret
    http:
      timeout: 5.0
      retry: true
```

access_token 演示：`curl http://localhost:8080/api/wechat/token`

## 验证码（Captcha）

引入 `captcha` 依赖后，`CaptchaManager` 由 `CaptchaAutoConfiguration` 自动装配（前缀 `jaravel.captcha`），开箱注册四种验证码类型：图片数字 `number`、算术 `arithmetic`、滑动 `slider`、旋转 `rotate`。核心层为纯 Java 实现（基于 `java.awt`），无第三方依赖，可独立使用。

本项目通过 `CaptchaController`（标准 Spring MVC `@RestController`，与自定义 RouterFunction 路由并存）暴露 REST 接口演示。

### 支持的验证码类型

| 类型 | type | 说明 | 用户输入 |
|------|------|------|----------|
| 图片数字 | `number` | 随机字母+数字字符串（排除易混淆字符 0/O/1/I/L） | 识别的字符，如 `AB23` |
| 算术 | `arithmetic` | `a op b = ?` 算式（加减乘），答案为运算结果 | 计算结果，如 `17` |
| 滑动 | `slider` | 背景图抠缺口，拖动滑块拼回 | 启用轨迹验证时为 JSON `{"value":x,"trajectory":[...]}`，否则为 x 坐标数字 |
| 旋转 | `rotate` | 图片随机旋转，拖动转回正方向 | 启用轨迹验证时为 JSON `{"value":角度,"trajectory":[...]}`，否则为角度数字 |

滑动与旋转验证码支持**轨迹行为分析**：通过 `TrajectoryValidator` 校验拖动轨迹的人类行为特征（点数、时长、连续性、非匀速、加速度多样性），防范自动化脚本直接提交最终值。

### REST 接口

```bash
# 1. 查看支持的验证码类型
curl http://localhost:8080/api/captcha/types

# 2. 生成验证码（type 可选：number|arithmetic|slider|rotate，默认 number）
curl "http://localhost:8080/api/captcha/generate?type=slider"

# 3. 校验用户输入
curl -X POST http://localhost:8080/api/captcha/verify \
  -H "Content-Type: application/json" \
  -d '{"captchaKey":"<captchaKey>","input":"AB23","type":"number"}'
```

生成接口返回字段：

| 字段 | 说明 |
|------|------|
| `captchaKey` | 验证码标识，校验时原样回传 |
| `type` | 验证码类型 |
| `imageBase64` | 验证码图片（带 `data:image/png;base64,` 前缀，前端可直接用于 `<img src="...">`） |
| `token` | 无状态 token（包含答案的 Base64 编码信息，可用于无状态验证） |
| `expireTime` | 过期时间戳（毫秒） |
| `extra` | 额外数据（滑动验证码含 `sliderImage`/`gapY`/`blockSize`/`trajectoryEnabled`；旋转含 `size`/`trajectoryEnabled`） |

### 代码示例

```java
@Autowired
private CaptchaManager captchaManager;

// 生成
String captchaKey = UUID.randomUUID().toString().replace("-", "");
CaptchaResult result = captchaManager.generate("slider", captchaKey);
String image = result.getImageBase64();                 // 带缺口的背景图
String sliderImage = (String) result.getExtra().get("sliderImage"); // 滑块小块

// 验证（数字/算术）
boolean ok = captchaManager.verify("number", captchaKey, "AB23");

// 验证（滑动，启用轨迹验证时提交 JSON）
boolean ok2 = captchaManager.verify("slider", captchaKey,
        "{\"value\":123,\"trajectory\":[{\"t\":0,\"v\":0},{\"t\":50,\"v\":5}]}");
```

### 配置

```yaml
jaravel:
  captcha:
    enabled: true                  # 启用自动装配（默认 true）
    width: 200                     # 图片宽度
    height: 60                     # 图片高度
    length: 4                      # 字符长度（数字/算术验证码）
    expire-seconds: 300            # 过期时间（秒）
    tolerance: 5.0                 # 滑动/旋转容差（滑动为像素，旋转为角度）
    # 视觉配置
    font-family: Arial
    arc-interfere: true            # 弧线干扰（比直线更难被 OCR 识别）
    arc-interfere-count: 5
    # 轨迹验证（滑动/旋转）
    trajectory-enabled: true
    min-trajectory-points: 5
    min-trajectory-duration-ms: 500
    # 水印（可选）
    # watermark-text: "Jaravel Demo"
    # watermark-position: bottom-right
```

存储选择：引入 jaravel `cache` 模块时，验证码答案自动存入 `CacheStore`（Redis/数据库，支持跨进程）；未引入则使用内存存储（`MemoryCaptchaStore`，单进程有效）。

详细文档见 [captcha README](../jaravel-vendor/captcha/README.md)。

## 纯前端文档包

本项目包含一套纯前端文档包，使用 jblade 模板引擎渲染，位于 `src/main/resources/templates/docs/` 目录下。文档页面包括：

- 文档布局模板（`layout.blade.html`）：侧边栏导航 + 内容区域，使用 `@yield('content')` 占位
- 11 个内容页面：首页、安装指南、路由、Eloquent ORM、认证、缓存、事件系统、Artisan CLI、定时任务、队列、插件系统

文档路由定义在 `routes/Web.java` 中，通过 `ResponseBuilder.view()` 渲染 jblade 模板。访问 http://localhost:8080/docs 即可浏览文档。

每个文档页面包含：
- 标题和简介
- Java 代码示例（代码块）
- YAML 配置示例（代码块）
- 注意事项（note/tip/warn 提示框）

## 迁移系统

迁移类名采用 `Migration_YYYY_MM_DD_PascalCaseDescription` 约定，类名自带日期前缀，
`getName()` 默认返回类名，`Migrator` 按类名字典序排序即可保证执行顺序。
一次 `up()` 可处理多张表，`down()` 应对称回滚。

```java
@Component
public class Migration_2024_01_01_CreateUsersTable implements Migration {
    // getName() 默认返回类名 "Migration_2024_01_01_CreateUsersTable"，无需覆写

    @Override
    public void up(Schema schema) {
        schema.create("users", table -> {
            table.id();
            table.string("name", 50);
            table.string("number", 50);
            table.string("password", 100);
            table.string("email", 50).nullable();
            table.timestamps();
        });
        // 一次 up() 可创建多张表
        schema.create("user_profiles", table -> {
            table.id();
            table.bigInteger("user_id").unsigned();
            table.string("nickname", 50).nullable();
            table.timestamps();
        });
    }

    @Override
    public void down(Schema schema) {
        schema.dropIfExists("user_profiles");
        schema.dropIfExists("users");
    }
}
```

## 插件系统

本项目演示了两种插件系统：JAR 插件（`plugin-jar-core`）和 Java 文件插件（`plugin-java-core`）。

### 配置

```yaml
jaravel:
  plugin-jar:
    enabled: true
    plugins-dir: plugins
    auto-restore: true       # 重启后自动恢复已启用的插件
    auto-register: true      # true=自动注册@PluginMapping, false=手动注册
  plugin-java:
    enabled: true
    source-dir: plugins-java
    auto-scan: true          # 启动时自动扫描 .java 文件插件
    auto-register: true      # true=自动注册@PluginMapping, false=手动注册
```

### 路由注册模式

插件系统支持两种路由注册模式，通过 `auto-register` 配置控制：

| 模式 | @PluginMapping | @PluginRoute | 说明 |
|------|----------------|--------------|------|
| 自动注册（默认） | 自动注册 | 列为可用 | 插件启用时自动注册 |
| 手动注册 | 列为可用 | 列为可用 | 所有路由需手动注册 |

### Java 文件插件演示

演示插件位于 `plugins-java/demo-greeting/GreetingPlugin.java`，包含：
- `@PluginMapping` 自动注册路由：`/api/plugin/greeting`、`/api/plugin/time`
- `@PluginRoute` 可注册路由：`/api/plugin/manual-greeting`、`/api/plugin/info`

启动时自动扫描编译，无需手动操作。修改 .java 文件后可通过 API 热重载：

```bash
# 列出所有 Java 文件插件
curl http://localhost:8080/api/plugins/java

# 热重载指定插件（修改 .java 文件后执行）
curl -X POST http://localhost:8080/api/plugins/java/demo-greeting/reload

# 重载所有有变更的插件
curl -X POST http://localhost:8080/api/plugins/java/reload-all

# 测试自动注册路由
curl http://localhost:8080/api/plugin/greeting?name=world

# 列出可注册路由（manual-register 模式）
curl http://localhost:8080/api/plugins/java/demo-greeting/available-routes

# 手动注册可注册路由
curl -X POST "http://localhost:8080/api/plugins/java/demo-greeting/available-routes/register?path=/api/plugin/manual-greeting&method=GET"

# 测试手动注册的路由
curl http://localhost:8080/api/plugin/manual-greeting?name=world
```

### JAR 插件管理 API（仅演示，生产环境不应暴露）

```bash
# 列出所有 JAR 插件
curl http://localhost:8080/api/plugins/jar

# 上传 JAR 插件（落盘持久化）
curl -X POST http://localhost:8080/api/plugins/jar/upload -F "file=@my-plugin.jar"

# 启用插件
curl -X POST http://localhost:8080/api/plugins/jar/my-plugin/enable

# 禁用插件
curl -X POST http://localhost:8080/api/plugins/jar/my-plugin/disable

# 手动注册路由
curl -X POST http://localhost:8080/api/plugins/jar/my-plugin/routes \
  -H "Content-Type: application/json" \
  -d '{"path":"/api/test","method":"GET","beanName":"myService","methodName":"test"}'

# 手动注销路由
curl -X DELETE "http://localhost:8080/api/plugins/jar/my-plugin/routes?path=/api/test&method=GET"

# 列出可注册路由
curl http://localhost:8080/api/plugins/jar/my-plugin/available-routes

# 手动注册可注册路由
curl -X POST "http://localhost:8080/api/plugins/jar/my-plugin/available-routes/register?path=/api/test&method=GET"
```

### 编写插件

```java
@PluginComponent("myService")
public class MyService {

    // 自动注册路由（auto-register=true 时自动注册）
    @PluginMapping(path = "/api/my-service", method = HttpMethod.GET)
    public String handle(String param) {
        return "Result: " + param;
    }

    // 可注册路由（需手动注册）
    @PluginRoute(path = "/api/manual-service", method = HttpMethod.GET)
    public String manualHandle(String param) {
        return "Manual Result: " + param;
    }
}
```

详细文档见 [plugin-jar-core README](../jaravel-vendor/plugin-jar-core/README.md) 和 [plugin-java-core README](../jaravel-vendor/plugin-java-core/README.md)。

## 线程安全

所有 vendor 模块均考虑了线程安全，可在高并发环境下安全使用：

- **中间件**：所有中间件为 Spring `@Component` 管理的不可变单例（`final` 字段、无 setter），无状态、可安全并发复用
- **AuthManager**：Provider/Guard 注册表使用 `ConcurrentHashMap`，`defaultGuard` 字段使用 `volatile`，per-request 状态使用 `ThreadLocal` + `ConcurrentHashMap`
- **JwtService**：无状态单例，所有字段 `final`，token 签发/验证为纯函数，黑名单操作委托给线程安全的 CacheStore
- **CacheManager**：ArrayStore 内部使用 `ConcurrentHashMap`，FileStore 使用文件锁
- **EventDispatcher**：监听器注册表使用 `CopyOnWriteArrayList`，QueueManager 使用 `ConcurrentHashMap` 管理线程池
- **Router**：路由注册在启动阶段完成（单线程），运行时只读访问
- **HotPluginManager**：ReadWriteLock 保护插件状态，ConcurrentHashMap 管理插件列表和 ClassLoader
- **JavaFilePluginManager**：ReadWriteLock 保护状态变更，ConcurrentHashMap 管理插件和 ClassLoader

## 完整配置参考

```yaml
jaravel:
  auth:
    default-guard: api              # 默认守卫
  jwt:
    secret: your-secret-key         # JWT 密钥（至少 32 字节）
    ttl: 3600000                    # access token 有效期（毫秒）
    refresh-ttl: 604800000          # refresh token 有效期（毫秒）
    refresh-enabled: true           # token 自动续期（默认启用）
    blacklist-store: array          # 登出黑名单缓存（array/file）
    blacklist-prefix: "jwt:blacklist:"
  cache:
    default-store: array            # 默认缓存驱动（array/file/redis）
    prefix: jaravel
  event:
    queue-enabled: true             # 是否启用异步队列分发
    queue:
      default:
        pool-size: 4                # 默认队列线程池大小
      notification:                # notification 队列（SendNotificationJob 使用）
        pool-size: 2
    retry:
      max-attempts: 3               # 最大重试次数
      delay-ms: 1000                # 重试间隔毫秒
  artisan:
    enabled: true                   # 启用 Artisan CLI（默认启用）
  schedule:
    enabled: true                   # 启用定时任务调度（默认启用）
    # pool-size: 4                   # 调度线程池大小
    # distributed-lock: true         # 启用分布式锁（Redis 可用时生效）
  redis:
    options:
      cluster: redis                # 集群模式：redis(单机)/cluster/sentinel
      prefix: "jaravel-demo_"
    connections:
      default:
        host: 127.0.0.1
        port: 6379
  wechat:
    enabled: true                   # 启用微信 SDK（默认启用）
    official-accounts:
      default:
        app-id: your-app-id
        secret: your-secret
    mini-apps:
      default:
        app-id: your-mini-app-id
        secret: your-mini-secret
    http:
      timeout: 5.0                  # HTTP 超时（秒）
      retry: true                   # 失败重试
  captcha:
    enabled: true                   # 启用验证码自动装配（默认 true）
    width: 200                      # 图片宽度
    height: 60                      # 图片高度
    length: 4                       # 字符长度（数字/算术验证码）
    expire-seconds: 300             # 过期时间（秒）
    tolerance: 5.0                  # 滑动/旋转容差
    trajectory-enabled: true        # 启用轨迹验证（滑动/旋转）
  migration:
    enabled: true                   # 是否启用迁移
    table: migrations               # 迁移记录表名
    auto-run: true                  # 启动时自动执行迁移
  plugin-jar:
    enabled: true                   # 启用 JAR 插件系统
    plugins-dir: plugins            # 插件目录
    auto-restore: true              # 启动时自动恢复已启用的插件
    auto-register: true             # true=自动注册@PluginMapping, false=手动注册
  plugin-java:
    enabled: true                   # 启用 Java 文件插件系统
    source-dir: plugins-java        # .java 文件插件源目录
    auto-scan: true                 # 启动时自动扫描并注册
    auto-register: true             # true=自动注册@PluginMapping, false=手动注册
```

## jaravel-vendor 模块

所有 vendor 模块包名统一为 `com.weacsoft.jaravel.vendor.*`，与业务项目的 `com.weacsoft.jaravel.*` 分离。
各模块详细文档见 [jaravel-vendor README](../jaravel-vendor/README.md)。

| 模块 | 说明 | 详细文档 |
|------|------|---------|
| `core` | Facade/Config/ServiceProvider/SpringContext/Validation | [README](../jaravel-vendor/core/README.md) |
| `http` | Middleware管道/Request·Response/路由系统 | [README](../jaravel-vendor/http/README.md) |
| `auth` | AuthManager/Guard(JWT·Session)/多Provider | [README](../jaravel-vendor/auth/README.md) |
| `jwt` | JWT认证插件（续期/登出黑名单/Cache集成） | [README](../jaravel-vendor/jwt/README.md) |
| `database` | BaseModel(Eloquent合并模式)/@DataSource多数据源 | [README](../jaravel-vendor/database/README.md) |
| `migration` | Blueprint/Schema/Migrator（MySQL/SQLite/SQL Server） | [README](../jaravel-vendor/migration/README.md) |
| `cache` | CacheManager/Array·File驱动/Cache门面 | [README](../jaravel-vendor/cache/README.md) |
| `event` | Dispatcher/Listener/QueueManager（多队列+重试） | [README](../jaravel-vendor/event/README.md) |
| `jblade` | Blade模板引擎（@if/@foreach/@extends等指令） | [README](../jaravel-vendor/jblade/README.md) |
| `springboot` | RouterFunction桥接/全局中间件注入/MVC解析 | [README](../jaravel-vendor/springboot/README.md) |
| `artisan` | Artisan CLI命令框架（签名解析/参数/命令注册） | [README](../jaravel-vendor/artisan/README.md) |
| `schedule` | 定时任务调度（Cron/固定间隔/Redis分布式锁） | [README](../jaravel-vendor/schedule/README.md) |
| `queue-database` | 数据库队列驱动（持久化/多实例消费/重试） | [README](../jaravel-vendor/queue-database/README.md) |
| `wechat-sdk` | 微信SDK（公众号/小程序/access_token管理） | [README](../jaravel-vendor/wechat-sdk/README.md) |
| `captcha` | 验证码（图片数字/算术/滑动/旋转，轨迹行为分析） | [README](../jaravel-vendor/captcha/README.md) |
| `redis-config` | Redis连接管理（多命名连接/Lettuce/分布式锁） | [README](../jaravel-vendor/redis-config/README.md) |
| `redis-cache` | Redis缓存驱动（多机缓存同步） | [README](../jaravel-vendor/redis-cache/README.md) |
| `session-redis` | Redis Session守卫（多机Session同步） | [README](../jaravel-vendor/session-redis/README.md) |
| `starter` | 聚合Starter（引入即自动装配全部模块，jwt可选） | [README](../jaravel-vendor/starter/README.md) |
| `plugin-jar-core` | JAR插件系统（动态加载/卸载/三级ClassLoader/动态路由） | [README](../jaravel-vendor/plugin-jar-core/README.md) |
| `plugin-jar-database` | JAR插件数据库持久化（BaseModel/自动建表） | [README](../jaravel-vendor/plugin-jar-database/README.md) |
| `plugin-java-core` | Java文件插件系统（动态编译.java/热更新） | [README](../jaravel-vendor/plugin-java-core/README.md) |
| `plugin-jar-multi-tenant` | JAR插件多租户支持（租户隔离的Bean/路由前缀化，可选） | [README](../jaravel-vendor/plugin-jar-multi-tenant/README.md) |
| `plugin-jar-remote-server` | JAR插件远程执行服务端（P2SP子节点，TCP/HTTP） | [README](../jaravel-vendor/plugin-jar-remote-server/README.md) |
| `plugin-jar-remote-client` | JAR插件远程执行客户端（P2SP主节点，动态代理/协调器） | [README](../jaravel-vendor/plugin-jar-remote-client/README.md) |
| `utils` | 通用工具（内存编译基础设施 MemoryClassLoader 等） | [README](../jaravel-vendor/utils/README.md) |

## 许可证

MIT License
