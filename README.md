# jaravel v0.1.0

多租户 Jar/Java 热更新在线运行平台。基于 jaravel-vendor 框架，提供 Java 源码在线编译执行、Jar 插件热加载、多租户隔离、P2SP 远程执行等能力，配以双 Guard 认证和树形 RBAC 权限控制。

## 核心特性

- **Java 在线编译**：使用 javax.tools.JavaCompiler 实时编译 Java 源码，反射调用 run() 或 main() 方法返回结果
- **Jar 插件热加载**：URLClassLoader 动态加载 Jar 插件，支持启用/禁用、路由注册、Bean 注入、自动恢复
- **多租户隔离**：同一 JAR 插件按租户隔离加载，Bean 名称和路由路径自动按租户前缀化
- **P2SP 远程执行**：树形拓扑远程执行，主节点分发任务到子节点，支持 relay 转发和三重防环检测
- **双 Guard 认证**：admin guard（管理员）+ api guard（用户），均为 JWT 驱动
- **树形 RBAC 权限**：Admin 和 User 各自独立的权限树，父节点授权等同于旗下所有子节点授权
- **Java 文件插件热重载**：监听 .java 文件变化自动重载，无需重启应用
- **Eloquent 合并 Model**：单一类同时承担实体定义与查询能力，对齐 Laravel Eloquent
- **Laravel 风格目录结构**：config/、routes/、database/ 与 app/ 同级
- **Artisan CLI**：Laravel 风格命令行工具，支持 db:seed 种子数据初始化

## 目录结构

```
jaravel/
├── src/main/java/com/weacsoft/jaravel/
│   ├── JaravelApplication.java              # 应用入口
│   ├── config/                              # 配置
│   │   ├── App.java                         # 引导配置
│   │   └── Database.java                    # 数据库配置（单数据源）
│   ├── routes/                              # 路由
│   │   ├── Api.java                         # API 路由（公开 + Admin + User 三组）
│   │   └── Web.java                         # Web 路由（首页重定向）
│   ├── database/migration/                  # 数据库迁移
│   │   ├── Migration_2024_01_01_CreateUsersTable.java
│   │   ├── Migration_2024_01_03_CreateAdminRbacTables.java   # Admin RBAC 五表
│   │   └── Migration_2024_01_04_CreateUserRbacTables.java    # User RBAC 五表
│   └── app/
│       ├── model/
│       │   ├── User.java                    # 用户模型（implements Authenticatable）
│       │   ├── admin/                       # Admin RBAC 模型
│       │   │   ├── Admin.java               # 管理员（implements Authenticatable）
│       │   │   ├── AdminRole.java
│       │   │   ├── AdminPermission.java
│       │   │   └── middle/                  # 中间表
│       │   └── user/                        # User RBAC 模型
│       │       ├── UserRole.java
│       │       ├── UserPermission.java
│       │       └── middle/
│       ├── service/
│       │   ├── UserService.java             # 用户登录/注册
│       │   ├── PluginRunService.java        # Java 编译执行 + Jar 反射调用
│       │   ├── AdminRolePermissionService.java  # Admin RBAC 服务
│       │   └── UserRolePermissionService.java   # User RBAC 服务
│       ├── http/
│       │   ├── controller/
│       │   │   ├── AuthController.java      # 双 Guard 认证（admin + user）
│       │   │   ├── PluginController.java     # 插件管理（Jar + Java）
│       │   │   ├── PluginRunController.java  # 插件执行（runJava + runJar）
│       │   │   ├── TenantController.java     # 多租户管理
│       │   │   ├── RemoteController.java     # 远程执行管理
│       │   │   ├── AdminRbacController.java  # Admin RBAC 端点
│       │   │   ├── UserRbacController.java   # User RBAC 端点
│       │   │   └── UserController.java       # 用户列表
│       │   └── middleware/
│       │       ├── RoutePermissionMiddleware.java       # Admin 路由权限
│       │       └── UserRoutePermissionMiddleware.java   # User 路由权限
│       ├── console/
│       │   ├── DatabaseSeedCommand.java     # db:seed 种子数据初始化
│       │   └── ScheduleConfig.java          # 定时任务（插件清理）
│       └── provider/
│           ├── AuthServiceProvider.java     # 认证配置（admin + api 双 Guard）
│           └── RouteServiceProvider.java   # 路由加载
├── src/main/resources/
│   ├── application.yml
│   └── static/                              # 前端页面
│       ├── index.html                       # 平台首页
│       ├── admin.html                       # 管理员后台
│       ├── user.html                        # 用户插件执行
│       └── css/style.css                    # 共享样式
├── plugins-java/                            # Java 文件插件目录
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

### 2. 运行 jaravel

```bash
cd jaravel
mvn spring-boot:run
```

应用启动后：
1. 自动执行数据库迁移（users 表 + Admin RBAC 五表 + User RBAC 五表）
2. 启动 Web 服务：`http://localhost:8080`

