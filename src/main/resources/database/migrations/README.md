# Jaravel 目录结构说明（对齐 Laravel）

本模板项目在 `src/main/resources` 下保留 Laravel 风格目录，对应关系如下：

| Laravel 目录 | Jaravel 对应位置 | 说明 |
|---|---|---|
| `config/` | `src/main/resources/config/` + `application.yml` | 配置文件，通过 `Config.get("key")` 读取 |
| `database/migrations/` | `src/main/java/.../app/database/migration/` | 迁移类（Java 实现 `Migration` 接口，`@Component` 自动发现） |
| `database/seeders/` | `src/main/java/.../app/database/seeder/` | 数据填充（`ApplicationRunner` 实现） |
| `routes/` | `src/main/java/.../app/http/controller/` | 路由通过 `@RestController` + `@RequestMapping` 声明（保留 Spring 原生能力） |
| `resources/views/` | `src/main/resources/resources/views/` | 视图模板（可选，配合 jblade 模板引擎） |
| `app/Http/Controllers` | `src/main/java/.../app/http/controller/` | 控制器 |
| `app/Models` | `src/main/java/.../app/model/` | Eloquent Model |
| `app/Services` | `src/main/java/.../app/service/` | Service 层（静态调用风格） |
| `app/Providers` | `src/main/java/.../app/provider/` | 服务提供者（继承 `ServiceProvider`） |
| `app/Http/Requests` | `src/main/java/.../app/http/request/` | 表单请求校验（继承 `FormRequest`） |

## 路由风格

Jaravel 保留 SpringBoot 的 `@RequestMapping` 路由能力，并叠加 Laravel 风格中间件：

```java
@RestController
@RequestMapping("/api/users")
@Middleware("auth")                      // 类级中间件
public class UserController {
    @GetMapping
    @Middleware("permission:user.view")  // 方法级中间件（带参数）
    public Map<String, Object> list() { ... }
}
```

## 迁移风格

迁移是 Java 类，实现 `Migration` 接口，用 `Blueprint` 流式 API 建表。
类名采用 `Migration_YYYY_MM_DD_PascalCaseDescription` 约定，`getName()` 默认返回类名，
`Migrator` 按类名字典序排序即可保证执行顺序。一次 `up()` 可处理多张表：

```java
@Component
public class Migration_2024_01_01_CreateUsersTable implements Migration {
    // getName() 默认返回类名，无需覆写
    public void up(Schema schema) {
        schema.create("users", table -> {
            table.id();
            table.string("name");
            table.timestamps();
        });
        // 一次 up() 可创建多张表
        schema.create("user_profiles", table -> {
            table.id();
            table.bigInteger("user_id").unsigned();
            table.timestamps();
        });
    }
    public void down(Schema schema) {
        schema.dropIfExists("user_profiles");
        schema.dropIfExists("users");
    }
}
```

启动时自动执行（`jaravel.migration.auto-run=true`），或通过命令参数：
- `--jaravel.migrate` 执行迁移
- `--jaravel.rollback=1` 回滚
- `--jaravel.refresh` 重置并重新迁移
- `--jaravel.migration-status` 查看状态
