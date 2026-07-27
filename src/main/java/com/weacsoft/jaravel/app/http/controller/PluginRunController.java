package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.service.PluginRunService;
import com.weacsoft.jaravel.vendor.auth.facade.Auth;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.plugin.jar.manager.HotPluginManager;
import com.weacsoft.jaravel.vendor.plugin.jar.model.PluginInfo;
import com.weacsoft.jaravel.vendor.plugin.java.manager.JavaFilePluginManager;
import com.weacsoft.jaravel.vendor.plugin.java.model.JavaFilePluginInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件运行控制器，提供 Java 源码在线编译执行和 Jar 插件反射调用。
 * <p>
 * 插件执行路由通过 Authenticate("api") + UserRoutePermissionMiddleware 中间件保护，
 * 控制器内不再手动校验权限，从 Auth.user() 获取当前用户。
 */
@Controller
public class PluginRunController implements Controllers {

    @Autowired
    private HotPluginManager jarPluginManager;

    @Autowired(required = false)
    private JavaFilePluginManager javaFilePluginManager;

    /**
     * 在线编译执行 Java 源码。
     * <p>
     * 请求参数：
     * <ul>
     *   <li>code（Java 源代码字符串）</li>
     *   <li>in_memory（是否纯内存编译，默认 true；false 时使用文件编译）</li>
     * </ul>
     * <p>
     * 内存编译使用 DynamicJavaCompiler（MemoryJavaFileManager）+ DynamicClassLoader，
     * 文件编译使用标准 javac + URLClassLoader，
     * 反射调用 run() 或 main() 方法，返回执行结果。
     */
    public Response runJava(Request request) {
        String code = request.input("code");
        if (code == null || code.isEmpty()) {
            return ResponseBuilder.error(400, "缺少 code 参数");
        }
        boolean inMemory = !"false".equalsIgnoreCase(request.input("in_memory", "true"));

        User user = (User) Auth.user();
        Map<String, Object> result = PluginRunService.runJava(code, inMemory);
        if (user != null) {
            result.put("user_id", user.getId());
        }
        return ResponseBuilder.json(result);
    }

    /**
     * 反射调用 Jar 插件方法。
     * <p>
     * 请求参数：
     * <ul>
     *   <li>jar_name（Jar 文件名）</li>
     *   <li>main_class（主类全限定名）</li>
     *   <li>method（方法名，默认 run）</li>
     *   <li>in_memory（是否纯内存加载，默认 true；false 时使用 URLClassLoader 文件加载）</li>
     * </ul>
     */
    public Response runJar(Request request) {
        String jarName = request.input("jar_name");
        String mainClass = request.input("main_class");
        String method = request.input("method", "run");
        boolean inMemory = !"false".equalsIgnoreCase(request.input("in_memory", "true"));

        Map<String, Object> result = PluginRunService.runJar(jarName, mainClass, method, inMemory);
        return ResponseBuilder.json(result);
    }

    /**
     * Java 插件系统状态（JDK 编译器是否可用、已注册的 Java 文件插件列表）。
     */
    public Response javaStatus(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compiler_available", PluginRunService.isCompilerAvailable());
        result.put("java_version", System.getProperty("java.version"));

        if (javaFilePluginManager != null) {
            List<JavaFilePluginInfo> plugins = javaFilePluginManager.getAllPlugins();
            List<Map<String, Object>> pluginList = new ArrayList<>();
            for (var info : plugins) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("pluginId", info.getPluginId());
                map.put("state", info.getState().name());
                map.put("errorMessage", info.getErrorMessage());
                pluginList.add(map);
            }
            result.put("plugins", pluginList);
            result.put("total", pluginList.size());
        } else {
            result.put("plugins", List.of());
            result.put("total", 0);
            result.put("message", "Java 文件插件系统未启用");
        }

        return ResponseBuilder.json(result);
    }

    /**
     * Jar 插件系统状态（已加载的 Jar 插件列表）。
     */
    public Response jarStatus(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<PluginInfo> plugins = jarPluginManager.getAllPlugins();
        List<Map<String, Object>> pluginList = new ArrayList<>();
        for (PluginInfo info : plugins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pluginId", info.getPluginId());
            map.put("version", info.getVersion());
            map.put("state", info.getState().name());
            map.put("persisted", info.isPersisted());
            map.put("routeCount", info.getRouteMappings() != null ? info.getRouteMappings().size() : 0);
            map.put("beanCount", info.getRegisteredBeanNames() != null ? info.getRegisteredBeanNames().size() : 0);
            pluginList.add(map);
        }
        result.put("plugins", pluginList);
        result.put("total", pluginList.size());
        return ResponseBuilder.json(result);
    }

    /**
     * 插件系统总览（公开接口，无需认证）。
     */
    public Response overview(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("java_compiler_available", PluginRunService.isCompilerAvailable());
        result.put("java_version", System.getProperty("java.version"));

        // Jar 插件统计
        List<PluginInfo> jarPlugins = jarPluginManager.getAllPlugins();
        long jarEnabled = jarPlugins.stream()
                .filter(p -> p.getState() == PluginInfo.State.ENABLED)
                .count();
        result.put("jar_plugin_total", jarPlugins.size());
        result.put("jar_plugin_enabled", jarEnabled);

        // Java 文件插件统计
        if (javaFilePluginManager != null) {
            List<JavaFilePluginInfo> javaPlugins = javaFilePluginManager.getAllPlugins();
            result.put("java_plugin_total", javaPlugins.size());
            result.put("java_plugin_system", "enabled");
        } else {
            result.put("java_plugin_total", 0);
            result.put("java_plugin_system", "disabled");
        }

        result.put("message", "多租户 Jar/Java 热更新在线运行平台");
        return ResponseBuilder.json(result);
    }
}