### 3. 初始化种子数据

```bash
java -jar target/jaravel-0.1.0.jar --artisan db:seed
```

种子数据包括：
- Admin 权限树（system 根 + 7 个子节点：admin/role/permission/user/plugin/tenant/remote 管理）
- User 权限树（platform 根 + java/jar 子节点 + 4 个叶子节点）
- 超级管理员角色（分配所有 Admin 权限）
- 默认用户角色（普通用户、仅 Java、仅 Jar）
- 初始管理员账号：**admin / admin123**

### 4. 访问前端页面

| 页面 | URL | 说明 |
|------|-----|------|
| 平台首页 | http://localhost:8080/ | 平台介绍 + 插件系统总览 |
| 管理员后台 | http://localhost:8080/admin.html | 管理员登录 + RBAC + 插件管理 |
| 用户入口 | http://localhost:8080/user.html | 用户登录 + Java/Jar 插件执行 |

### 5. API 验证

```bash
# 管理员登录
curl -X POST http://localhost:8080/api/auth/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 用户注册
curl -X POST http://localhost:8080/api/auth/user/register \
  -H "Content-Type: application/json" \
  -d '{"name":"alice","number":"alice001","password":"secret123","email":"alice@test.com"}'

# 用户登录
curl -X POST http://localhost:8080/api/auth/user/login \
  -H "Content-Type: application/json" \
  -d '{"number":"alice001","password":"secret123"}'

# 在线编译执行 Java 源码（需用户 token）
curl -X POST http://localhost:8080/api/plugin/java/run \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"public class Hello { public String run() { return \"Hello!\"; } }"}'

# 插件系统总览（公开）
curl http://localhost:8080/api/plugin/overview
```

## 认证系统

### 双 Guard 架构

| Guard | 驱动 | Provider | 凭证字段 | 用途 |
|-------|------|----------|----------|------|
| admin | JWT | admins | username | 管理员认证 |
| api | JWT | users | number | 用户认证 |

```java
// AuthServiceProvider 注册双 Guard
authManager.registerProvider("users", new EloquentUserProvider<>(userModel, "number"));
authManager.registerProvider("admins", new EloquentUserProvider<>(adminModel, "username"));
authManager.registerGuard("api", "jwt", "users");
authManager.registerGuard("admin", "jwt", "admins");
```

### 路由保护

```java
// Admin 路由：admin guard + admin 路由权限中间件
api.group(Map.of(), admin -> {
    admin.get("/rbac/admins", rbacController::listAdmins);
}).middleware(new Authenticate("admin"), adminRbacMiddleware);

// User 路由：api guard + user 路由权限中间件
api.group(Map.of(), user -> {
    user.post("/plugin/java/run", pluginRun::runJava);
}).middleware(new Authenticate("api"), userRbacMiddleware);
```

## RBAC 权限系统

### 树形权限原理

权限为树形层级关系，父节点授权等同于旗下所有子节点授权。判断时从目标权限沿 parent_id 向上遍历，途经任一已授权节点即为拥有。默认拒绝（无任何授权时返回 false）。

### Admin RBAC（五表）

| 表 | 说明 |
|----|------|
| admins | 管理员表 |
| admin_roles | 角色表 |
| admin_permissions | 权限表（含 parent_id 树形自引用、route 路由匹配字段） |
| admin_role | 管理员-角色中间表 |
| role_permission | 角色-权限中间表 |

### User RBAC（五表）

与 Admin RBAC 结构一致，独立管理用户侧权限。User 权限的 route 字段用于路由匹配，支持全匹配和通配匹配（如 `/api/plugin/java/*`）。

### 种子数据权限树

**Admin 权限树：**
```
system（根）
├── admin.manage    /api/rbac/admins/*
├── role.manage     /api/rbac/roles/*
├── permission.manage /api/rbac/permissions/*
├── user.manage     /api/user-rbac/users/*
├── plugin.manage   /api/plugins/*
├── tenant.manage   /api/multi-tenant/*
└── remote.manage   /api/remote/*
```

