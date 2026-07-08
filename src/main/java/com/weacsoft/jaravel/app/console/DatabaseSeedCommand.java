package com.weacsoft.jaravel.app.console;

import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.app.model.admin.AdminPermission;
import com.weacsoft.jaravel.app.model.admin.AdminRole;
import com.weacsoft.jaravel.app.model.user.UserPermission;
import com.weacsoft.jaravel.app.model.user.UserRole;
import com.weacsoft.jaravel.app.service.AdminRolePermissionService;
import com.weacsoft.jaravel.app.service.UserRolePermissionService;
import com.weacsoft.jaravel.vendor.artisan.ArtisanCommand;
import gaarason.database.contract.eloquent.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 种子数据初始化命令，对齐 Laravel 的 {@code php artisan db:seed}。
 * <p>
 * 初始化以下数据：
 * <ol>
 *   <li>Admin 权限树（system 根节点 + 7 个子节点）</li>
 *   <li>User 权限树（platform 根节点 + java/jar 子节点 + 4 个叶子节点）</li>
 *   <li>超级管理员角色并分配所有 Admin 权限</li>
 *   <li>默认用户角色（普通用户、仅 Java、仅 Jar）并分配对应权限</li>
 *   <li>初始管理员账号（admin/admin123）</li>
 * </ol>
 * <p>
 * 执行方式：{@code java -jar jaravel.jar --artisan db:seed}
 */
