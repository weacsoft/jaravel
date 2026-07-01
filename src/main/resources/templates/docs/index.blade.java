@extends('docs.layout')

@section('content')
<h1>Jaravel Demo</h1>
<p>Jaravel 是一个 Laravel 风格的 Java Web 框架，基于 Spring Boot 构建。本项目（jaravel-demo）展示了 jaravel 的全部能力，包括路由、ORM、认证、缓存、事件、Artisan 命令、定时任务、队列、微信 SDK 和插件系统。</p>

<h2>核心特性</h2>
<ul class="feature-list">
    <li><strong>Eloquent 合并 Model</strong> - 单一类同时承担实体定义与查询能力，无需拆分 Entity 和 Model</li>
    <li><strong>Laravel 风格目录结构</strong> - config/、routes/、database/ 与 app/ 同级，对齐 Laravel</li>
    <li><strong>路由系统</strong> - Api.java 和 Web.java 分别定义 API 和 Web 路由，支持分组与中间件</li>
    <li><strong>多 Guard / 多 Provider 认证</strong> - 支持 JWT 和 Session 两种 Guard 驱动</li>
    <li><strong>JWT 续期与登出</strong> - Token 自动续期、登出黑名单（基于 Cache）</li>
    <li><strong>多数据库支持</strong> - @DataSource 注解指定 Model 使用的数据源</li>
    <li><strong>事件系统</strong> - per-listener 队列（ShouldQueue），多命名队列独立线程池，失败自动重试</li>
    <li><strong>Artisan CLI</strong> - Laravel 风格命令行工具，支持参数解析与签名定义</li>
    <li><strong>定时任务</strong> - Cron 表达式、固定间隔任务，支持 Redis 分布式锁</li>
    <li><strong>队列任务</strong> - 异步监听器、数据库队列持久化、重试机制</li>
    <li><strong>缓存系统</strong> - Array/File/Redis 多驱动，支持 remember</li>
    <li><strong>Blade 模板引擎</strong> - @if/@foreach/@extends/@yield 等指令</li>
    <li><strong>迁移系统</strong> - Laravel 风格 Schema Builder，支持多数据库</li>
    <li><strong>微信 SDK</strong> - 公众号与小程序 API 封装，access_token 自动管理</li>
    <li><strong>插件系统</strong> - JAR 插件与 Java 文件插件，支持热更新</li>
</ul>

<h2>快速开始</h2>
<p>启动应用后，访问以下地址验证功能：</p>
<pre><code># Hello World
curl http://localhost:8080/api/hello

# Artisan 命令演示
curl http://localhost:8080/api/artisan/demo

# 定时任务状态
curl http://localhost:8080/api/schedule/status

# 队列任务演示
curl http://localhost:8080/api/queue/demo

# 微信 access_token 演示
curl http://localhost:8080/api/wechat/token</code></pre>

<h2>文档导航</h2>
<p>使用左侧导航栏浏览各功能模块的详细文档。每个页面包含代码示例、配置说明和注意事项。</p>

<div class="tip">
    <strong>提示：</strong>本项目同时提供 API 接口（/api/*）和文档页面（/docs/*）。API 接口返回 JSON，文档页面使用 jblade 模板引擎渲染。
</div>

<h2>纯前端文档包说明</h2>
<p>本文档页面使用 jblade 模板引擎（jaravel-vendor/jblade 模块）渲染，是纯前端文档包。所有文档页面位于 <code>src/main/resources/templates/docs/</code> 目录下，使用 <code>.blade.html</code> 后缀。文档内容包括：</p>
<ul>
    <li>项目介绍与特性列表</li>
    <li>各功能模块的使用指南</li>
    <li>Java 代码示例与 YAML 配置示例</li>
    <li>注意事项与最佳实践</li>
</ul>
<p>文档路由定义在 <code>routes/Web.java</code> 中，通过 <code>ResponseBuilder.view()</code> 渲染 jblade 模板。</p>
@endsection
