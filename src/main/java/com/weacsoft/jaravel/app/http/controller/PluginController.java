package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.plugin.jar.annotation.HttpMethod;
import com.weacsoft.jaravel.vendor.plugin.jar.manager.HotPluginManager;
import com.weacsoft.jaravel.vendor.plugin.jar.model.PluginInfo;
import com.weacsoft.jaravel.vendor.plugin.jar.model.RouteInfo;
import com.weacsoft.jaravel.vendor.plugin.java.autoconfigure.PluginJavaProperties;
import com.weacsoft.jaravel.vendor.plugin.java.manager.JavaFilePluginManager;
import com.weacsoft.jaravel.vendor.plugin.java.model.JavaFilePluginInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 插件管理控制器，提供 Jar 插件和 Java 文件插件的管理能力。
 * <p>
 * 包括插件列表、上传、启用/禁用、热重载、路由注册等管理操作。
 * 所有管理路由通过 Authenticate("admin") + RoutePermissionMiddleware 保护。
 */
@Controller
public class PluginController implements Controllers {

    @Autowired
    private HotPluginManager jarPluginManager;

    @Autowired(required = false)
    private JavaFilePluginManager javaFilePluginManager;

    @Autowired(required = false)
    private PluginJavaProperties pluginJavaProperties;

    // ===== JAR 插件管理 =====

