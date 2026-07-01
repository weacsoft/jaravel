@extends('docs.layout')

@section('content')
<h1>插件系统</h1>
<p>Jaravel 提供两种插件系统：JAR 插件（plugin-jar-core）和 Java 文件插件（plugin-java-core），均支持动态加载、卸载和热更新。</p>

<h2>JAR 插件</h2>
<p>JAR 插件以独立 JAR 包形式提供，支持动态上传、启用、禁用和路由注册。采用三级 ClassLoader 隔离机制，插件间互不影响。</p>

<h3>配置</h3>
<pre><code>jaravel:
  plugin-jar:
    enabled: true
    plugins-dir: plugins
    auto-restore: true       # 重启后自动恢复已启用的插件
    auto-register: true      # true=自动注册@PluginMapping, false=手动注册</code></pre>

<h3>编写 JAR 插件</h3>
<pre><code>@PluginComponent("myService")
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
}</code></pre>

<h3>JAR 插件管理 API</h3>
<pre><code># 列出所有 JAR 插件
curl http://localhost:8080/api/plugins/jar

# 上传 JAR 插件
curl -X POST http://localhost:8080/api/plugins/jar/upload -F "file=@my-plugin.jar"

# 启用插件
curl -X POST http://localhost:8080/api/plugins/jar/my-plugin/enable

# 禁用插件
curl -X POST http://localhost:8080/api/plugins/jar/my-plugin/disable</code></pre>

<h2>Java 文件插件</h2>
<p>Java 文件插件以 .java 源文件形式提供，支持动态编译和热更新。修改 .java 文件后可通过 API 热重载，无需重启应用。</p>

<h3>配置</h3>
<pre><code>jaravel:
  plugin-java:
    enabled: true
    source-dir: plugins-java
    auto-scan: true          # 启动时自动扫描 .java 文件插件
    auto-register: true      # true=自动注册@PluginMapping, false=手动注册</code></pre>

<h3>演示插件</h3>
<p>演示插件位于 <code>plugins-java/demo-greeting/GreetingPlugin.java</code>，包含：</p>
<ul>
    <li><code>@PluginMapping</code> 自动注册路由：<code>/api/plugin/greeting</code>、<code>/api/plugin/time</code></li>
    <li><code>@PluginRoute</code> 可注册路由：<code>/api/plugin/manual-greeting</code>、<code>/api/plugin/info</code></li>
</ul>

<h3>热更新</h3>
<pre><code># 列出所有 Java 文件插件
curl http://localhost:8080/api/plugins/java

# 热重载指定插件（修改 .java 文件后执行）
curl -X POST http://localhost:8080/api/plugins/java/demo-greeting/reload

# 重载所有有变更的插件
curl -X POST http://localhost:8080/api/plugins/java/reload-all</code></pre>

<h2>路由注册模式</h2>
<p>插件系统支持两种路由注册模式，通过 <code>auto-register</code> 配置控制：</p>
<table>
    <tr><th>模式</th><th>@PluginMapping</th><th>@PluginRoute</th><th>说明</th></tr>
    <tr><td>自动注册（默认）</td><td>自动注册</td><td>列为可用</td><td>插件启用时自动注册</td></tr>
    <tr><td>手动注册</td><td>列为可用</td><td>列为可用</td><td>所有路由需手动注册</td></tr>
</table>

<h2>可用路由管理</h2>
<pre><code># 列出可注册路由（manual-register 模式）
curl http://localhost:8080/api/plugins/java/demo-greeting/available-routes

# 手动注册可注册路由
curl -X POST "http://localhost:8080/api/plugins/java/demo-greeting/available-routes/register?path=/api/plugin/manual-greeting&method=GET"

# 测试手动注册的路由
curl http://localhost:8080/api/plugin/manual-greeting?name=world</code></pre>

<div class="warn">
    <strong>警告：</strong>插件管理 API（上传、启用、禁用、热重载等）仅用于演示，生产环境不应暴露这些 REST API。请在内网管理后台或通过运维工具操作插件。
</div>

<div class="note">
    <strong>注意：</strong>JAR 插件采用三级 ClassLoader 隔离（AppClassLoader -> PluginClassLoader -> PluginInnerClassLoader），插件间类隔离，避免依赖冲突。Java 文件插件通过内存编译器动态编译 .java 文件，修改后热重载无需重启 JVM。
</div>
@endsection
