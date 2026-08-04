package com.weacsoft.jaravel.app.model;

import com.weacsoft.jaravel.vendor.database.BaseModel;
import gaarason.database.annotation.Column;
import gaarason.database.annotation.Primary;
import gaarason.database.annotation.Table;
import gaarason.database.contract.eloquent.Record;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Repository;

/**
 * 任务模型，用于 Wire 列表演示（基于 SQLite 真实数据库）。
 * <p>
 * 对齐 Laravel Eloquent 的 {@code app/Models/Task.php}。
 * <pre>
 * // 创建新记录
 * Task task = new Task();
 * task.setName("任务 A");
 * task.setDone(false);
 * task.save();
 *
 * // 查询
 * Task found = Task.self().find(1L).toObject();
 *
 * // 分页（使用 BaseModel 内置 paginate）
 * Paginator&lt;Task&gt; result = Task.self().paginate(1, 5);
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@Table(name = "tasks")
public class Task extends BaseModel<Task, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "done")
    private Boolean done;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    // ---- 静态入口方法 ----

    /** 获取 Spring 管理的实例，可调用所有 gaarason 方法 */
    public static Task self() {
        return BaseModel.self(Task.class);
    }

    /** 按 id 查询单条 */
    public static Task findById(long id) {
        Record<Task, Long> record = self().find(id);
        return record == null ? null : record.toObject();
    }

    /** 总数 */
    public static long count() {
        return self().newQuery().count();
    }
}
