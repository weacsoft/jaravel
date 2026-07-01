package com.weacsoft.jaravel.app.console;

import com.weacsoft.jaravel.vendor.artisan.ArtisanCommand;
import org.springframework.stereotype.Component;

/**
 * Hello 命令，对齐 Laravel Artisan 命令。
 * <p>
 * 签名：{@code hello {name? : 你的名字}}
 * <p>
 * 用法：
 * <pre>
 * java -jar app.jar --artisan hello
 * java -jar app.jar --artisan hello Alice
 * </pre>
 */
@Component
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
        // 获取可选位置参数 name，不存在时使用默认值
        String name = argument("name", "World");

        info("========================================");
        info("  Hello, " + name + "!");
        info("  Welcome to Jaravel Demo Project.");
        info("  当前时间: " + java.time.LocalDateTime.now());
        info("========================================");

        return 0;
    }
}
