@extends('docs.layout')

@section('content')
<h1>路由</h1>
<p>Jaravel 提供了 Laravel 风格的路由系统，通过 <code>routes/Api.java</code> 和 <code>routes/Web.java</code> 定义路由，由 <code>RouteServiceProvider</code> 加载。</p>

<h2>路由定义</h2>
<p>路由通过 <code>Router</code> 的 HTTP 动词方法注册，处理函数签名为 <code>Response handle(Request request)</code>：</p>
<pre><code>@Component
public class Api {
    public void register(Router router, ApplicationContext context) {
        WelcomeController welcome = context.getBean(WelcomeController.class);

        router.group(Map.of(Route.Group.PREFIX, "api"), api -> {
            // GET 路由
            api.get("/hello", welcome::hello);

            // POST 路由
            api.post("/auth/register", authController::register);

            // 带路径参数的路由
            api.get("/users/{id}", userController::show);
        });
    }
}</code></pre>

<h2>路由分组</h2>
<p>使用 <code>group()</code> 方法创建路由分组，支持设置前缀、命名空间和名称：</p>
<pre><code>router.group(Map.of(
    Route.Group.PREFIX, "/api/v1",
    Route.Group.NAMESPACE, "Api\\V1"
), api -> {
    api.get("/posts", req -> ResponseBuilder.json(postService.all()));
    api.post("/posts", req -> {
        String title = req.input("title");
        return ResponseBuilder.json(postService.create(title));
    });
});</code></pre>

<h2>中间件</h2>
<p>路由可附加中间件，支持链式调用（洋葱模型）：</p>
<pre><code>// 需要认证的路由（默认 Guard）
api.group(Map.of(), auth -> {
    auth.get("/auth/me", authController::me);
    auth.get("/users", userController::list);
}).middleware(new Authenticate());

// 指定 Guard 的路由
api.get("/guard/api", handler).middleware(new Authenticate("api"));
api.get("/guard/web", handler).middleware(new Authenticate("web"));

// 多中间件链（洋葱模型）
api.get("/middleware-test", handler)
   .middleware(new OrderTestMiddleware("A"))
   .middleware(new OrderTestMiddleware("B"))
   .middleware(new OrderTestMiddleware("C"));</code></pre>

<h2>RESTful 路由示例</h2>
<pre><code>// 用户资源路由
api.get("/users", userController::list);           // GET    /api/users
api.get("/users/{id}", userController::show);      // GET    /api/users/{id}
api.post("/users", userController::create);        // POST   /api/users
api.put("/users/{id}", userController::update);    // PUT    /api/users/{id}
api.delete("/users/{id}", userController::delete); // DELETE /api/users/{id}</code></pre>

<h2>Request 参数获取</h2>
<pre><code>// 路径参数
Long id = request.routeParam("id", Long.class);

// 查询参数
String filter = request.query("filter");

// 输入参数（自动从 JSON body 或 form 中解析）
String name = request.input("name");

// 请求头
String token = request.header("Authorization");

// 文件上传
if (request.hasFile("avatar")) {
    MultipartFile avatar = request.file("avatar");
}</code></pre>

<h2>路由配置</h2>
<p>路由由 <code>RouteServiceProvider</code> 加载，在 <code>boot()</code> 中注册全局中间件：</p>
<pre><code>@Configuration
public class RouteServiceProvider extends ServiceProvider {
    @Override
    public void boot() {
        // 系统全局中间件
        globalMiddlewareRegistry.addByType(TrimStrings.class);
        globalMiddlewareRegistry.addByType(ConvertEmptyStringsToNull.class);
    }

    @Override
    public void register() {
        // 加载 API 和 Web 路由
        Router router = Facade.resolve(Router.class);
        api.register(router, context);
        web.register(router, context);
    }
}</code></pre>

<div class="note">
    <strong>注意：</strong>系统全局中间件（TrimStrings、ConvertEmptyStringsToNull 等）已标注 @Component，在 RouteServiceProvider.boot() 中统一注册，无需在路由中手动添加。需要构造参数的中间件（如 new Authenticate("api")）直接使用 new 创建，它们不可变、无状态，可安全并发复用。
</div>
@endsection