@Component
public class DatabaseSeedCommand extends ArtisanCommand {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeedCommand.class);

    @Override
    public String signature() {
        return "db:seed";
    }

    @Override
    public String description() {
        return "初始化种子数据（权限树、角色、管理员）";
    }

    @Override
    public int handle() {
        log.info("[db:seed] 开始初始化种子数据...");

        seedAdminPermissions();
        seedUserPermissions();
        seedAdminRoles();
        seedUserRoles();
        seedAdminAccount();

        log.info("[db:seed] 种子数据初始化完成");
        return 0;
    }

    // ===== Admin 权限树 =====

    private void seedAdminPermissions() {
        log.info("[db:seed] 初始化 Admin 权限树...");

        // 根节点
        AdminPermission system = findOrCreateAdminPermission("系统管理", "system", null, null, "系统管理根权限");

        // 二级节点
        findOrCreateAdminPermission("个人中心", "admin.profile", system.getId(), "/api/auth/admin/*", "管理员个人信息（me / logout）");
        findOrCreateAdminPermission("管理员管理", "admin.manage", system.getId(), "/api/rbac/admins/*", "管理员 CRUD");
        findOrCreateAdminPermission("角色管理", "role.manage", system.getId(), "/api/rbac/roles/*", "Admin 角色 CRUD");
        findOrCreateAdminPermission("权限管理", "permission.manage", system.getId(), "/api/rbac/permissions/*", "Admin 权限 CRUD");
        findOrCreateAdminPermission("用户管理", "user.manage", system.getId(), "/api/user-rbac/users/*", "平台用户 CRUD");
        findOrCreateAdminPermission("插件管理", "plugin.manage", system.getId(), "/api/plugins/*", "插件管理（Java + Jar）");
        findOrCreateAdminPermission("多租户管理", "tenant.manage", system.getId(), "/api/multi-tenant/*", "多租户插件管理");
        findOrCreateAdminPermission("远程执行管理", "remote.manage", system.getId(), "/api/remote/*", "P2SP 远程执行管理");

        log.info("[db:seed] Admin 权限树初始化完成（1 根节点 + 8 子节点）");
    }

    // ===== User 权限树 =====

    private void seedUserPermissions() {
        log.info("[db:seed] 初始化 User 权限树...");

        // 根节点
        UserPermission platform = findOrCreateUserPermission("插件平台", "platform", null, null, "插件平台根权限");

        // 二级节点
        findOrCreateUserPermission("个人中心", "user.profile", platform.getId(), "/api/auth/user/*", "用户个人信息（me / logout）");
        findOrCreateUserPermission("用户列表", "user.list", platform.getId(), "/api/users/*", "查看用户列表与详情");
        UserPermission javaPerm = findOrCreateUserPermission("Java 插件", "plugin.java", platform.getId(), "/api/plugin/java/*", "Java 插件功能");
        UserPermission jarPerm = findOrCreateUserPermission("Jar 插件", "plugin.jar", platform.getId(), "/api/plugin/jar/*", "Jar 插件功能");

        // 三级节点（Java）
        findOrCreateUserPermission("运行 Java 插件", "plugin.java.run", javaPerm.getId(), "/api/plugin/java/run", "在线编译执行 Java 源码");
        findOrCreateUserPermission("查看 Java 状态", "plugin.java.status", javaPerm.getId(), "/api/plugin/java/status", "查看 Java 插件状态");

        // 三级节点（Jar）
        findOrCreateUserPermission("运行 Jar 插件", "plugin.jar.run", jarPerm.getId(), "/api/plugin/jar/run", "反射调用 Jar 插件方法");
        findOrCreateUserPermission("查看 Jar 状态", "plugin.jar.status", jarPerm.getId(), "/api/plugin/jar/status", "查看 Jar 插件状态");

        log.info("[db:seed] User 权限树初始化完成（1 根 + 6 子 + 4 叶）");
    }

    // ===== Admin 角色 =====

    private void seedAdminRoles() {
        log.info("[db:seed] 初始化 Admin 角色...");

        // 超级管理员角色
        AdminRole superAdmin = findOrCreateAdminRole("超级管理员", "super_admin", "拥有全部权限");

        // 为超级管理员分配所有 Admin 权限
        List<AdminPermission> allPermissions = AdminPermission.all();
        for (AdminPermission perm : allPermissions) {
            AdminRolePermissionService.assignPermissionToRole(superAdmin.getId(), perm.getId());
        }

        log.info("[db:seed] Admin 角色初始化完成（超级管理员已分配 {} 个权限）", allPermissions.size());
    }

    // ===== User 角色 =====

    private void seedUserRoles() {
        log.info("[db:seed] 初始化 User 角色...");

        // 普通用户角色（可运行 Java 和 Jar 插件）
        UserRole userRole = findOrCreateUserRole("普通用户", "user", "可运行 Java 和 Jar 插件");
        assignUserPermissionByCode(userRole.getId(), "platform");

        // 仅 Java 角色
        UserRole javaOnly = findOrCreateUserRole("仅 Java", "java_only", "只能运行 Java 插件");
        assignUserPermissionByCode(javaOnly.getId(), "plugin.java");
        assignUserPermissionByCode(javaOnly.getId(), "user.profile");

        // 仅 Jar 角色
        UserRole jarOnly = findOrCreateUserRole("仅 Jar", "jar_only", "只能运行 Jar 插件");
        assignUserPermissionByCode(jarOnly.getId(), "plugin.jar");
        assignUserPermissionByCode(jarOnly.getId(), "user.profile");

        log.info("[db:seed] User 角色初始化完成（普通用户、仅 Java、仅 Jar）");
    }

    // ===== 初始管理员账号 =====

    private void seedAdminAccount() {
        log.info("[db:seed] 初始化管理员账号...");

        if (Admin.findByUsername("admin") != null) {
            log.info("[db:seed] 管理员账号 admin 已存在，跳过");
            return;
        }

        Admin admin = AdminRolePermissionService.createAdmin(
                "admin", "admin123", "系统管理员", "初始超级管理员");

        // 分配超级管理员角色
        AdminRole superAdminRole = findAdminRoleByCode("super_admin");
        if (superAdminRole != null) {
            AdminRolePermissionService.assignRoleToAdmin(admin.getId(), superAdminRole.getId());
        }

        log.info("[db:seed] 管理员账号创建完成（admin/admin123），已分配超级管理员角色");
    }

    // ===== 工具方法 =====

    private AdminPermission findOrCreateAdminPermission(String name, String code, Long parentId, String route, String description) {
        Record<AdminPermission, Long> record = AdminPermission.query().where("code", code).first();
        if (record != null) {
            return record.toObject();
        }
        return AdminRolePermissionService.createPermission(name, code, parentId, route, description);
    }

    private UserPermission findOrCreateUserPermission(String name, String code, Long parentId, String route, String description) {
        Record<UserPermission, Long> record = UserPermission.query().where("code", code).first();
        if (record != null) {
            return record.toObject();
        }
        return UserRolePermissionService.createPermission(name, code, parentId, route, description);
    }

    private AdminRole findOrCreateAdminRole(String name, String code, String description) {
        Record<AdminRole, Long> record = AdminRole.query().where("code", code).first();
        if (record != null) {
            return record.toObject();
        }
        return AdminRolePermissionService.createRole(name, code, description);
    }

    private UserRole findOrCreateUserRole(String name, String code, String description) {
        Record<UserRole, Long> record = UserRole.query().where("code", code).first();
        if (record != null) {
            return record.toObject();
        }
        return UserRolePermissionService.createRole(name, code, description);
    }

    private AdminRole findAdminRoleByCode(String code) {
        Record<AdminRole, Long> record = AdminRole.query().where("code", code).first();
        return record == null ? null : record.toObject();
    }

    private void assignUserPermissionByCode(Long roleId, String permissionCode) {
        Record<UserPermission, Long> record = UserPermission.query().where("code", permissionCode).first();
        if (record != null) {
            UserRolePermissionService.assignPermissionToRole(roleId, record.toObject().getId());
        }
    }
}