**User 权限树：**
```
platform（根）
├── plugin.java
│   ├── plugin.java.run    /api/plugin/java/run
│   └── plugin.java.status /api/plugin/java/status
└── plugin.jar
    ├── plugin.jar.run     /api/plugin/jar/run
    └── plugin.jar.status  /api/plugin/jar/status
```

## 插件执行

### Java 在线编译

使用 javax.tools.JavaCompiler 实时编译 Java 源码，通过 URLClassLoader 加载，反射调用 run() 或 main() 方法：

```bash
curl -X POST http://localhost:8080/api/plugin/java/run \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"code":"public class Calc { public String run() { return String.valueOf(1+2); } }"}'
```

### Jar 插件反射调用

通过 URLClassLoader 加载 Jar 文件，反射调用指定方法：

```bash
curl -X POST http://localhost:8080/api/plugin/jar/run \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"jar_name":"my-plugin.jar","main_class":"com.example.MyService","method":"run"}'
```

## 插件管理

### Jar 插件管理（Admin）

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/plugins/jar | GET | 列出所有 Jar 插件 |
| /api/plugins/jar/upload | POST | 上传 Jar 插件 |
| /api/plugins/jar/{pluginId}/enable | POST | 启用插件 |
| /api/plugins/jar/{pluginId}/disable | POST | 禁用插件 |
| /api/plugins/jar/{pluginId}/routes | POST | 手动注册路由 |
| /api/plugins/jar/{pluginId}/available-routes | GET | 列出可注册路由 |

### Java 文件插件管理（Admin）

| 端点 | 方法 | 说明 |
|------|------|------|
| /api/plugins/java | GET | 列出所有 Java 插件 |
| /api/plugins/java/register | POST | 注册 Java 插件 |
| /api/plugins/java/{pluginId}/reload | POST | 热重载指定插件 |
| /api/plugins/java/reload-all | POST | 重载所有变更插件 |

### 配置

```yaml
jaravel:
  plugin-jar:
    enabled: true
    plugins-dir: plugins
    auto-restore: true
    auto-register: true
  plugin-java:
    enabled: true
    source-dir: plugins-java
    auto-scan: true
    auto-register: true
```

## 定时任务

```java
// 每小时清理临时编译文件
schedule.call("cleanup-temp-classfiles", this::cleanupTempClassFiles)
        .hourly()
        .withDistributedLock();

// 每天清理过期缓存
schedule.call("cleanup-expired-cache", this::cleanupExpiredCache)
        .daily()
        .withDistributedLock();
```

## 完整配置参考

```yaml
jaravel:
  auth:
    default-guard: api
  jwt:
    secret: your-secret-key-must-be-32-bytes
    ttl: 3600000
    refresh-ttl: 604800000
    refresh-enabled: true
    blacklist-store: array
  cache:
    default-store: array
    prefix: jaravel
  artisan:
    enabled: true
  schedule:
    enabled: true
  migration:
    enabled: true
    table: migrations
    auto-run: true
  plugin-jar:
    enabled: true
    plugins-dir: plugins
    auto-restore: true
    auto-register: true
  plugin-java:
    enabled: true
    source-dir: plugins-java
    auto-scan: true
    auto-register: true
```

## jaravel-vendor 模块

| 模块 | 说明 |
|------|------|
| core | Facade/Config/ServiceProvider/SpringContext |
| http | Middleware 管道/Request·Response/路由系统 |
| auth | AuthManager/Guard(JWT·Session)/多 Provider |
| jwt | JWT 认证（续期/登出黑名单/Cache 集成） |
| database | BaseModel(Eloquent 合并模式)/@DataSource |
| migration | Blueprint/Schema/Migrator |
| cache | CacheManager/Array·File 驱动 |
| event | Dispatcher/Listener/QueueManager |
| jblade | Blade 模板引擎 |
| springboot | RouterFunction 桥接/全局中间件 |
| artisan | Artisan CLI 命令框架 |
| schedule | 定时任务调度（Cron/分布式锁） |
| starter | 聚合 Starter |
| plugin-jar-core | JAR 插件系统（动态加载/三级 ClassLoader） |
| plugin-jar-database | JAR 插件数据库持久化 |
| plugin-java-core | Java 文件插件系统（动态编译/热更新） |
| plugin-jar-multi-tenant | JAR 插件多租户支持 |
| plugin-jar-remote-server | P2SP 远程执行服务端 |
| plugin-jar-remote-client | P2SP 远程执行客户端 |

## 许可证

MIT License
