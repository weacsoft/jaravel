package com.weacsoft.jaravel.app.service;

import com.weacsoft.jaravel.app.model.admin.Admin;
import com.weacsoft.jaravel.app.model.admin.AdminPermission;
import com.weacsoft.jaravel.app.model.admin.AdminRole;
import com.weacsoft.jaravel.app.model.admin.middle.RolePermission;

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
 * 管理员-角色-权限（RBAC）服务，对齐 Laravel 中 {@code app/Services/AdminRolePermissionService.php}。
 * <p>
 * 全部方法为静态方法，提供完整的 RBAC 能力：
 * <ol>
 *   <li>管理员 / 角色 / 权限 三类主体的增删改查；</li>
 *   <li>管理员↔角色、角色↔权限 的分配与解除，以及「全部（含未分配）」「已分配」两类列表查询；</li>
 *   <li>管理员权限判断 {@link #adminHasPermission(Long, Long)}（默认拒绝 + 树形祖先授权）；</li>
 *   <li>溯源查询 {@link #findRolesGrantingPermission(Long, Long)}（某权限由哪个角色授予）。</li>
 * </ol>
 *
 * <h3>树形权限语义</h3>
 * 权限为树形层级（{@link AdminPermission#getParentId()}）。授权遵循「父节点授权即等同旗下所有子节点授权」，
 * 因此权限判断采用「祖先授权」推导：只要管理员拥有的任一权限是目标权限的祖先（或自身），即判定为拥有。
 * 该设计在读取时推导，天然保证「不会出现父节点有权限、子节点没有权限」——
 * 新增子权限无需重新授权即可被父权限覆盖。
 *
 * <h3>命名说明</h3>
 * 角色实体 {@link AdminRole}（表 {@code admin_roles}）与中间表记录
 * {@code com.weacsoft.jaravel.app.model.admin.middle.AdminRole}（表 {@code admin_role}）同名异包，
 * 本服务导入角色实体，中间表通过全限定名访问并封装于私有方法中。
 *
 * @see Admin#hasRole(String) 实例方法委托本服务
 * @see Admin#hasPermission(String) 实例方法委托本服务
 */
public final class AdminRolePermissionService {

    private AdminRolePermissionService() {
    }

    // ================================================================
    // 1. 管理员 CRUD
    // ================================================================

    /** 新增管理员，对齐 Laravel Admin::create([...]) */
    public static Admin createAdmin(String username, String password, String nickname, String description) {
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setNickname(nickname);
        admin.setDescription(description);
        admin.setStatus(1);
        return admin.save();
    }

    /** 按主键查询管理员 */
    public static Admin findAdmin(Long id) {
        return Admin.find(id);
    }

    /** 按用户名查询管理员 */
    public static Admin findAdminByUsername(String username) {
        return Admin.findByUsername(username);
    }

    /** 查询全部管理员 */
    public static List<Admin> listAdmins() {
        return Admin.all();
    }

    /**
     * 更新管理员，对齐 Laravel Admin::where('id', $id)->update([...])。
     *
     * @param id     管理员 ID
     * @param fields 待更新字段（键为列名，如 username/password/nickname/status）
     * @return 更新后的管理员，不存在返回 null
     */
    public static Admin updateAdmin(Long id, Map<String, Object> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return Admin.find(id);
        }
        Admin.query().where("id", id).data(fields).update();
        return Admin.find(id);
    }

    /**
     * 删除管理员，并级联清理其在 {@code admin_role} 中间表中的关联（保持数据一致性）。
     *
     * @return 成功删除返回 true
     */
    public static boolean deleteAdmin(Long id) {
        if (id == null) {
            return false;
        }
        detachAllAdminRoles(id);
        return Admin.query().where("id", id).delete() > 0;
    }

    // ================================================================
    // 2. 角色 CRUD
    // ================================================================

    /** 新增角色 */
    public static AdminRole createRole(String name, String code, String description) {
        AdminRole role = new AdminRole();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        return role.save();
    }

    /** 按主键查询角色 */
    public static AdminRole findRole(Long id) {
        return AdminRole.find(id);
    }

    /** 按编码查询角色 */
    public static AdminRole findRoleByCode(String code) {
        return AdminRole.findByCode(code);
    }

    /** 查询全部角色 */
    public static List<AdminRole> listRoles() {
        return AdminRole.all();
    }

    /** 更新角色字段 */
    public static AdminRole updateRole(Long id, Map<String, Object> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return AdminRole.find(id);
        }
        AdminRole.query().where("id", id).data(fields).update();
        return AdminRole.find(id);
    }

    /**
     * 删除角色，并级联清理 {@code admin_role} 与 {@code role_permission} 中间表关联。
     *
     * @return 成功删除返回 true
     */
    public static boolean deleteRole(Long id) {
        if (id == null) {
            return false;
        }
        deleteAdminRolePivotsByRole(id);
        deleteRolePermissionPivotsByRole(id);
        return AdminRole.query().where("id", id).delete() > 0;
    }

    // ================================================================
    // 3. 权限 CRUD（树形）
    // ================================================================

    /**
     * 新增权限。
     *
     * @param parentId 父权限 ID，{@code null} 表示根节点
     * @param route    关联的路由模式，如 {@code /admin/*} 或 {@code /admin/user}，{@code null} 表示不关联路由
     */
    public static AdminPermission createPermission(String name, String code, Long parentId, String route, String description) {
        AdminPermission permission = new AdminPermission();
        permission.setName(name);
        permission.setCode(code);
        permission.setParentId(parentId);
        permission.setRoute(route);
        permission.setDescription(description);
        return permission.save();
    }

    /** 按主键查询权限 */
    public static AdminPermission findPermission(Long id) {
        return AdminPermission.find(id);
    }

    /** 按编码查询权限 */
    public static AdminPermission findPermissionByCode(String code) {
        return AdminPermission.findByCode(code);
    }

    /** 查询全部权限 */
    public static List<AdminPermission> listPermissions() {
        return AdminPermission.all();
    }

    /** 更新权限字段（注意：变更 parent_id 会改变树形结构） */
    public static AdminPermission updatePermission(Long id, Map<String, Object> fields) {
        if (id == null || fields == null || fields.isEmpty()) {
            return AdminPermission.find(id);
        }
        AdminPermission.query().where("id", id).data(fields).update();
        return AdminPermission.find(id);
    }

    /**
     * 删除权限：将其直接子节点的 parent_id 指向被删节点的父节点（「上移」到祖父），
     * 保证权限树结构始终有效，并清理 {@code role_permission} 中间表关联。
     *
     * @return 成功删除返回 true
     */
    public static boolean deletePermission(Long id) {
        if (id == null) {
            return false;
        }
        AdminPermission target = AdminPermission.find(id);
        if (target == null) {
            return false;
        }
        // 子节点上移到祖父，保持树结构有效
        Long grandParent = target.getParentId();
        Map<String, Object> reparent = new HashMap<>();
        reparent.put("parent_id", grandParent);
        AdminPermission.query().where("parent_id", id).data(reparent).update();
        // 清理中间表关联
        deleteRolePermissionPivotsByPermission(id);
        return AdminPermission.query().where("id", id).delete() > 0;
    }

    // ================================================================
    // 4. 管理员 ↔ 角色
    // ================================================================

    /**
     * 为管理员分配角色。已分配则跳过（幂等）。
     *
     * @return 新分配成功返回 true，已分配返回 false
     */
    public static boolean assignRoleToAdmin(Long adminId, Long roleId) {
        if (adminId == null || roleId == null) {
            return false;
        }
        if (adminRolePivotExists(adminId, roleId)) {
            return false;
        }
        attachAdminRole(adminId, roleId);
        return true;
    }

    /** 解除管理员的单个角色，返回受影响行数 */
    public static int removeRoleFromAdmin(Long adminId, Long roleId) {
        return detachAdminRole(adminId, roleId);
    }

    /** 解除管理员的所有角色，返回受影响行数 */
    public static int removeAllRolesFromAdmin(Long adminId) {
        return detachAllAdminRoles(adminId);
    }

    /** 判断管理员是否拥有指定角色（按角色 ID） */
    public static boolean adminHasRole(Long adminId, Long roleId) {
        if (adminId == null || roleId == null) {
            return false;
        }
        return adminRolePivotExists(adminId, roleId);
    }

    /** 判断管理员是否拥有指定角色（按角色 code，如 "super_admin"） */
    public static boolean adminHasRole(Long adminId, String roleCode) {
        if (adminId == null || roleCode == null) {
            return false;
        }
        AdminRole role = AdminRole.findByCode(roleCode);
        return role != null && adminRolePivotExists(adminId, role.getId());
    }

    /**
     * 查询管理员的全部角色（含未分配），每条角色的 {@link AdminRole#getAssigned()}
     * 标记该角色是否已分配给此管理员。
     */
    public static List<AdminRole> getAdminRolesAll(Long adminId) {
        Set<Long> assignedIds = roleIdsOfAdmin(adminId);
        List<AdminRole> all = AdminRole.all();
        for (AdminRole role : all) {
            role.setAssigned(assignedIds.contains(role.getId()));
        }
        return all;
    }

    /** 查询管理员已分配的角色（仅已设置的角色） */
    public static List<AdminRole> getAdminRolesAssigned(Long adminId) {
        Set<Long> roleIds = roleIdsOfAdmin(adminId);
        List<AdminRole> result = new ArrayList<>();
        for (Long roleId : roleIds) {
            AdminRole role = AdminRole.find(roleId);
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

    /** 为角色分配权限。已分配则跳过（幂等） */
    public static boolean assignPermissionToRole(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            return false;
        }
        if (RolePermission.exists(roleId, permissionId)) {
            return false;
        }
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionId(permissionId);
        rp.save();
        return true;
    }

    /** 解除角色的单个权限，返回受影响行数 */
    public static int removePermissionFromRole(Long roleId, Long permissionId) {
        return RolePermission.query().where("role_id", roleId).where("permission_id", permissionId).delete();
    }

    /** 解除角色的所有权限，返回受影响行数 */
    public static int removeAllPermissionsFromRole(Long roleId) {
        return RolePermission.query().where("role_id", roleId).delete();
    }

    /** 判断角色是否显式拥有指定权限（按权限 ID，不含树形祖先推导） */
    public static boolean roleHasPermission(Long roleId, Long permissionId) {
        if (roleId == null || permissionId == null) {
            return false;
        }
        return RolePermission.exists(roleId, permissionId);
    }

    /** 判断角色是否显式拥有指定权限（按权限 code） */
    public static boolean roleHasPermission(Long roleId, String permissionCode) {
        if (roleId == null || permissionCode == null) {
            return false;
        }
        AdminPermission permission = AdminPermission.findByCode(permissionCode);
        return permission != null && RolePermission.exists(roleId, permission.getId());
    }

    /**
     * 查询角色的全部权限（含未分配），每条权限的 {@link AdminPermission#getAssigned()}
     * 标记该权限是否显式分配给此角色。
     */
    public static List<AdminPermission> getRolePermissionsAll(Long roleId) {
        Set<Long> assignedIds = permissionIdsOfRole(roleId);
        List<AdminPermission> all = AdminPermission.all();
        for (AdminPermission permission : all) {
            permission.setAssigned(assignedIds.contains(permission.getId()));
        }
        return all;
    }

    /** 查询角色已显式分配的权限（仅已设置，不含树形推导） */
    public static List<AdminPermission> getRolePermissionsAssigned(Long roleId) {
        Set<Long> permissionIds = permissionIdsOfRole(roleId);
        List<AdminPermission> result = new ArrayList<>();
        for (Long permissionId : permissionIds) {
            AdminPermission permission = AdminPermission.find(permissionId);
            if (permission != null) {
                permission.setAssigned(true);
                result.add(permission);
            }
        }
        return result;
    }

    // ================================================================
    // 6. 管理员 ↔ 权限（树形祖先授权，默认拒绝）
    // ================================================================

    /**
     * 判断管理员是否拥有指定权限（按权限 ID）。
     * <p>
     * <b>默认拒绝</b>：管理员无任何角色或角色无任何权限时返回 false。
     * <p>
     * <b>树形祖先授权</b>：只要管理员拥有的任一权限是目标权限的祖先（或自身），即判定为拥有。
     * 这保证「父节点授权即等同旗下所有子节点授权」，不会出现「父有、子没有」。
     *
     * @return 拥有该权限（含祖先授权）返回 true，否则 false
     */
    public static boolean adminHasPermission(Long adminId, Long permissionId) {
        if (adminId == null || permissionId == null) {
            return false;
        }
        Set<Long> roleIds = roleIdsOfAdmin(adminId);
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

    /** 判断管理员是否拥有指定权限（按权限 code，如 "user.create"） */
    public static boolean adminHasPermission(Long adminId, String permissionCode) {
        if (adminId == null || permissionCode == null) {
            return false;
        }
        AdminPermission permission = AdminPermission.findByCode(permissionCode);
        return permission != null && adminHasPermission(adminId, permission.getId());
    }

    /**
     * 查询管理员的全部权限（含未分配），每条权限的 {@link AdminPermission#getAssigned()}
     * 标记该权限是否对管理员生效（已考虑树形祖先授权：父权限生效则其所有子权限均标记为生效）。
     */
    public static List<AdminPermission> getAdminPermissionsAll(Long adminId) {
        List<AdminPermission> all = AdminPermission.all();
        for (AdminPermission permission : all) {
            permission.setAssigned(adminHasPermission(adminId, permission.getId()));
        }
        return all;
    }

    /**
     * 查询管理员已生效的权限（仅拥有的，含树形祖先授权推导出的后代权限）。
     */
    public static List<AdminPermission> getAdminPermissionsAssigned(Long adminId) {
        List<AdminPermission> result = new ArrayList<>();
        for (AdminPermission permission : AdminPermission.all()) {
            if (adminHasPermission(adminId, permission.getId())) {
                permission.setAssigned(true);
                result.add(permission);
            }
        }
        return result;
    }

    /**
     * 溯源：查询管理员的某个权限是由哪个（些）角色授予的。
     * <p>
     * 遍历管理员的每个角色，若该角色拥有的任一权限是目标权限的祖先（或自身），
     * 则该角色即为授权角色。可能返回多个角色（不同角色通过不同祖先节点分别授权）。
     *
     * @param adminId      管理员 ID
     * @param permissionId 目标权限 ID
     * @return 授予该权限的角色列表（可能为空）
     */
    public static List<AdminRole> findRolesGrantingPermission(Long adminId, Long permissionId) {
        List<AdminRole> grantors = new ArrayList<>();
        if (adminId == null || permissionId == null) {
            return grantors;
        }
        Set<Long> roleIds = roleIdsOfAdmin(adminId);
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
                AdminRole role = AdminRole.find(roleId);
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

    /**
     * 规整路由路径：保证以 {@code /} 开头，去除多余的尾部斜杠（根 {@code /} 除外）。
     *
     * @param route 原始路由
     * @return 规整后的路由，{@code null} 输入返回 {@code null}
     */
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
     * <p>
     * 支持两种匹配方式：
     * <ul>
     *   <li><b>全匹配</b>：模式 {@code /admin/user} 仅匹配路径 {@code /admin/user} 本身</li>
     *   <li><b>通配匹配</b>：模式以 {@code /*} 结尾时，匹配前缀下的所有路由。
     *       例如 {@code /admin/*} 匹配 {@code /admin}、{@code /admin/user}、{@code /admin/order/list} 等</li>
     * </ul>
     * 路由以 {@code /} 开头，{@code *} 仅作为后缀通配符，不支持中间通配。
     *
     * @param pattern 权限关联的路由模式，如 {@code /admin/*} 或 {@code /admin/user}
     * @param path    实际请求的路由路径，如 {@code /admin/user/create}
     * @return 匹配返回 true，不匹配或参数为 null 返回 false
     */
    public static boolean routeMatches(String pattern, String path) {
        if (pattern == null || path == null) {
            return false;
        }
        pattern = normalizeRoute(pattern);
        path = normalizeRoute(path);

        if (pattern.endsWith("/*")) {
            // 提取通配前缀：/admin/* → /admin
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (prefix.isEmpty()) {
                // /* 匹配所有路由
                return true;
            }
            // 匹配前缀本身（/admin）或前缀下的子路径（/admin/xxx）
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        // 全匹配
        return pattern.equals(path);
    }

    /**
     * 判断管理员是否有权访问指定路由。
     * <p>
     * 遍历所有权限节点，对于每个关联了路由模式（{@code route} 非空）的权限：
     * <ol>
     *   <li>检查管理员是否拥有该权限（含树形祖先授权推导）；</li>
     *   <li>若拥有，检查该权限的路由模式是否匹配目标路由。</li>
     * </ol>
     * 任一匹配即返回 true。<b>默认拒绝</b>：无任何匹配时返回 false。
     * <p>
     * 此方法可在中间件中调用，实现「有没有访问这个功能的权限」判断：
     * <pre>
     * if (AdminRolePermissionService.adminCanAccessRoute(adminId, "/admin/user/create")) {
     *     // 允许访问
     * }
     * </pre>
     *
     * @param adminId 管理员 ID
     * @param route   目标路由路径，以 {@code /} 开头
     * @return 有权访问返回 true，否则 false
     * @see #routeMatches(String, String)
     * @see #adminHasPermission(Long, Long)
     */
    public static boolean adminCanAccessRoute(Long adminId, String route) {
        if (adminId == null || route == null) {
            return false;
        }
        String normalizedRoute = normalizeRoute(route);
        for (AdminPermission permission : AdminPermission.all()) {
            String pattern = permission.getRoute();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (routeMatches(pattern, normalizedRoute) && adminHasPermission(adminId, permission.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取管理员可以访问的全部路由模式列表。
     * <p>
     * 遍历所有权限节点，对于每个关联了路由模式且管理员拥有的权限（含树形祖先授权推导），
     * 收集其路由模式。返回的路由模式可能包含通配符（如 {@code /admin/*}）。
     * <p>
     * 可用于前端菜单渲染或权限展示：
     * <pre>
     * List&lt;String&gt; routes = AdminRolePermissionService.getAdminAccessibleRoutes(adminId);
     * // 例如返回：[/admin/dashboard, /admin/user/*, /admin/order/*]
     * </pre>
     *
     * @param adminId 管理员 ID
     * @return 可访问的路由模式列表（可能含通配符），无权限时返回空列表
     */
    public static List<String> getAdminAccessibleRoutes(Long adminId) {
        List<String> routes = new ArrayList<>();
        if (adminId == null) {
            return routes;
        }
        for (AdminPermission permission : AdminPermission.all()) {
            String pattern = permission.getRoute();
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (adminHasPermission(adminId, permission.getId())) {
                routes.add(pattern);
            }
        }
        return routes;
    }

    // ================================================================
    // 8. 树形权限工具方法
    // ================================================================

    /**
     * 构建权限树「子→父」映射：permissionId → parentId（parentId 为 null 表示根节点）。
     * <p>
     * 权限节点数量通常有限，一次性加载全表到内存用于祖先/后代推导。
     */
    private static Map<Long, Long> buildParentMap() {
        Map<Long, Long> parentMap = new HashMap<>();
        for (AdminPermission permission : AdminPermission.all()) {
            parentMap.put(permission.getId(), permission.getParentId());
        }
        return parentMap;
    }

    /**
     * 构建「父→子列表」映射，用于后代遍历。
     */
    private static Map<Long, List<Long>> buildChildrenMap() {
        Map<Long, List<Long>> children = new HashMap<>();
        for (AdminPermission permission : AdminPermission.all()) {
            if (permission.getParentId() != null) {
                children.computeIfAbsent(permission.getParentId(), k -> new ArrayList<>())
                        .add(permission.getId());
            }
        }
        return children;
    }

    /**
     * 判断 {@code candidate} 是否为 {@code target} 的祖先（或自身）。
     * <p>
     * 即：从 target 沿 parent_id 向上遍历，若途经 candidate 则返回 true。
     * 含 visited 防环保护，避免脏数据导致的死循环。
     * <p>
     * 这是「父节点授权即等同子节点授权」的核心：授权 candidate 时，
     * candidate 的所有后代 target 都会被判定为拥有。
     */
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

    /**
     * 获取某权限的全部后代 ID（含自身），广度优先遍历。
     */
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
    // 9. 中间表私有封装（避免与角色实体 AdminRole 同名冲突，统一全限定名访问）
    // ================================================================

    /** 查询管理员拥有的全部角色 ID */
    private static Set<Long> roleIdsOfAdmin(Long adminId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (com.weacsoft.jaravel.app.model.admin.middle.AdminRole pivot
                : com.weacsoft.jaravel.app.model.admin.middle.AdminRole.findByAdminId(adminId)) {
            ids.add(pivot.getRoleId());
        }
        return ids;
    }

    /** 判断 admin↔role 关联是否存在 */
    private static boolean adminRolePivotExists(Long adminId, Long roleId) {
        return com.weacsoft.jaravel.app.model.admin.middle.AdminRole.exists(adminId, roleId);
    }

    /** 新增 admin↔role 关联记录 */
    private static void attachAdminRole(Long adminId, Long roleId) {
        com.weacsoft.jaravel.app.model.admin.middle.AdminRole pivot
                = new com.weacsoft.jaravel.app.model.admin.middle.AdminRole();
        pivot.setAdminId(adminId);
        pivot.setRoleId(roleId);
        pivot.save();
    }

    /** 删除指定 admin↔role 关联，返回受影响行数 */
    private static int detachAdminRole(Long adminId, Long roleId) {
        return com.weacsoft.jaravel.app.model.admin.middle.AdminRole.query()
                .where("admin_id", adminId).where("role_id", roleId).delete();
    }

    /** 删除管理员的全部 admin↔role 关联，返回受影响行数 */
    private static int detachAllAdminRoles(Long adminId) {
        return com.weacsoft.jaravel.app.model.admin.middle.AdminRole.query()
                .where("admin_id", adminId).delete();
    }

    /** 删除某角色被引用的全部 admin↔role 关联（角色删除时级联） */
    private static int deleteAdminRolePivotsByRole(Long roleId) {
        return com.weacsoft.jaravel.app.model.admin.middle.AdminRole.query()
                .where("role_id", roleId).delete();
    }

    /** 查询角色显式拥有的全部权限 ID */
    private static Set<Long> permissionIdsOfRole(Long roleId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (RolePermission rp : RolePermission.findByRoleId(roleId)) {
            ids.add(rp.getPermissionId());
        }
        return ids;
    }

    /** 查询多个角色显式拥有的全部权限 ID（并集） */
    private static Set<Long> permissionIdsOfRoles(Set<Long> roleIds) {
        Set<Long> ids = new HashSet<>();
        for (Long roleId : roleIds) {
            ids.addAll(permissionIdsOfRole(roleId));
        }
        return ids;
    }

    /** 删除某角色被引用的全部 role↔permission 关联（角色删除时级联） */
    private static int deleteRolePermissionPivotsByRole(Long roleId) {
        return RolePermission.query().where("role_id", roleId).delete();
    }

    /** 删除某权限被引用的全部 role↔permission 关联（权限删除时级联） */
    private static int deleteRolePermissionPivotsByPermission(Long permissionId) {
        return RolePermission.query().where("permission_id", permissionId).delete();
    }
}
