package com.weacsoft.jaravel.app.http.middleware;

import com.weacsoft.jaravel.vendor.http.middleware.ConvertEmptyStringsToNull;
import com.weacsoft.jaravel.vendor.springboot.annotation.MiddlewareAlias;

/**
 * 应用级空字符串转 Null 中间件，继承 {@link ConvertEmptyStringsToNull}。
 * <p>
 * 使用默认排除列表（password 等），无需覆盖 {@link #except()}。
 * 标注 {@code @MiddlewareAlias}（无别名）后由 SpringBoot classpath 扫描自动发现并注册，
 * 路由中通过类对象 {@code AppConvertEmptyStringsToNull.class} 引用。
 */
@MiddlewareAlias
public class AppConvertEmptyStringsToNull extends ConvertEmptyStringsToNull {
    // 使用父类默认排除列表，无需覆盖
}
