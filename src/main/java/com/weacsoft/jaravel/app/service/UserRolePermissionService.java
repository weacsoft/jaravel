package com.weacsoft.jaravel.app.service;

import com.weacsoft.jaravel.app.model.User;
import com.weacsoft.jaravel.app.model.user.UserPermission;
import com.weacsoft.jaravel.app.model.user.UserRole;
import com.weacsoft.jaravel.app.model.user.middle.UserRolePermission;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户-角色-权限（RBAC）服务，面向多租户场景下的用户权限管理。
 * <p>
 * 与 {@link AdminRolePermissionService} 对称，但面向普通用户。
 * 全部方法为静态方法，提供完整的 RBAC 能力：
 * <ol>
 *   <li>用户 / 角色 / 权限 三类主体的增删改查；</li>
 *   <li>用户↔角色、角色↔权限 的分配与解除，以及「全部（含未分配）」「已分配」两类列表查询；</li>
 *   <li>用户权限判断 {@link #userHasPermission(Long, Long)}（默认拒绝 + 树形祖先授权）；</li>
 *   <li>路由权限判断 {@link #userCanAccessRoute(Long, String)}（路由模式匹配）；</li>
 *   <li>溯源查询 {@link #findRolesGrantingPermission(Long, Long)}。</li>
 * </ol>
 *
 * <h3>多租户插件运行场景</h3>
 * 典型权限树设计（Java/Jar 插件运行控制）：
 * <pre>
 * plugin (插件管理)
 * ├── plugin.java (Java 插件, route: /plugin/java/*)
 * │   ├── plugin.java.run (运行, route: /plugin/java/run)
 * │   └── plugin.java.upload (上传, route: /plugin/java/upload)
 * └── plugin.jar (Jar 插件, route: /plugin/jar/*)
 *     ├── plugin.jar.run (运行, route: /plugin/jar/run)
 *     └── plugin.jar.upload (上传, route: /plugin/jar/upload)
 * </pre>
 * 授予 {@code plugin.java} 即隐含授予 {@code plugin.java.run} 和 {@code plugin.java.upload}，
 * 用户可以运行和上传 Java 插件但不可操作 Jar 插件。
 *
 * @see User#hasRole(String) 实例方法委托本服务
 * @see User#hasPermission(String) 实例方法委托本服务
 * @see User#canAccessRoute(String) 路由权限判断
 */
public final class UserRolePermissionService {

    private UserRolePermissionService() {
    }

    // ================================================================
    // 1. 用户 CRUD
    // ================================================================

    /** 新增用户 */
    public static User createUser(String name, String number, String password, String email, String description) {
        User user = new User();
        user.setName(name);
        user.setNumber(number);
        user.setPassword(password);
        user.setEmail(email);
        user.setDescription(description);
        return user.save();
    }

    /** 按主键查询用户 */
    public static User findUser(Long id) {
        return User.find(id);
    }

    /** 按工号查询用户 */
    public static User findUserByNumber(String number) {
        return User.findByNumber(number);
    }

    /** 查询全部用户 */
    public static List<User> listUsers() {
        return User.all();
    }

    /** 更新用户字段 */
    public static User updateUser(Long id, Map<String, Object> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return User.find(id);
        }
        User.query().where("id", id).data(fields).update();
        return User.find(id);
    }

    /** 删除用户，级联清理 user_role 中间表关联 */
    public static boolean deleteUser(Long id) {
        if (id == null) {
            return false;
        }
        detachAllUserRoles(id);
        return User.query().where("id", id).delete() > 0;
    }

    // ================================================================
    // 2. 角色 CRUD
    // ================================================================

    /** 新增角色 */
    public static UserRole createRole(String name, String code, String description) {
        UserRole role = new UserRole();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        return role.save();
    }

    /** 按主键查询角色 */
    public static UserRole findRole(Long id) {
        return UserRole.find(id);
    }

    /** 按编码查询角色 */
    public static UserRole findRoleByCode(String code) {
        return UserRole.findByCode(code);
    }

    /** 查询全部角色 */
    public static List<UserRole> listRoles() {
        return UserRole.all();
    }

    /** 更新角色字段 */
    public static UserRole updateRole(Long id, Map<String, Object> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return UserRole.find(id);
        }
        UserRole.query().where("id", id).data(fields).update();
        return UserRole.find(id);
    }

    /** 删除角色，级联清理 user_role 与 user_role_permission 中间表 */
    public static boolean deleteRole(Long id) {
        if (id == null) {
            return false;
        }
        deleteUserRolePivotsByRole(id);
        deleteUserRolePermissionPivotsByRole(id);
        return UserRole.query().where("id", id).delete() > 0;
    }

    // ================================================================
    // 3. 权限 CRUD（树形 + route 路由关联）
    // ================================================================

    /**
     * 新增权限。
     *
     * @param parentId 父权限 ID，{@code null} 表示根节点
     * @param route    关联的路由模式，如 {@code /plugin/java/*}，{@code null} 表示不关联路由
     */
    public static UserPermission createPermission(String name, String code, Long parentId, String route, String description) {
        UserPermission permission = new UserPermission();
        permission.setName(name);
        permission.setCode(code);
        permission.setParentId(parentId);
        permission.setRoute(route);
        permission.setDescription(description);
        return permission.save();
    }

    /** 按主键查询权限 */
    public static UserPermission findPermission(Long id) {
        return UserPermission.find(id);
    }

    /** 按编码查询权限 */
    public static UserPermission findPermissionByCode(String code) {
        return UserPermission.findByCode(code);
    }

    /** 查询全部权限 */
    public static List<UserPermission> listPermissions() {
        return UserPermission.all();
    }

    /** 更新权限字段 */
    public static UserPermission updatePermission(Long id, Map<String, Object> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return UserPermission.find(id);
        }
        UserPermission.query().where("id", id).data(fields).update();
        return UserPermission.find(id);
    }

    /**
     * 删除权限：子节点上移到祖父，清理 user_role_permission 中间表关联。
     */
    public static boolean deletePermission(Long id) {
        if (id == null) {
            return false;
        }
        UserPermission target = UserPermission.find(id);
        if (target == null) {
            return false;
        }
        Long grandParent = target.getParentId();
        Map<String, Object> reparent = new HashMap<>();
        reparent.put("parent_id", grandParent);
        UserPermission.query().where("parent_id", id).data(reparent).update();
        deleteUserRolePermissionPivotsByPermission(id);
        return UserPermission.query().where("id", id).delete() > 0;
    }

    // ================================================================
    // 4. 用户 ↔ 角色
    // ================================================================

    /** 为用户分配角色（幂等） */
    public static boolean assignRoleToUser(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        if (userRolePivotExists(userId, roleId)) {
            return false;
        }
        attachUserRole(userId, roleId);
        return true;
    }

    /** 解除用户的单个角色 */
    public static int removeRoleFromUser(Long userId, Long roleId) {
        return detachUserRole(userId, roleId);
    }

    /** 解除用户的所有角色 */
    public static int removeAllRolesFromUser(Long userId) {
        return detachAllUserRoles(userId);
    }

    /** 判断用户是否拥有指定角色（按 ID） */
    public static boolean userHasRole(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        return userRolePivotExists(userId, roleId);
    }

    /** 判断用户是否拥有指定角色（按 code） */
    public static boolean userHasRole(Long userId, String roleCode) {
        if (userId == null || roleCode == null) {
            return false;
        }
        UserRole role = UserRole.findByCode(roleCode);
        return role != null && userRolePivotExists(userId, role.getId());
    }

    /** 查询用户的全部角色（含未分配），assigned 标记 */
    public static List<UserRole> getUserRolesAll(Long userId) {
        Set<Long> assignedIds = roleIdsOfUser(userId);
        List<UserRole> all = UserRole.all();
        for (UserRole role : all) {
            role.setAssigned(assignedIds.contains(role.getId()));
        }
        return all;
    }

    /** 查询用户已分配的角色 */
    public static List<UserRole> getUserRolesAssigned(Long userId) {
        Set<Long> roleIds = roleIdsOfUser(userId);
        List<UserRole> result = new ArrayList<>();
        for (Long roleId : roleIds) {
            UserRole role = UserRole.find(roleId);
            if (role != null) {
                role.setAssigned(true);
                result.add(role);
            }
        }
        return result;
    }

    // ================================================================
    // 5. 角色 ↔ 权限
    // ================================================================

    /** 为角色分配权限（幂等） */
    public static boolean assignPermissionToRole(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            return false;
        }
        if (UserRolePermission.exists(roleId, permissionId)) {
            return false;
        }
        UserRolePermission rp = new UserRolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rp.save();
        return true;
    }

    /** 解除角色的单个权限 */
    public static int removePermissionFromRole(Long roleId, Long permissionId) {
        return UserRolePermission.query().where("role_id", roleId).where("permission_id", permissionId).delete();
    }

    /** 解除角色的所有权限 */
    public static int removeAllPermissionsFromRole(Long roleId) {
        return UserRolePermission.query().where("role_id", roleId).delete();
    }

    /** 判断角色是否显式拥有权限（按 ID） */
    public static boolean roleHasPermission(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            return false;
        }
        return UserRolePermission.exists(roleId, permissionId);
    }

    /** 判断角色是否显式拥有权限（按 code） */
    public static boolean roleHasPermission(Long roleId, String permissionCode) {
        if (roleId == null || permissionCode == null) {
            return false;
        }
        UserPermission permission = UserPermission.findByCode(permissionCode);
        return permission != null && UserRolePermission.exists(roleId, permission.getId());
    }

    /** 查询角色的全部权限（含未分配），assigned 标记 */
    public static List<UserPermission> getRolePermissionsAll(Long roleId) {
        Set<Long> assignedIds = permissionIdsOfRole(roleId);
        List<UserPermission> all = UserPermission.all();
        for (UserPermission permission : all) {
            permission.setAssigned(assignedIds.contains(permission.getId()));
        }
        return all;
    }

    /** 查询角色已显式分配的权限 */
    public static List<UserPermission> getRolePermissionsAssigned(Long roleId) {
        Set<Long> permissionIds = permissionIdsOfRole(roleId);
        List<UserPermission> result = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            UserPermission permission = UserPermission.find(permissionId);
            if (permission != null) {
                permission.setAssigned(true);
                result.add(permission);
            }
        }
        return result;
    }

    // ================================================================
    // 6. 用户 ↔ 权限（树形祖先授权，默认拒绝）
    // ================================================================

    /**
     * 判断用户是否拥有指定权限（按 ID，含树形祖先授权推导）。
     * <p>
     * 默认拒绝 + 祖先授权：只要用户拥有的任一权限是目标权限的祖先（或自身），即判定为拥有。
     *
     * @return 拥有该权限返回 true
     */
    public static boolean userHasPermission(Long userId, Long permissionId) {
        if (userId == null || permissionId == null) {
            return false;
        }
        Set<Long> roleIds = roleIdsOfUser(userId);
        if (roleIds.isEmpty()) {
            return false;
        }
        Set<Long> granted = permissionIdsOfRoles(roleIds);
        if (granted.isEmpty()) {
            return false;
        }
        Map<Long, Long> parentMap = buildParentMap();
        for (Long grantedId : granted) {
            if (isAncestorOrSelf(grantedId, permissionId, parentMap)) {
                return true;
            }
        }
        return false;
    }

    /** 判断用户是否拥有指定权限（按 code，如 "plugin.java.run"） */
    public static boolean userHasPermission(Long userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }
        UserPermission permission = UserPermission.findByCode(permissionCode);
        return permission != null && userHasPermission(userId, permission.getId());
    }

    /** 查询用户的全部权限（含未分配），assigned 标记是否生效（含祖先推导） */
    public static List<UserPermission> getUserPermissionsAll(Long userId) {
        List<UserPermission> all = UserPermission.all();
        for (UserPermission permission : all) {
            permission.setAssigned(userHasPermission(userId, permission.getId()));
        }
        return all;
    }

    /** 查询用户已生效的权限（含树形推导） */
    public static List<UserPermission> getUserPermissionsAssigned(Long userId) {
        List<UserPermission> result = new ArrayList<>();
        for (UserPermission permission : UserPermission.all()) {
            if (userHasPermission(userId, permission.getId())) {
                permission.setAssigned(true);
                result.add(permission);
            }
        }
        return result;
    }

    /** 溯源：查询用户的某权限由哪个角色授予 */
    public static List<UserRole> findRolesGrantingPermission(Long userId, Long permissionId) {
        List<UserRole> grantors = new ArrayList<>();
        if (userId == null || permissionId == null) {
            return grantors;
        }
        Set<Long> roleIds = roleIdsOfUser(userId);
        if (roleIds.isEmpty()) {
            return grantors;
        }
        Map<Long, Long> parentMap = buildParentMap();
        for (Long roleId : roleIds) {
            Set<Long> permIds = permissionIdsOfRole(roleId);
            boolean grants = false;
            for (Long pid : permIds) {
                if (isAncestorOrSelf(pid, permissionId, parentMap)) {
                    grants = true;
                    break;
                }
            }
            if (grants) {
                UserRole role = UserRole.find(roleId);
                if (role != null) {
                    grantors.add(role);
                }
            }
        }
        return grantors;
    }

    // ================================================================
    // 7. 路由权限匹配
    // ================================================================

    /** 规整路由路径 */
    public static String normalizeRoute(String route) {
        if (route == null) {
            return null;
        }
        route = route.trim();
        if (route.isEmpty()) {
            return "/";
        }
        if (!route.startsWith("/")) {
            route = "/" + route;
        }
        while (route.length() > 1 && route.endsWith("/")) {
            route = route.substring(0, route.length() - 1);
        }
        return route;
    }

    /**
     * 路由模式匹配（不使用正则）。
     * <ul>
     *   <li>全匹配：{@code /plugin/java/run} 仅匹配自身</li>
     *   <li>通配匹配：{@code /plugin/java/*} 匹配前缀下所有路由</li>
     * </ul>
     */
    public static boolean routeMatches(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        pattern = normalizeRoute(pattern);
        path = normalizeRoute(path);

        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (prefix.isEmpty()) {
                return true;
            }
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        return pattern.equals(path);
    }

    /**
     * 判断用户是否有权访问指定路由（默认拒绝 + 树形祖先授权 + 路由模式匹配）。
     * <p>
     * 典型用法：
     * <pre>
     * if (UserRolePermissionService.userCanAccessRoute(userId, "/plugin/java/run")) {
     *     // 允许运行 Java 插件
     * }
     * </pre>
     */
    public static boolean userCanAccessRoute(Long userId, String route) {
        if (userId == null || route == null) {
            return false;
        }
        String normalizedRoute = normalizeRoute(route);
        for (UserPermission permission : UserPermission.all()) {
            String pattern = permission.getRoute();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (routeMatches(pattern, normalizedRoute) && userHasPermission(userId, permission.getId())) {
                return true;
            }
        }
        return false;
    }

    /** 获取用户可访问的全部路由模式列表 */
    public static List<String> getUserAccessibleRoutes(Long userId) {
        List<String> routes = new ArrayList<>();
        if (userId == null) {
            return routes;
        }
        for (UserPermission permission : UserPermission.all()) {
            String pattern = permission.getRoute();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (userHasPermission(userId, permission.getId())) {
                routes.add(pattern);
            }
        }
        return routes;
    }

    // ================================================================
    // 8. 树形权限工具方法
    // ================================================================

    private static Map<Long, Long> buildParentMap() {
        Map<Long, Long> parentMap = new HashMap<>();
        for (UserPermission permission : UserPermission.all()) {
            parentMap.put(permission.getId(), permission.getParentId());
        }
        return parentMap;
    }

    private static Map<Long, List<Long>> buildChildrenMap() {
        Map<Long, List<Long>> children = new HashMap<>();
        for (UserPermission permission : UserPermission.all()) {
            if (permission.getParentId() != null) {
                children.computeIfAbsent(permission.getParentId(), k -> new ArrayList<>())
                        .add(permission.getId());
            }
        }
        return children;
    }

    private static boolean isAncestorOrSelf(Long candidate, Long target, Map<Long, Long> parentMap) {
        Long cur = target;
        Set<Long> visited = new HashSet<>();
        while (cur != null && visited.add(cur)) {
            if (cur.equals(candidate)) {
                return true;
            }
            cur = parentMap.get(cur);
        }
        return false;
    }

    private static Set<Long> getDescendantIdsIncludingSelf(Long permissionId) {
        Map<Long, List<Long>> children = buildChildrenMap();
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(permissionId);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            if (result.add(cur)) {
                List<Long> kids = children.get(cur);
                if (kids != null) {
                    queue.addAll(kids);
                }
            }
        }
        return result;
    }

    // ================================================================
    // 9. 中间表私有封装
    // ================================================================

    /** 查询用户拥有的全部角色 ID */
    private static Set<Long> roleIdsOfUser(Long userId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (com.weacsoft.jaravel.app.model.user.middle.UserRole pivot
                : com.weacsoft.jaravel.app.model.user.middle.UserRole.findByUserId(userId)) {
            ids.add(pivot.getRoleId());
        }
        return ids;
    }

    private static boolean userRolePivotExists(Long userId, Long roleId) {
        return com.weacsoft.jaravel.app.model.user.middle.UserRole.exists(userId, roleId);
    }

    private static void attachUserRole(Long userId, Long roleId) {
        com.weacsoft.jaravel.app.model.user.middle.UserRole pivot
                = new com.weacsoft.jaravel.app.model.user.middle.UserRole();
        pivot.setUserId(userId);
        pivot.setRoleId(roleId);
        pivot.save();
    }

    private static int detachUserRole(Long userId, Long roleId) {
        return com.weacsoft.jaravel.app.model.user.middle.UserRole.query()
                .where("user_id", userId).where("role_id", roleId).delete();
    }

    private static int detachAllUserRoles(Long userId) {
        return com.weacsoft.jaravel.app.model.user.middle.UserRole.query()
                .where("user_id", userId).delete();
    }

    private static int deleteUserRolePivotsByRole(Long roleId) {
        return com.weacsoft.jaravel.app.model.user.middle.UserRole.query()
                .where("role_id", roleId).delete();
    }

    private static Set<Long> permissionIdsOfRole(Long roleId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (UserRolePermission rp : UserRolePermission.findByRoleId(roleId)) {
            ids.add(rp.getPermissionId());
        }
        return ids;
    }

    private static Set<Long> permissionIdsOfRoles(Set<Long> roleIds) {
        Set<Long> ids = new HashSet<>();
        for (Long roleId : roleIds) {
            ids.addAll(permissionIdsOfRole(roleId));
        }
        return ids;
    }

    private static int deleteUserRolePermissionPivotsByRole(Long roleId) {
        return UserRolePermission.query().where("role_id", roleId).delete();
    }

    private static int deleteUserRolePermissionPivotsByPermission(Long permissionId) {
        return UserRolePermission.query().where("permission_id", permissionId).delete();
    }
}
