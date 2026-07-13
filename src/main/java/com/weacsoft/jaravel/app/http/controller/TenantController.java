package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.plugin.jar.manager.HotPluginManager;
import com.weacsoft.jaravel.vendor.plugin.jar.model.PluginInfo;
import com.weacsoft.jaravel.vendor.plugin.jar.model.SharedInterfaceDescriptor;
import com.weacsoft.jaravel.vendor.plugin.jar.multitenant.TenantAwareHotPluginManager;
import com.weacsoft.jaravel.vendor.plugin.jar.multitenant.TenantContext;
import com.weacsoft.jaravel.vendor.plugin.jar.multitenant.TenantNaming;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多租户插件管理控制器。
 * <p>
 * 管理 plugin-jar-multi-tenant 模块的能力：同一 JAR 插件可按租户隔离地重复加载，
 * Bean 名称和路由路径自动按租户前缀化。
 */
@Controller
public class TenantController implements Controllers {

    @Autowired
    private HotPluginManager jarPluginManager;

    /** 查看多租户模块状态和当前租户上下文 */
    public Response status(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean multiTenantEnabled = jarPluginManager instanceof TenantAwareHotPluginManager;
        result.put("multiTenantEnabled", multiTenantEnabled);
        result.put("managerClass", jarPluginManager.getClass().getSimpleName());
        result.put("currentTenant", TenantContext.getCurrentTenant());

        if (multiTenantEnabled) {
            TenantAwareHotPluginManager tam = (TenantAwareHotPluginManager) jarPluginManager;
            result.put("separator", tam.getSeparator());
        }
        result.put("message", multiTenantEnabled
                ? "多租户插件模式已激活，可使用 /api/multi-tenant/* 系列接口"
                : "多租户插件模式未激活（未引入 plugin-jar-multi-tenant 依赖或已禁用）");
        return ResponseBuilder.json(result);
    }

    /** 列出指定租户的所有插件 */
    public Response listByTenant(Request request) {
        String tenantId = request.routeParam("tenantId");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);

        if (!(jarPluginManager instanceof TenantAwareHotPluginManager tam)) {
            return ResponseBuilder.error(400, "多租户插件模式未激活");
        }

