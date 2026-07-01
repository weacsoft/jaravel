package com.weacsoft.jaravel.app.model;

import com.weacsoft.jaravel.vendor.database.BaseModel;
import com.weacsoft.jaravel.vendor.database.DataSource;
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
 * 产品模型，对齐 Laravel Eloquent 的 {@code app/Models/Product.php}。
 * <p>
 * 通过 {@link DataSource} 注解指定使用第二数据源（{@code secondaryDataSource}），
 * 用于多数据库测试场景。查询方法委托给 {@link BaseModel} 的静态工具方法，
 * 由 {@link BaseModel#getGaarasonDataSource()} 根据 {@code @DataSource} 注解
 * 动态选择对应的数据源 Bean。
 * <p>
 * 使用方式与 {@link User} 一致：
 * <pre>
 * Product p = Product.find(1L);
 * List&lt;Product&gt; all = Product.all();
 * </pre>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Repository
@DataSource("secondaryGaarasonDataSource")
@Table(name = "products")
public class Product extends BaseModel<Product, Long> {

    @Primary
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Double price;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    // ---- 静态查询方法（委托给 BaseModel 工具方法） ----

    /** 按主键查询，对齐 Laravel Product::find(1) */
    public static Product find(Long id) {
        return BaseModel.find(Product.class, id);
    }

    /** 查询全部，对齐 Laravel Product::all() */
    public static List<Product> all() {
        return BaseModel.all(Product.class);
    }

    /** 获取查询构造器，对齐 Laravel Product::query() */
    public static QueryBuilder<Product, Long> query() {
        return BaseModel.query(Product.class);
    }

    /** 按名称查询，对齐 Laravel Product::where('name', x)->first() */
    public static Product findByName(String name) {
        Record<Product, Long> record = query().where("name", name).first();
        return record == null ? null : record.toObject();
    }
}
