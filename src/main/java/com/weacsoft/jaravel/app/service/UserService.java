package com.weacsoft.jaravel.app.service;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.user.UserRole;
import com.weacsoft.jaravel.vendor.core.SpringContext;
import com.weacsoft.jaravel.vendor.event.Dispatcher;
import com.weacsoft.jaravel.vendor.event.example.UserRegisteredEvent;
import gaarason.database.contract.eloquent.Record;

import java.util.List;

/**
 * 用户服务，采用 Service 层静态调用风格（对齐 manage8.0 / Laravel 的 {@code app/Services}）。
 * <p>
 * 所有方法均为 {@code public static}，无状态、无实例化。
 * 直接通过 Model 的静态方法操作数据库，对齐 Laravel 风格。
 * <pre>
 * User u = UserService.login("admin", "123456");
 * List&lt;User&gt; all = UserService.list();
 * </pre>
 */
public final class UserService {

    private UserService() {
    }

    /**
     * 用户注册（创建用户），对齐 Laravel {@code User::create([...])}。
     * <p>
     * 注册成功后分发 {@link UserRegisteredEvent} 事件，触发监听器
     * （同步日志记录 + 异步发送欢迎邮件），对齐 Laravel 的 Event::dispatch。
     *
     * @return 新建的用户实体
     */
    public static User register(String name, String number, String password, String email) {
        // 唯一性校验
        if (User.findByNumber(number) != null) {
            throw new RuntimeException("工号已存在");
        }
        User user = new User();
        user.setName(name);
        user.setNumber(number);
        user.setPassword(password);
        user.setEmail(email);
        user.save();

        // 为新用户分配默认角色（普通用户），使其具备基本访问权限
        Record<UserRole, Long> defaultRole = UserRole.query().where("code", "user").first();
        if (defaultRole != null) {
            UserRolePermissionService.assignRoleToUser(user.getId(), defaultRole.toObject().getId());
        }

        // 分发用户注册事件（对齐 Laravel event(new UserRegisteredEvent(...))）
        Dispatcher dispatcher = SpringContext.bean(Dispatcher.class);
        dispatcher.dispatch(new UserRegisteredEvent(user.getId(), user.getName()));

        return user;
    }

    /**
     * 用户登录，对齐 manage8.0 的 {@code UserService::checkPassword}。
     * <p>
     * <b>密码校验在应用层完成</b>（Service 层），不在 UserProvider / Guard 中。
     * 流程：按 number 查出用户 → 应用层校验密码 → 返回用户实体（由 Controller 调用 Auth.login 登入）。
     *
     * @return 登录成功的用户实体
     * @throws RuntimeException 工号或密码错误时抛出
     */
    public static User login(String number, String password) {
        // 1. 按凭证（number）查出用户
        User user = User.findByNumber(number);
        if (user == null) {
            throw new RuntimeException("工号或密码错误");
        }
        // 2. 应用层校验密码（演示用明文比对，生产环境应使用 BCrypt）
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("工号或密码错误");
        }
        // 3. 返回用户实体，由 Controller 调用 Auth.login(user) 登入
        return user;
    }

    /** 按主键查询用户，对齐 Laravel User::find() */
    public static User findById(Long id) {
        return User.find(id);
    }

    /** 查询全部用户，对齐 Laravel User::all() */
    public static List<User> list() {
        return User.all();
    }
}