        List<PluginInfo> plugins = tam.getPluginsByTenant(tenantId);
        List<Map<String, Object>> pluginList = new ArrayList<>();
        for (PluginInfo info : plugins) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("pluginId", info.getPluginId());
            map.put("state", info.getState().name());
            map.put("version", info.getVersion());
            map.put("routeCount", info.getRouteMappings() != null ? info.getRouteMappings().size() : 0);
            pluginList.add(map);
        }
        result.put("plugins", pluginList);
        result.put("total", pluginList.size());
        return ResponseBuilder.json(result);
    }

    /** 为指定租户注册插件（从已上传的 JAR 文件加载） */
    public Response registerForTenant(Request request) {
        String tenantId = request.routeParam("tenantId");
        String jarPath = request.input("jarPath");
        String pluginId = request.input("pluginId", "custom-plugin");

        if (!(jarPluginManager instanceof TenantAwareHotPluginManager tam)) {
            return ResponseBuilder.error(400, "多租户插件模式未激活");
        }
        if (jarPath == null || jarPath.isEmpty()) {
            return ResponseBuilder.error(400, "缺少 jarPath 参数");
        }

        java.nio.file.Path path = java.nio.file.Paths.get(jarPath);
        if (!java.nio.file.Files.exists(path)) {
            return ResponseBuilder.error(404, "JAR 文件不存在: " + jarPath);
        }

        String fullPluginId = tam.registerPluginForTenant(path, tenantId, pluginId, true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("pluginId", pluginId);
        result.put("fullPluginId", fullPluginId);
        result.put("status", "REGISTERED");
        result.put("message", "插件已为租户 " + tenantId + " 注册，Bean/路由将自动前缀化");
        return ResponseBuilder.json(result);
    }

    /**
     * 上传 JAR 文件并为指定租户注册插件（通用接口上传）。
     * <p>
     * 上传 JAR 文件后自动注册为租户插件，Bean 名称和路由路径自动按租户前缀化，
     * 其他应用可通过 /{tenantId}/... 路径跨应用调用。
     */
    public Response uploadAndRegister(Request request) {
        String tenantId = request.routeParam("tenantId");
        String pluginId = request.input("pluginId", "custom-plugin");

        if (!(jarPluginManager instanceof TenantAwareHotPluginManager tam)) {
            return ResponseBuilder.error(400, "多租户插件模式未激活");
        }

        MultipartFile file = request.file("file");
        if (file == null || file.isEmpty()) {
            return ResponseBuilder.error(400, "缺少 file 参数（请上传 JAR 文件）");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "plugin.jar";
        if (!originalFilename.endsWith(".jar")) {
            return ResponseBuilder.error(400, "仅支持 .jar 文件");
        }

        // 如果未指定 pluginId，则用文件名
        if ("custom-plugin".equals(pluginId)) {
            pluginId = originalFilename.replace(".jar", "");
        }

        try {
            // 保存到临时文件
            Path tempFile = Files.createTempFile("tenant-upload-", ".jar");
            file.transferTo(tempFile.toFile());

            // 为租户注册插件
            String fullPluginId = tam.registerPluginForTenant(tempFile, tenantId, pluginId, true);

            // 删除临时文件（已加载到内存）
            Files.deleteIfExists(tempFile);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("tenantId", tenantId);
            result.put("pluginId", pluginId);
            result.put("fullPluginId", fullPluginId);
            result.put("status", "UPLOADED_AND_REGISTERED");
            result.put("message", "JAR 文件已上传并为租户 " + tenantId + " 注册，路由前缀：/" + tenantId + "/...");
            return ResponseBuilder.json(result);
        } catch (IOException e) {
            return ResponseBuilder.error(500, "上传失败: " + e.getMessage());
        }
    }

    /** 启用指定租户的插件 */
    public Response enableForTenant(Request request) {
        String tenantId = request.routeParam("tenantId");
        String pluginId = request.routeParam("pluginId");

        if (!(jarPluginManager instanceof TenantAwareHotPluginManager tam)) {
            return ResponseBuilder.error(400, "多租户插件模式未激活");
        }

        boolean success = tam.enablePluginForTenant(tenantId, pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("pluginId", pluginId);
        result.put("status", success ? "ENABLED" : "ENABLE_FAILED");

        if (success) {
            PluginInfo info = tam.getPluginForTenant(tenantId, pluginId);
            result.put("routeMappings", info != null ? info.getRouteMappings() : List.of());
            result.put("registeredBeans", info != null ? info.getRegisteredBeanNames() : List.of());
            result.put("message", "插件已启用，路由自动添加租户前缀 /" + tenantId + "/...");
        }
        return ResponseBuilder.json(result);
    }

    /** 禁用指定租户的插件 */
    public Response disableForTenant(Request request) {
        String tenantId = request.routeParam("tenantId");
        String pluginId = request.routeParam("pluginId");

        if (!(jarPluginManager instanceof TenantAwareHotPluginManager tam)) {
            return ResponseBuilder.error(400, "多租户插件模式未激活");
        }

        boolean success = tam.disablePluginForTenant(tenantId, pluginId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("pluginId", pluginId);
        result.put("status", success ? "DISABLED" : "DISABLE_FAILED");
        return ResponseBuilder.json(result);
    }

    /** 演示命名规则：展示 Bean 名称和路由路径的前缀化效果 */
    public Response namingDemo(Request request) {
        String tenantId = request.query("tenant", "demoTenant");
        String beanName = request.query("bean", "userController");
        String routePath = request.query("path", "/api/users");
        String separator = request.query("separator", "@");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenantId", tenantId);
        result.put("separator", separator);
        result.put("originalBeanName", beanName);
        result.put("prefixedBeanName", TenantNaming.prefixBeanName(tenantId, beanName));
        result.put("originalRoutePath", routePath);
        result.put("prefixedRoutePath", TenantNaming.prefixRoutePath(tenantId, routePath));
        result.put("fullPluginId", TenantNaming.buildPluginId(tenantId, "blog", separator));
        result.put("message", "多租户命名规则演示：Bean 名称和路由路径自动按租户前缀化");
        return ResponseBuilder.json(result);
    }

    /**
     * 注册共享接口（全手动指定）。
     * <p>
     * 用字符串指定插件中的哪个 Bean 的哪个方法作为共享接口，
     * 开发时无需包含目标类，运行时通过反射调用。
     * <p>
     * POST /api/multi-tenant/shared-interfaces/register
     * body: {interfaceName, pluginId, beanName, methodName, description?}
     */
    public Response registerSharedInterface(Request request) {
        String interfaceName = request.input("interfaceName");
        String pluginId = request.input("pluginId");
        String beanName = request.input("beanName");
        String methodName = request.input("methodName");
        String description = request.input("description", "");

        if (interfaceName.isEmpty() || pluginId.isEmpty()
                || beanName.isEmpty() || methodName.isEmpty()) {
            return ResponseBuilder.error(400, "缺少必要参数: interfaceName, pluginId, beanName, methodName");
        }

        boolean success = jarPluginManager.registerSharedInterface(
                interfaceName, pluginId, beanName, methodName, description);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("interfaceName", interfaceName);
        result.put("pluginId", pluginId);
        result.put("beanName", beanName);
        result.put("methodName", methodName);
        result.put("success", success);
        result.put("status", success ? "REGISTERED" : "REGISTER_FAILED");
        if (!success) {
            result.put("message", "注册失败，请检查插件是否已启用且Bean已注册");
        }
        return ResponseBuilder.json(result);
    }

    /**
     * 调用共享接口。
     * <p>
     * 通过共享接口名称反射调用方法，请求参数和返回参数都用 Map 表示。
     * <p>
     * POST /api/multi-tenant/shared-interfaces/{name}/invoke
     * body: 请求参数 Map
     */
    public Response invokeSharedInterface(Request request) {
        String interfaceName = request.routeParam("name");
        if (interfaceName == null || interfaceName.isEmpty()) {
            return ResponseBuilder.error(400, "缺少路由参数: name");
        }

        Map<String, Object> args = request.all();
        try {
            Map<String, Object> result = jarPluginManager.invokeSharedInterface(interfaceName, args);
            return ResponseBuilder.json(result);
        } catch (IllegalStateException e) {
            return ResponseBuilder.error(400, e.getMessage());
        }
    }

    /**
     * 列出所有已注册的共享接口。
     * <p>
     * GET /api/multi-tenant/shared-interfaces
     */
    public Response listSharedInterfaces(Request request) {
        List<SharedInterfaceDescriptor> interfaces = jarPluginManager.getSharedInterfaces();
        List<Map<String, Object>> list = new ArrayList<>();
        for (SharedInterfaceDescriptor desc : interfaces) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("interfaceName", desc.getInterfaceName());
            map.put("pluginId", desc.getPluginId());
            map.put("beanName", desc.getBeanName());
            map.put("methodName", desc.getMethodName());
            map.put("description", desc.getDescription());
            list.add(map);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sharedInterfaces", list);
        result.put("total", list.size());
        return ResponseBuilder.json(result);
    }
}