    /** 列出所有 JAR 插件 */
    public Response listJarPlugins(Request request) {
        List<PluginInfo> plugins = jarPluginManager.getAllPlugins();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PluginInfo info : plugins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pluginId", info.getPluginId());
            map.put("version", info.getVersion());
            map.put("state", info.getState().name());
            map.put("persisted", info.isPersisted());
            map.put("routeCount", info.getRouteMappings() != null ? info.getRouteMappings().size() : 0);
            map.put("beanCount", info.getRegisteredBeanNames() != null ? info.getRegisteredBeanNames().size() : 0);
            map.put("errorMessage", info.getErrorMessage());
            result.add(map);
        }
        return ResponseBuilder.json(Map.of("plugins", result, "total", result.size()));
    }

    /** 上传 JAR 插件（落盘持久化） */
    public Response uploadJarPlugin(Request request) {
        try {
            MultipartFile file = request.file("file");
            if (file == null) {
                return ResponseBuilder.error(400, "缺少 file 参数");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) originalFilename = "plugin.jar";
            String pluginId = originalFilename.replace(".jar", "");

            Path tempFile = Files.createTempFile("plugin-upload-", ".jar");
            file.transferTo(tempFile.toFile());

            String registeredId = jarPluginManager.registerPluginFromPath(tempFile, pluginId, true);
            Files.deleteIfExists(tempFile);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pluginId", registeredId);
            result.put("status", "UPLOADED");
            result.put("persisted", true);
            return ResponseBuilder.json(result);
        } catch (IOException e) {
            return ResponseBuilder.error(500, "上传失败: " + e.getMessage());
        }
    }

    /** 启用 JAR 插件 */
    public Response enableJarPlugin(Request request) {
        String pluginId = request.routeParam("pluginId");
        boolean success = jarPluginManager.enablePlugin(pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("status", success ? "ENABLED" : "ENABLE_FAILED");
        if (success) {
            PluginInfo info = jarPluginManager.getPlugin(pluginId);
            result.put("routeMappings", info != null ? info.getRouteMappings() : Collections.emptyList());
            result.put("registeredBeans", info != null ? info.getRegisteredBeanNames() : Collections.emptySet());
            return ResponseBuilder.json(result);
        } else {
            PluginInfo info = jarPluginManager.getPlugin(pluginId);
            String errMsg = info != null ? info.getErrorMessage() : "Plugin not found";
            return ResponseBuilder.error(400, "启用失败: " + errMsg);
        }
    }

    /** 禁用 JAR 插件 */
    public Response disableJarPlugin(Request request) {
        String pluginId = request.routeParam("pluginId");
        boolean success = jarPluginManager.disablePlugin(pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("status", success ? "DISABLED" : "DISABLE_FAILED");
        return ResponseBuilder.json(result);
    }

    /** 手动注册路由 */
    public Response registerRoute(Request request) {
        String pluginId = request.routeParam("pluginId");
        String path = request.input("path");
        String method = request.input("method", "GET");
        String beanName = request.input("beanName");
        String methodName = request.input("methodName");

        RouteInfo route = new RouteInfo();
        route.setPath(path);
        route.setMethod(HttpMethod.valueOf(method));
        route.setBeanName(beanName);
        route.setMethodName(methodName);
        route.setProduces("application/json");

        boolean success = jarPluginManager.registerRoute(pluginId, route);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("route", route.toString());
        result.put("status", success ? "REGISTERED" : "REGISTER_FAILED");
        return ResponseBuilder.json(result);
    }

    /** 手动注销路由 */
    public Response unregisterRoute(Request request) {
        String pluginId = request.routeParam("pluginId");
        String path = request.input("path");
        String method = request.input("method", "GET");
        boolean success = jarPluginManager.unregisterRoute(pluginId, path, method);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("path", path);
        result.put("method", method);
        result.put("status", success ? "UNREGISTERED" : "UNREGISTER_FAILED");
        return ResponseBuilder.json(result);
    }

    // ===== Java 文件插件管理 =====

    /** 列出所有 Java 文件插件 */
    public Response listJavaPlugins(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        List<JavaFilePluginInfo> plugins = javaFilePluginManager.getAllPlugins();
        List<Map<String, Object>> result = new ArrayList<>();
        for (JavaFilePluginInfo info : plugins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pluginId", info.getPluginId());
            map.put("state", info.getState().name());
            map.put("sourceFiles", info.getSourceFiles());
            map.put("componentClasses", info.getComponentClasses());
            map.put("routeMappings", info.getRouteMappings());
            map.put("errorMessage", info.getErrorMessage());
            result.add(map);
        }
        return ResponseBuilder.json(Map.of("plugins", result, "total", result.size()));
    }

    /** 注册 Java 文件插件（从指定目录） */
    public Response registerJavaPlugin(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        String dirName = request.input("dir");
        if (dirName == null || dirName.isEmpty()) {
            return ResponseBuilder.error(400, "缺少 dir 参数");
        }
        // JavaFilePluginManager 未暴露 getSourceDir()，通过 PluginJavaProperties 获取配置的源目录
        String sourceDir = pluginJavaProperties != null ? pluginJavaProperties.getSourceDir() : "plugins-java";
        Path pluginDir = Paths.get(sourceDir, dirName);
        if (!Files.isDirectory(pluginDir)) {
            return ResponseBuilder.error(404, "目录不存在: " + dirName);
        }
        String pluginId = javaFilePluginManager.registerPlugin(pluginDir);
        boolean enabled = javaFilePluginManager.enablePlugin(pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("status", enabled ? "ENABLED" : "REGISTERED_BUT_ENABLE_FAILED");
        return ResponseBuilder.json(result);
    }

    /** 热重载 Java 文件插件 */
    public Response reloadJavaPlugin(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        String pluginId = request.routeParam("pluginId");
        boolean success = javaFilePluginManager.reloadPlugin(pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("status", success ? "RELOADED" : "RELOAD_FAILED");
        return ResponseBuilder.json(result);
    }

    /** 检查并重载所有有变更的 Java 文件插件 */
    public Response reloadAllChanged(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        javaFilePluginManager.reloadAllChanged();
        return ResponseBuilder.json(Map.of("message", "已检查并重载所有变更的 Java 文件插件"));
    }

    /** 禁用 Java 文件插件 */
    public Response disableJavaPlugin(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        String pluginId = request.routeParam("pluginId");
        boolean success = javaFilePluginManager.disablePlugin(pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("status", success ? "DISABLED" : "DISABLE_FAILED");
        return ResponseBuilder.json(result);
    }

    // ===== 可用路由（manual-register 模式） =====

    /** 列出 JAR 插件的可注册路由 */
    public Response listAvailableJarRoutes(Request request) {
        String pluginId = request.routeParam("pluginId");
        var routes = jarPluginManager.getAvailableRoutes(pluginId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var route : routes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("path", route.getPath());
            map.put("method", route.getMethod().name());
            map.put("beanName", route.getBeanName());
            map.put("methodName", route.getMethodName());
            map.put("produces", route.getProduces());
            result.add(map);
        }
        return ResponseBuilder.json(Map.of("pluginId", pluginId, "availableRoutes", result));
    }

    /** 手动注册 JAR 插件的可注册路由 */
    public Response registerAvailableJarRoute(Request request) {
        String pluginId = request.routeParam("pluginId");
        String path = request.input("path");
        String method = request.input("method", "GET");
        boolean success = jarPluginManager.registerAvailableRoute(pluginId, path, method);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("path", path);
        result.put("method", method);
        result.put("status", success ? "REGISTERED" : "REGISTER_FAILED");
        return ResponseBuilder.json(result);
    }

    /** 列出 Java 文件插件的可注册路由 */
    public Response listAvailableJavaRoutes(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        String pluginId = request.routeParam("pluginId");
        var routes = javaFilePluginManager.getAvailableRoutes(pluginId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (var route : routes) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("path", route.getPath());
            map.put("method", route.getMethod().name());
            map.put("beanName", route.getBeanName());
            map.put("methodName", route.getMethodName());
            map.put("produces", route.getProduces());
            result.add(map);
        }
        return ResponseBuilder.json(Map.of("pluginId", pluginId, "availableRoutes", result));
    }

    /** 手动注册 Java 文件插件的可注册路由 */
    public Response registerAvailableJavaRoute(Request request) {
        if (javaFilePluginManager == null) {
            return ResponseBuilder.error(404, "Java 文件插件系统未启用");
        }
        String pluginId = request.routeParam("pluginId");
        String path = request.input("path");
        String method = request.input("method", "GET");
        boolean success = javaFilePluginManager.registerAvailableRoute(pluginId, path, method);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pluginId", pluginId);
        result.put("path", path);
        result.put("method", method);
        result.put("status", success ? "REGISTERED" : "REGISTER_FAILED");
        return ResponseBuilder.json(result);
    }
}
