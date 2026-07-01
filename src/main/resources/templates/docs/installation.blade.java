@extends('docs.layout')

@section('content')
<h1>安装指南</h1>
<p>本页介绍如何从零开始构建和运行 jaravel-demo 项目。</p>

<h2>前置要求</h2>
<ul>
    <li><strong>JDK 17+</strong> - 推荐 JDK 17 或 JDK 21</li>
    <li><strong>Maven 3.6+</strong> - 用于构建项目</li>
    <li><strong>Redis</strong>（可选）- 用于分布式锁、缓存同步、Session 共享</li>
    <li><strong>MySQL / SQLite</strong> - SQLite 为默认数据库（零配置），MySQL 为可选</li>
</ul>

<h2>构建步骤</h2>

<h3>1. 构建 jaravel-vendor</h3>
<p>jaravel-vendor 是框架核心，需要先安装到本地 Maven 仓库：</p>
<pre><code>cd jaravel-vendor
mvn install -DskipTests</code></pre>

<h3>2. 构建 jaravel-demo</h3>
<pre><code>cd jaravel
mvn clean package -DskipTests</code></pre>

<h3>3. 运行项目</h3>
<p>方式一：Maven 直接运行</p>
<pre><code>cd jaravel
mvn spring-boot:run</code></pre>

<p>方式二：运行打包后的 JAR</p>
<pre><code>java -jar target/jaravel-demo-0.1.0.jar</code></pre>

<p>方式三：运行 Artisan 命令</p>
<pre><code># 列出所有可用命令
java -jar target/jaravel-demo-0.1.0.jar --artisan list

# 执行 hello 命令
java -jar target/jaravel-demo-0.1.0.jar --artisan hello Alice

# 创建用户
java -jar target/jaravel-demo-0.1.0.jar --artisan user:create 1001 Alice --email=alice@test.com</code></pre>

<h2>运行方法</h2>
<p>应用启动后自动执行以下操作：</p>
<ol>
    <li><strong>数据库迁移</strong> - 创建 users 表（主数据库）和 products 表（第二数据库）</li>
    <li><strong>定时任务注册</strong> - 注册缓存清理和日报生成任务</li>
    <li><strong>事件监听器注册</strong> - 自动扫描 @ListensTo 注解的监听器</li>
    <li><strong>插件系统初始化</strong> - 扫描并加载 JAR 和 Java 文件插件</li>
    <li><strong>启动 Web 服务</strong> - 监听 http://localhost:8080</li>
</ol>

<h2>验证安装</h2>
<pre><code># 基础验证
curl http://localhost:8080/api/hello

# 文档页面
open http://localhost:8080/docs</code></pre>

<div class="note">
    <strong>注意：</strong>如果 Redis 未安装，应用仍可正常启动。Redis 相关功能（分布式锁、缓存同步、Session 共享）会自动降级为内存模式。
</div>

<h2>配置文件</h2>
<p>主要配置文件位于 <code>src/main/resources/application.yml</code>，环境变量配置参考 <code>.env.example</code>。</p>
<pre><code>server:
  port: 8080

spring:
  datasource:
    url: jdbc:sqlite:database1.sqlite
    driver-class-name: org.sqlite.JDBC

jaravel:
  auth:
    default-guard: api
  jwt:
    secret: jaravel-secret-key-change-in-production-must-be-32-bytes
    ttl: 3600000
  cache:
    default-store: array
  schedule:
    enabled: true
  wechat:
    enabled: true
    official-accounts:
      default:
        app-id: your-app-id
        secret: your-secret</code></pre>

<div class="tip">
    <strong>提示：</strong>生产环境请务必修改 JWT secret、数据库连接字符串和微信 appId/secret 等敏感配置。
</div>
@endsection
