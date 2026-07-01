@extends('docs.layout')

@section('content')
<h1>缓存</h1>
<p>Jaravel 提供对齐 Laravel Cache 门面的缓存系统，支持 Array（内存）、File（文件）和 Redis 三种驱动。</p>

<h2>基本操作</h2>
<pre><code>// 写入缓存（60 秒 TTL）
Cache.put("key", "value", 60);

// 读取缓存
String value = Cache.get("key");

// 判断缓存是否存在
boolean has = Cache.has("key");

// 删除缓存
Cache.forget("key");

// 自增
Cache.increment("counter");        // +1
Cache.increment("counter", 5);     // +5</code></pre>

<h2>remember（带 TTL 的缓存闭包）</h2>
<p>对齐 Laravel <code>Cache::remember()</code>，首次调用执行闭包并缓存结果，后续调用直接返回缓存值：</p>
<pre><code>Object result = Cache.remember("cfg", 300, () -> {
    return loadConfig();  // 5 分钟内不会重复执行
});</code></pre>

<h2>驱动配置</h2>
<pre><code>jaravel:
  cache:
    default-store: array    # array（内存，单机）/ file（文件，持久化）/ redis
    prefix: jaravel</code></pre>

<h2>Redis 缓存驱动</h2>
<p>当 Redis 可用时，可切换为 Redis 驱动实现多机缓存同步：</p>
<pre><code>jaravel:
  cache:
    default-store: redis
    prefix: jaravel
    redis:
      connection: cache
  redis:
    connections:
      cache:
        host: 127.0.0.1
        port: 6379
        database: 1</code></pre>

<h2>演示路由</h2>
<p>访问 <code>/api/cache-demo</code> 查看 Cache 功能演示：</p>
<pre><code>curl http://localhost:8080/api/cache-demo</code></pre>

<p>演示内容包括：</p>
<ul>
    <li>put + get 基本读写</li>
    <li>has 判断存在</li>
    <li>increment 自增</li>
    <li>remember 缓存闭包（含命中验证）</li>
    <li>forget 删除</li>
</ul>

<div class="note">
    <strong>注意：</strong>Array 驱动为内存缓存，应用重启后丢失。生产环境建议使用 Redis 或 File 驱动。Redis 驱动需要先配置 jaravel.redis.connections。
</div>

<div class="tip">
    <strong>提示：</strong>Cache 门面通过 SpringContext 获取 CacheManager Bean，所有操作均为静态方法调用，对齐 Laravel 的 Cache::put() / Cache::get() 风格。
</div>
@endsection
