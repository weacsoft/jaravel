package com.weacsoft.jaravel.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.weacsoft.jaravel.vendor.core.SpringContext;
import com.weacsoft.jaravel.vendor.database.ConnectionManager;
import com.weacsoft.jaravel.vendor.database.RegisterConnection;
import gaarason.database.bootstrap.ContainerBootstrap;
import gaarason.database.connection.GaarasonDataSourceBuilder;
import gaarason.database.contract.connection.GaarasonDataSource;
import gaarason.database.provider.ModelInstanceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;

/**
 * 数据库配置（对齐 Laravel 的 {@code config/database.php}）。
 * <p>
 * 由 {@code artisan vendor:publish --tag=database} 发布生成，可自由修改。
 *
 * <h2>一、两层来源：声明 → 配置（database <b>没有</b>兜底默认）</h2>
 * <ol>
 *   <li><b>声明</b>：本类中的 {@link RegisterConnection @RegisterConnection} 方法（本项目采用）</li>
 *   <li><b>配置</b>：{@code application.yml} 的 {@code spring.datasource.*}（下方方式二）</li>
 *   <li><b>默认</b>：<b>没有兜底</b>。数据库无法凭空造一个能用的默认值，
 *       一个连接都没配却又去用 {@code DB} / Model，会<b>直接报错</b>，而不是静默降级</li>
 * </ol>
 *
 * <h2>二、"安装 ≠ 启用"</h2>
 * 引入 database 模块<b>不等于</b>会创建连接。只有本类注册了连接（或配了
 * {@code spring.datasource}）才会真正建连；其他模块的 {@code database} 驱动
 * （cache / queue / storage）也都是显式写了 {@code driver: database} 才装配，
 * 因此"引入了 database 模块但不用数据库"的项目可以正常启动。
 *
 * <h2>三、ContainerBootstrap（务必只有一个）</h2>
 * gaarason 在 SpringBoot 环境下，每个 {@code GaarasonDataSource} 都<b>必须</b>携带
 * {@code ContainerBootstrap}，且所有数据源必须<b>共用同一个实例</b>，
 * 否则 Model 注册表、类型转换器会分裂到不同容器，导致查询报错。
 * 因此下方所有连接方法都注入同一个 {@code bootstrap} 参数，切勿在别处再次
 * {@code ContainerBootstrap.build()}。
 *
 * <h2>四、为什么用 &#64;RegisterConnection 而不是 &#64;Bean</h2>
 * 与 auth 的 {@code @RegisterGuard} 一致：连接别名与 Spring bean name 解耦，
 * 别名可自由取名（如 {@code mysql}），不会触发 BeanDefinitionOverrideException。
 * Model 通过 {@code @DataSource("别名")} 使用时，框架<b>先查别名注册表</b>，
 * 找不到才回退 Spring 容器。
 */
@Configuration
public class DatabaseConfig {

    /**
     * 创建并初始化全局唯一的 gaarason Container。
     * <p>
     * 注册自定义 {@link ModelInstanceProvider}，使 gaarason 需要 Model 实例时
     * 通过 Spring 容器获取托管单例；随后存入 {@link ConnectionManager}，
     * 供框架内部（迁移、seeder、DB 门面等）复用同一实例。
     */
    @Bean
    public ContainerBootstrap containerBootstrap(@Autowired Environment env) {
        String scanPackages = env.getProperty("gaarason.database.scan.packages",
                "com.weacsoft.jaravel.app.model");
        if (System.getProperty("gaarason.database.scan.packages") == null) {
            System.setProperty("gaarason.database.scan.packages", scanPackages);
        }

        ContainerBootstrap bootstrap = ContainerBootstrap.build();
        bootstrap.defaultRegister();

        ModelInstanceProvider modelInstanceProvider = bootstrap.getBean(ModelInstanceProvider.class);
        modelInstanceProvider.register(modelClass -> SpringContext.bean(modelClass));

        bootstrap.bootstrapGaarasonAutoconfiguration();
        bootstrap.initialization();

        // 存入框架门面，保证全框架自始至终使用同一个 ContainerBootstrap
        ConnectionManager.setContainer(bootstrap);
        return bootstrap;
    }

