package com.weacsoft.jaravel;

import com.weacsoft.jaravel.vendor.artisan.ArtisanApplication;
import com.weacsoft.jaravel.vendor.artisan.ArtisanRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Jaravel 标准模板应用入口。
 * <p>
 * 扫描 com.weacsoft.jaravel 包（含 config、routes、app/controller、app/service、app/model 等 Laravel 风格分层），
 * 同时由 jaravel-starter 自动装配框架能力。
 * <p>
 * 排除 {@link DataSourceAutoConfiguration}，因为数据源由 {@code config/Database.java} 手动创建
 * （DruidDataSource + GaarasonDataSource），不使用 Spring Boot 的自动数据源装配。
 *
 * <h3>两种运行模式</h3>
 * <ul>
 *   <li><b>HTTP 模式</b>（默认）：{@code java -jar jaravel.jar}，启动 Web 服务</li>
 *   <li><b>Artisan 模式</b>：{@code java -jar jaravel.jar artisan <命令>}，
 *       不启动 HTTP 服务，仅执行命令行任务后退出</li>
 * </ul>
 *
 * <h3>常用 artisan 命令</h3>
 * <pre>
 * java -jar jaravel.jar artisan                            # 列出所有命令
 * java -jar jaravel.jar artisan vendor:publish --list      # 列出可发布配置
 * java -jar jaravel.jar artisan vendor:publish --tag=cache # 发布 cache 配置类
 * java -jar jaravel.jar artisan migrate                    # 执行数据库迁移
 * </pre>
 */
@SpringBootApplication(scanBasePackages = "com.weacsoft.jaravel", exclude = { DataSourceAutoConfiguration.class })
public class JaravelApplication {

    public static void main(String[] args) {
        if (ArtisanRunner.isArtisanMode(args)) {
            runArtisan(args);
        } else {
            SpringApplication.run(JaravelApplication.class, args);
        }
    }

    /**
     * Artisan 模式：以 {@link WebApplicationType#NONE} 启动容器，
     * 执行命令后关闭上下文并以命令退出码结束进程。
     * <p>
     * 不启动 HTTP 服务，避免与已运行的实例争抢端口。
     */
    private static void runArtisan(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(JaravelApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        int exitCode;
        try {
            exitCode = ArtisanRunner.run(context.getBean(ArtisanApplication.class), args);
        } finally {
            context.close();
        }
        System.exit(exitCode);
    }
}
