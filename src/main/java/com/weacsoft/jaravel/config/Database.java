package com.weacsoft.jaravel.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.weacsoft.jaravel.vendor.core.SpringContext;
import gaarason.database.bootstrap.ContainerBootstrap;
import gaarason.database.connection.GaarasonDataSourceBuilder;
import gaarason.database.contract.connection.GaarasonDataSource;
import gaarason.database.provider.ModelInstanceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * 数据库配置，手动创建 GaarasonDataSource（对齐 Laravel 的 {@code config/database.php}）。
 * <p>
 * 支持多数据源：主数据源（默认）与第二数据源（secondary），两个数据源共享同一个
 * {@link ContainerBootstrap}（gaarason 容器），通过 {@code @DataSource} 注解在 Model 上
 * 指定使用哪个数据源。
 * <p>
 * GaarasonDataSource 实现了 {@link javax.sql.DataSource} 接口，因此迁移系统可直接使用
 * GaarasonDataSource Bean 作为 JDBC DataSource。迁移类通过 {@code getDataSourceName()}
 * 指定使用哪个数据源 Bean。
 * <p>
 * 关键步骤：手动创建 {@link ContainerBootstrap}，在 {@code initialization()} 之前注册
 * 自定义的 {@link ModelInstanceProvider} 实例化函数，使 gaarason Container 在需要
 * Model 实例时通过 Spring {@code ApplicationContext.getBean()} 获取 Spring 管理的单例
 * （带有 {@code @Autowired} 注入的数据源），而非通过反射 {@code newInstance()} 创建
 * 未受 Spring 管理的裸实例。
 */
@Configuration
public class Database {

    /**
     * 创建并初始化 gaarason Container（共享容器）。
     * 两个数据源共用同一个 Container，注册自定义 Model 实例化策略后完成引导。
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

        return bootstrap;
    }

    // ==================== 主数据源（Primary） ====================

    /**
     * 主 GaarasonDataSource，供 ORM 和迁移系统使用。
     * 标记 @Primary 使 BaseModel 默认注入此数据源，迁移系统默认使用此数据源。
     */
    @Bean
    @Primary
    public GaarasonDataSource gaarasonDataSource(@Autowired Environment env,
                                                  @Autowired ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.url", "jdbc:sqlite:database.sqlite"));
        druid.setDriverClassName(env.getProperty("spring.datasource.driver-class-name", "org.sqlite.JDBC"));
        druid.setUsername(env.getProperty("spring.datasource.username", ""));
        druid.setPassword(env.getProperty("spring.datasource.password", ""));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }

    // ==================== 第二数据源（Secondary） ====================

    /**
     * 第二 GaarasonDataSource，供 ORM 和迁移系统使用。
     * Model 通过 @DataSource("secondaryGaarasonDataSource") 注解指定使用此数据源。
     * 迁移类通过 getDataSourceName() 返回 "secondaryGaarasonDataSource" 指定使用此数据源。
     */
    @Bean("secondaryGaarasonDataSource")
    public GaarasonDataSource secondaryGaarasonDataSource(@Autowired Environment env,
                                                           @Autowired ContainerBootstrap bootstrap) {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(env.getProperty("spring.datasource.secondary.url", "jdbc:sqlite:database2.sqlite"));
        druid.setDriverClassName(env.getProperty("spring.datasource.secondary.driver-class-name", "org.sqlite.JDBC"));
        druid.setUsername(env.getProperty("spring.datasource.secondary.username", ""));
        druid.setPassword(env.getProperty("spring.datasource.secondary.password", ""));
        return GaarasonDataSourceBuilder.build(druid, bootstrap);
    }
}
