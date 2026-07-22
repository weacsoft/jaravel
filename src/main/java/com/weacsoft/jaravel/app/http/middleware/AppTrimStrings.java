package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.http.middleware.TrimStrings;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 应用级字符串裁剪中间件，继承 {@link TrimStrings} 并通过覆盖 {@link #except()} 自定义排除字段。
 * <p>
 * 标注 {@code @MiddlewareAlias}（无别名）后由 SpringBoot classpath 扫描自动发现并注册，
 * 路由中通过类对象 {@code AppTrimStrings.class} 引用。
 */
@MiddlewareAlias
public class AppTrimStrings extends TrimStrings {
    @Override
    protected String[] except() {
        return new String[]{"password", "password_confirmation"};
    }
}
