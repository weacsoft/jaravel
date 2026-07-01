package com.weacsoft.jaravel.app.console;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.service.UserService;
import com.weacsoft.jaravel.vendor.artisan.ArtisanCommand;
import org.springframework.stereotype.Component;

/**
 * 创建用户命令，对齐 Laravel Artisan 命令。
 * <p>
 * 签名：{@code user:create {number} {name} {--email= : 邮箱}}
 * <p>
 * 用法：
 * <pre>
 * java -jar app.jar --artisan user:create 1001 Alice
 * java -jar app.jar --artisan user:create 1001 Alice --email=alice@test.com
 * </pre>
 * <p>
 * 调用 {@link UserService#register(String, String, String, String)} 创建用户，
 * 注册成功后自动分发 {@code UserRegisteredEvent} 事件（触发日志记录与欢迎邮件监听器）。
 */
@Component
public class CreateUserCommand extends ArtisanCommand {

    @Override
    public String signature() {
        return "user:create {number} {name} {--email= : 邮箱}";
    }

    @Override
    public String description() {
        return "通过命令行创建用户，演示 Artisan 命令调用 Service 层";
    }

    @Override
    public int handle() {
        // 获取必填位置参数
        String number = argument("number");
        String name = argument("name");

        if (number == null || number.isEmpty()) {
            error("缺少必填参数: number（工号）");
            return 1;
        }
        if (name == null || name.isEmpty()) {
            error("缺少必填参数: name（姓名）");
            return 1;
        }

        // 获取可选选项参数 email
        String email = hasOption("email") ? option("email", "") : "";
        if (email != null && email.isEmpty()) {
            email = null;
        }

        // 演示用默认密码，生产环境应通过交互式输入或加密生成
        String defaultPassword = "123456";

        info("正在创建用户...");
        info("  工号: " + number);
        info("  姓名: " + name);
        info("  邮箱: " + (email != null ? email : "(无)"));

        try {
            // 调用 UserService 创建用户（注册成功后自动分发事件）
            User user = UserService.register(name, number, defaultPassword, email);

            info("");
            info("用户创建成功！");
            info("  ID: " + user.getId());
            info("  姓名: " + user.getName());
            info("  工号: " + user.getNumber());
            info("  邮箱: " + user.getEmail());
            info("  创建时间: " + user.getCreatedAt());

            return 0;
        } catch (RuntimeException e) {
            error("创建用户失败: " + e.getMessage());
            return 1;
        }
    }
}
