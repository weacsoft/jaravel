package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.plugin.jar.remote.client.RemoteExecutionDispatcher;
import com.weacsoft.jaravel.vendor.plugin.jar.remote.client.SubServerInfo;
import com.weacsoft.jaravel.vendor.plugin.jar.remote.server.RemotePluginServer;
import com.weacsoft.jaravel.vendor.plugin.jar.remote.server.RemoteServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程插件执行管理控制器。
 * <p>
 * 管理 plugin-jar-remote-server（P2SP 子节点）和 plugin-jar-remote-client（P2SP 主节点）的能力。
 */
@Controller
public class RemoteController implements Controllers {

    @Autowired(required = false)
    private RemoteExecutionDispatcher remoteExecutionDispatcher;

    @Autowired(required = false)
    private RemotePluginServer remotePluginServer;

    @Autowired(required = false)
    private RemoteServerProperties remoteServerProperties;

    /** 查看远程执行模块状态 */
    public Response status(Request request) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 服务端状态
        boolean serverEnabled = remotePluginServer != null;
        result.put("serverEnabled", serverEnabled);
        if (serverEnabled) {
            result.put("serverPort", remotePluginServer.getPort());
            result.put("serverRunning", remotePluginServer.isRunning());
        } else {
            result.put("serverMessage", "远程服务端未启动（设置 jaravel.plugin-jar.remote.server.enabled=true 启用）");
        }

        // 客户端状态
        boolean clientEnabled = remoteExecutionDispatcher != null;
        result.put("clientEnabled", clientEnabled);
        if (clientEnabled) {
            result.put("transportType", remoteExecutionDispatcher.getTransportType());
            result.put("subServerCount", remoteExecutionDispatcher.getSubServers().size());
            result.put("onlineSubServerCount", remoteExecutionDispatcher.getOnlineSubServers().size());
        } else {
            result.put("clientMessage", "远程客户端未启用（引入 plugin-jar-remote-client 依赖后自动启用）");
        }

        result.put("message", "远程插件执行 P2SP 架构状态");
        return ResponseBuilder.json(result);
    }

    /** 列出所有已注册的子服务器 */
    public Response listSubServers(Request request) {
        if (remoteExecutionDispatcher == null) {
            return ResponseBuilder.error(404, "远程客户端未启用");
        }

        List<SubServerInfo> servers = remoteExecutionDispatcher.getSubServers();
        List<Map<String, Object>> serverList = new ArrayList<>();
        for (SubServerInfo info : servers) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", info.getId());
            map.put("host", info.getHost());
            map.put("port", info.getPort());
            map.put("online", info.isOnline());
            map.put("hasAuthToken", info.getAuthToken() != null);
            map.put("lastHeartbeat", info.getLastHeartbeat());
            serverList.add(map);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subServers", serverList);
        result.put("total", serverList.size());
        result.put("online", remoteExecutionDispatcher.getOnlineSubServers().size());
        return ResponseBuilder.json(result);
    }

    /** 注册子服务器 */
    public Response registerSubServer(Request request) {
        if (remoteExecutionDispatcher == null) {
            return ResponseBuilder.error(404, "远程客户端未启用");
        }

        String id = request.input("id");
        String host = request.input("host");
        String portStr = request.input("port");
        String authToken = request.input("authToken");

        if (id == null || host == null || portStr == null) {
            return ResponseBuilder.error(400, "缺少 id / host / port 参数");
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return ResponseBuilder.error(400, "port 必须为整数");
        }

        SubServerInfo info = remoteExecutionDispatcher.registerSubServer(id, host, port, authToken);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", info.getId());
        result.put("host", info.getHost());
        result.put("port", info.getPort());
        result.put("status", "REGISTERED");
        result.put("message", "子服务器已注册，可调用 /api/remote/sub-servers/" + id + "/connect 连接");
        return ResponseBuilder.json(result);
    }

    /** 注销子服务器 */
    public Response unregisterSubServer(Request request) {
        if (remoteExecutionDispatcher == null) {
            return ResponseBuilder.error(404, "远程客户端未启用");
        }

        String id = request.routeParam("subServerId");
        boolean success = remoteExecutionDispatcher.unregisterSubServer(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("status", success ? "UNREGISTERED" : "NOT_FOUND");
        return ResponseBuilder.json(result);
    }

    /** 连接子服务器（启动远程模式） */
    public Response connectSubServer(Request request) {
        if (remoteExecutionDispatcher == null) {
            return ResponseBuilder.error(404, "远程客户端未启用");
        }

        String id = request.routeParam("subServerId");
        boolean success = remoteExecutionDispatcher.startRemoteMode(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("status", success ? "CONNECTED" : "CONNECT_FAILED");
        result.put("message", success ? "已连接到子服务器" : "连接失败（请检查地址/端口/认证）");
        return ResponseBuilder.json(result);
    }

    /** 断开子服务器连接 */
    public Response disconnectSubServer(Request request) {
        if (remoteExecutionDispatcher == null) {
            return ResponseBuilder.error(404, "远程客户端未启用");
        }

        String id = request.routeParam("subServerId");
        remoteExecutionDispatcher.stopRemoteMode(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("status", "DISCONNECTED");
        return ResponseBuilder.json(result);
    }
}
