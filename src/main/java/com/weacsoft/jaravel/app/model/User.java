package com.weacsoft.jaravel.app.model;

import com.weacsoft.jaravel.vendor.auth.contract.Authenticatable;
import com.weacsoft.jaravel.vendor.database.BaseModel;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Primary;
import gaarason.database.annotation.Table;
import gaarason.database.contract.eloquent.Record;
import gaarason.database.query.QueryBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户模型，对齐 Laravel Eloquent 的 {@code app/Models/User.php}。
 * <p>
 * 单一类即包含数据字段与查询能力，无需分离 Entity 和 Model：
 * <pre>
 * // 创建新记录
 * User user = new User();
 * user.setName("alice");
 * user.save();
 *
 * // 静态查询
 * User found = User.find(1L);
 * List&lt;User&gt; all = User.all();
 * User u = User.query().where("name", "alice").first().toObject();
 *
 * // 克隆
 * User clone = user.replicate();
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "users")
public class User extends BaseModel<User, Long> implements Authenticatable {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "number")
    private String number;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    // ---- 静态查询方法（委托给 BaseModel 工具方法） ----

    /** 按主键查询，对齐 Laravel User::find(1) */
    public static User find(Long id) {
        return BaseModel.find(User.class, id);
    }

    /** 查询全部，对齐 Laravel User::all() */
    public static List<User> all() {
        return BaseModel.all(User.class);
    }

    /** 获取查询构造器，对齐 Laravel User::query() */
    public static QueryBuilder<User, Long> query() {
        return BaseModel.query(User.class);
    }

    /** 按工号查询，对齐 Laravel User::where('number', x)->first() */
    public static User findByNumber(String number) {
        Record<User, Long> record = query().where("number", number).first();
        return record == null ? null : record.toObject();
    }

    // ---- Authenticatable ----
    // 仅实现以主键标识用户的方法；密码校验由应用层（Service/Controller）负责，
    // Authenticatable 契约不再包含 getAuthPassword()。

    @Override
    public Object getAuthIdentifier() {
        return id;
    }

    @Override
    public String getAuthIdentifierName() {
        return "id";
    }
}