    /**
     * 默认连接（别名 primary）。
     * <p>
     * Model 未标注 {@code @DataSource} 时使用本连接。用同一个 {@code bootstrap}
     * 把主库 {@link DataSource} 包装为 {@link GaarasonDataSource}。
     *
     * <h3>不需要再写 &#64;Bean DataSource</h3>
     * 框架会自动把<b>默认连接</b>以惰性委托的形式注册为 &#64;Primary 的 Spring
     * {@link DataSource} Bean，供事务管理器、JdbcTemplate 以及各类
     * {@code @ConditionalOnBean(DataSource.class)} 使用。
     * <p>
     * 默认连接 = 标记了 {@code defaultConnection = true} 的连接；
     * 若一个都没标记，则<b>第一个注册的连接</b>自动成为默认连接。
     */
    @RegisterConnection(value = "primary", defaultConnection = true)
    public GaarasonDataSource primaryConnection(Environment env, ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.url", "jdbc:sqlite:database.sqlite"));
        druid.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", "org.sqlite.JDBC"));
        druid.setUsername(env.getProperty("spring.datasource.username", ""));
        druid.setPassword(env.getProperty("spring.datasource.password", ""));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }

    // =====================================================================
    // 方式一（续）：额外连接 —— 多数据源
    // =====================================================================
    // Model 上写 @DataSource("mysql")、迁移里写 connection() { return "mysql"; } 即可使用。
    // 额外连接无需注册为 Spring Bean，别名可自由取名，不会与同名 bean 冲突。
    // 注意：这里复用同一个 bootstrap 参数，切勿另行 build()。
    //
    // @RegisterConnection("mysql")
    // public GaarasonDataSource mysqlConnection(Environment env, ContainerBootstrap bootstrap) {
    //     DruidDataSource druid = new DruidDataSource();
    //     druid.setUrl(env.getProperty("jaravel.database.mysql.url"));
    //     druid.setDriverClassName("com.mysql.cj.jdbc.Driver");
    //     druid.setUsername(env.getProperty("jaravel.database.mysql.username"));
    //     druid.setPassword(env.getProperty("jaravel.database.mysql.password"));
    //     return GaarasonDataSourceBuilder.build(druid, bootstrap);
    // }
    //
    // @RegisterConnection("pgsql")
    // public GaarasonDataSource pgsqlConnection(Environment env, ContainerBootstrap bootstrap) {
    //     DruidDataSource druid = new DruidDataSource();
    //     druid.setUrl(env.getProperty("jaravel.database.pgsql.url"));
    //     druid.setDriverClassName("org.postgresql.Driver");
    //     druid.setUsername(env.getProperty("jaravel.database.pgsql.username"));
    //     druid.setPassword(env.getProperty("jaravel.database.pgsql.password"));
    //     return GaarasonDataSourceBuilder.build(druid, bootstrap);
    // }

    // =====================================================================
    // 方式二：配置式 —— 完全不写本类，只配 application.yml
    // =====================================================================
    // 单数据源场景可以删掉本类，框架会用 spring.datasource 自动建默认连接：
    //
    // spring:
    //   datasource:
    //     url: jdbc:sqlite:database1.sqlite
    //     driver-class-name: org.sqlite.JDBC
    //     username: ""
    //     password: ""
    //
    // 多数据源仍建议用 @RegisterConnection，因为别名与 bean name 解耦。

    // =====================================================================
    // 方式三：无兜底 —— 一个连接都不配会怎样
    // =====================================================================
    // database 模块<b>没有</b>默认驱动可兜底：
    //   - 不配连接、也不用 DB / Model  -> 应用正常启动（安装 ≠ 启用）
    //   - 不配连接、却调用 DB / Model  -> 直接抛异常，提示未配置数据库连接
    // 这是刻意为之：数据库连接错了必须尽早暴露，不能静默降级到某个"假"数据源。
}
