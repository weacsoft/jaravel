package com.weacsoft.jaravel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Jaravel 标准模板应用入口。
 * <p>
 * 扫描 com.weacsoft.jaravel 包（含 config、routes、app/controller、app/service、app/model 等 Laravel 风格分层），
 * 同时由 jaravel-starter 自动装配框架能力。
 * <p>
 * 排除 {@link DataSourceAutoConfiguration}，因为数据源由 {@code config/Database.java} 手动创建
 * （DruidDataSource + GaarasonDataSource），不使用 Spring Boot 的自动数据源装配。
 */
@SpringBootApplication(scanBasePackages = "com.weacsoft.jaravel", exclude = { DataSourceAutoConfiguration.class })
public class JaravelApplication {

    public static void main(String[] args) {
        SpringApplication.run(JaravelApplication.class, args);
    }
}
