package demo.greeting;

import com.weacsoft.jaravel.vendor.plugin.jar.annotation.HttpMethod;
import com.weacsoft.jaravel.vendor.plugin.jar.annotation.PluginComponent;
import com.weacsoft.jaravel.vendor.plugin.jar.annotation.PluginMapping;
import com.weacsoft.jaravel.vendor.plugin.jar.annotation.PluginRoute;

@PluginComponent("demoGreetingService")
public class GreetingPlugin {

    // 自动注册路由（auto-register=true 模式下自动注册）
    @PluginMapping(path = "/api/plugin/greeting", method = HttpMethod.GET)
    public String greeting(String name) {
        return "Hello, " + (name != null ? name : "World") + "! (from java-file-plugin)";
    }

    // 自动注册路由
    @PluginMapping(path = "/api/plugin/time", method = HttpMethod.GET)
    public String time() {
        return "Current time: " + java.time.LocalDateTime.now().toString() + " (from java-file-plugin)";
    }

    // 可注册路由（manual-register 模式下需要手动注册）
    @PluginRoute(path = "/api/plugin/manual-greeting", method = HttpMethod.GET)
    public String manualGreeting(String name) {
        return "Manual Hello, " + (name != null ? name : "World") + "! (from java-file-plugin)";
    }

    // 可注册路由（manual-register 模式下需要手动注册）
    @PluginRoute(path = "/api/plugin/info", method = HttpMethod.GET)
    public String info() {
        return "Plugin info: demo-greeting v1.0 (from java-file-plugin)";
    }
}
