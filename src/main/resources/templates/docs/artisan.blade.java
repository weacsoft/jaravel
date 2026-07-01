@extends('docs.layout')

@section('content')
<h1>Artisan CLI</h1>
<p>Jaravel 提供 Laravel 风格的命令行工具框架（Artisan）。自定义命令继承 <code>ArtisanCommand</code>，实现 <code>signature()</code> 和 <code>handle()</code> 方法，标注 <code>@Component</code> 后自动注册。</p>

<h2>命令定义</h2>
<pre><code>@Component
public class HelloCommand extends ArtisanCommand {

    @Override
    public String signature() {
        return "hello {name? : 你的名字}";
    }

    @Override
    public String description() {
        return "输出问候语，演示 Artisan 命令的基本用法";
    }

    @Override
    public int handle() {
        String name = argument("name", "World");
        info("Hello, " + name + "!");
        return 0;
    }
}</code></pre>

<h2>签名解析</h2>
<p>签名格式对齐 Laravel signature 语法：</p>
<pre><code>// 无参数
"hello"

// 带位置参数
"user:create {number} {name}"

// 带可选位置参数
"hello {name?}"

// 带选项参数
"migrate {--force}"

// 带默认值的选项
"user:create {number} {name} {--email= : 邮箱}"

// 混合
"make:migration {name} {--force}"</code></pre>

<h2>参数与选项访问</h2>
<pre><code>@Override
public int handle() {
    // 获取位置参数
    String number = argument("number");
    String name = argument("name", "默认值");

    // 获取选项参数
    String email = option("email");
    String email = option("email", "default@test.com");
    boolean hasEmail = hasOption("email");

    // 输出到控制台
    info("普通信息");
    warn("警告信息");
    error("错误信息");

    return 0;  // 0=成功, 非0=失败
}</code></pre>

<h2>运行方式</h2>
<p>方式一：命令行运行（通过 --artisan 参数）</p>
<pre><code># 列出所有可用命令
java -jar app.jar --artisan list

# 执行 hello 命令
java -jar app.jar --artisan hello
java -jar app.jar --artisan hello Alice

# 创建用户
java -jar app.jar --artisan user:create 1001 Alice
java -jar app.jar --artisan user:create 1001 Alice --email=alice@test.com</code></pre>

<p>方式二：通过 API 触发（演示用）</p>
<pre><code>curl http://localhost:8080/api/artisan/demo</code></pre>

<h2>本项目已注册的命令</h2>
<table>
    <tr><th>命令名</th><th>签名</th><th>说明</th></tr>
    <tr><td>hello</td><td>hello {name?}</td><td>输出问候语</td></tr>
    <tr><td>user:create</td><td>user:create {number} {name} {--email=}</td><td>创建用户</td></tr>
</table>

<h2>配置</h2>
<pre><code>jaravel:
  artisan:
    enabled: true    # 启用 Artisan（默认启用）
    # 代码生成配置（make:xxx）
    make:
      base-package: com.weacsoft.jaravel    # 生成类的基包名
      output-dir: src/main/java             # 输出根目录
      migration-dir: migrations             # 迁移文件目录</code></pre>

<h2>代码生成命令（make:xxx）</h2>
<p>对齐 Laravel <code>php artisan make:xxx</code>，支持一键生成 Controller/Middleware/Model/Migration/Command/Event/Listener 代码骨架。</p>

<h3>支持的命令</h3>
<table>
    <tr><th>命令</th><th>说明</th><th>输出位置</th></tr>
    <tr><td>make:controller {name}</td><td>生成 Controller 类</td><td>base-package.controller</td></tr>
    <tr><td>make:middleware {name}</td><td>生成 Middleware 类</td><td>base-package.middleware</td></tr>
    <tr><td>make:model {name}</td><td>生成 Model 类</td><td>base-package.model</td></tr>
    <tr><td>make:migration {name}</td><td>生成 Migration 类（含日期前缀）</td><td>base-package.migration</td></tr>
    <tr><td>make:command {name}</td><td>生成 ArtisanCommand 类</td><td>base-package.command</td></tr>
    <tr><td>make:event {name}</td><td>生成 Event 类</td><td>base-package.event</td></tr>
    <tr><td>make:listener {name} --event=XxxEvent</td><td>生成 Listener 类</td><td>base-package.listener</td></tr>
    <tr><td>make:all {name}</td><td>一键生成以上全部 7 个文件</td><td>各对应包</td></tr>
</table>

<h3>使用示例</h3>
<pre><code># 单独生成
java -jar app.jar --artisan make:controller UserController
java -jar app.jar --artisan make:model User
java -jar app.jar --artisan make:migration create_users_table
java -jar app.jar --artisan make:listener SendWelcomeEmail --event=UserRegisteredEvent

# 一键生成全部（以 User 为名生成 7 个文件）
java -jar app.jar --artisan make:all User

# 强制覆盖已存在文件
java -jar app.jar --artisan make:controller UserController --force</code></pre>

<h3>make:all 输出示例</h3>
<pre><code>===== make:all User =====

  [+] Controller created: com/weacsoft/jaravel/controller/UserController.java
  [+] Middleware created: com/weacsoft/jaravel/middleware/UserMiddleware.java
  [+] Model created: com/weacsoft/jaravel/model/UserModel.java
  [+] Migration created: com/weacsoft/jaravel/migration/Migration_2026_06_27_User.java
  [+] Command created: com/weacsoft/jaravel/command/UserCommand.java
  [+] Event created: com/weacsoft/jaravel/event/UserEvent.java
  [+] Listener created: com/weacsoft/jaravel/listener/UserListener.java

===== 完成: 7 成功, 0 跳过 =====</code></pre>

<h2>P2SP 树形拓扑</h2>
<p>远程插件执行支持树形中继转发。启用后，节点本地无插件时自动转发给子节点，三重防环机制（visitedNodes + maxHops + maxDepth）。</p>
<pre><code>jaravel:
  plugin-jar:
    remote:
      server:
        node-id: "node-root"       # 本节点 ID
        relay-enabled: true         # 启用中继转发
        max-hops: 5                # 最大跳数
      client:
        tree-routing-enabled: true  # 启用树形路由
        max-hops: 5</code></pre>

<div class="note">
    <strong>注意：</strong>ArtisanRunner 在应用启动时检测 --artisan 参数，若存在则执行对应命令后退出 JVM。无 --artisan 参数时不做任何操作，应用正常启动。通过 API 触发的命令不会退出 JVM。
</div>
@endsection
