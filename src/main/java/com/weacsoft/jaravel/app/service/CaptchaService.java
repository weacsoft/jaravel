package com.weacsoft.jaravel.app.service;

import com.weacsoft.jaravel.vendor.captcha.CaptchaManager;
import com.weacsoft.jaravel.vendor.captcha.CaptchaProperties;
import com.weacsoft.jaravel.vendor.captcha.CaptchaResult;
import com.weacsoft.jaravel.vendor.captcha.VerifyResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 验证码服务。
 * <p>
 * 对 {@link CaptchaManager} 进行封装，便于在 jaravel 业务层中便捷地生成与校验验证码。
 * <p>
 * 主要能力：
 * <ul>
 *     <li>{@link #generate(String)} 生成指定类型的验证码；</li>
 *     <li>{@link #verify(String, String)} 用「合并凭证 + 用户输入」两个参数校验；</li>
 *     <li>{@link #getAvailableTypes()} 获取所有已注册的验证码类型。</li>
 * </ul>
 * <p>
 * 校验只接收<b>两个</b>参数：合并凭证 {@code key}（生成时下发，格式 {@code type.captchaKey}）
 * 与用户输入。验证码类型已编码在凭证中，因此可以把验证码与登录表单等业务字段
 * 一次性提交、一次性校验，不需要「先校验验证码、再提交业务」的两段式请求
 * （两段式存在拿到"已通过"状态后重放业务请求的时间窗）。
 * <p>
 * 同时支持静态调用，无需注入即可使用：
 * <pre>
 *   CaptchaResult result = CaptchaService.generateStatic("number");
 *   boolean ok = CaptchaService.verifyStatic(result.getKey(), userInput);
 * </pre>
 *
 * @see CaptchaManager
 */
@Service
public class CaptchaService {

    @Autowired
    private CaptchaManager captchaManager;

    /**
     * 生成指定类型的验证码。
     *
     * @param type 验证码类型，支持：number、arithmetic、slider、rotate、click
     * @return 验证码生成结果 {@link CaptchaResult}
     */
    public CaptchaResult generate(String type) {
        return captchaManager.generate(type);
    }

    /**
     * 生成验证码（带运行时配置覆盖）。
     *
     * @param type      验证码类型
     * @param overrides 运行时配置覆盖
     * @return 验证码生成结果
     */
    public CaptchaResult generate(String type, CaptchaProperties overrides) {
        return captchaManager.generate(type, overrides);
    }

    /**
     * 生成验证码（带运行时配置覆盖和加密密钥）。
     *
     * @param type          验证码类型
     * @param overrides     运行时配置覆盖
     * @param encryptionKey 运行时加密密钥
     * @return 验证码生成结果
     */
    public CaptchaResult generate(String type, CaptchaProperties overrides, String encryptionKey) {
        return captchaManager.generate(type, overrides, encryptionKey);
    }

    /**
     * 用<b>合并凭证</b>校验验证码。
     * <p>
     * 合并凭证 {@code key} 由生成时返回（{@link CaptchaResult#getKey()}，
     * 格式 {@code type + "." + captchaKey}），内部自动解析类型并分发，
     * 调用方无需再传 {@code type}。
     * 这样验证码可与其他业务表单字段放在<b>同一次请求</b>里一起提交、一起校验，
     * 避免「先单独校验验证码、再提交业务数据」的二次提交安全漏洞。
     *
     * @param key        合并凭证（生成时返回的 {@code key} 字段）
     * @param userInput  用户输入（文本，或含 value+trajectory 的 JSON，或含 clicks 的 JSON）
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean verify(String key, String userInput) {
        return captchaManager.verify(key, userInput);
    }

    /**
     * 用合并凭证校验验证码（带运行时加密密钥）。
     *
     * @param key           合并凭证
     * @param userInput     用户输入
     * @param encryptionKey 运行时加密密钥
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean verify(String key, String userInput, String encryptionKey) {
        return captchaManager.verify(key, userInput, encryptionKey);
    }

    /**
     * 用合并凭证校验验证码（详细结果）。
     * <p>
     * 区分"验证失败"和"验证码已被使用"两种情况，便于前端给出不同的提示。
     *
     * @param key        合并凭证（生成时返回的 {@code key} 字段）
     * @param userInput  用户输入
     * @return 验证结果（含是否通过、是否已被使用）
     */
    public VerifyResult verifyDetailed(String key, String userInput) {
        return captchaManager.verifyDetailed(key, userInput, null, null);
    }

    /**
     * 获取所有已注册的验证码类型。
     *
     * @return 已注册验证码类型集合
     */
    public Set<String> getAvailableTypes() {
        return captchaManager.getTypes();
    }

    // ==================== 静态方法 ====================

    /**
     * 静态方法：生成验证码。
     * <p>
     * 使用 {@link CaptchaManager#getDefault()} 获取默认管理器实例。
     *
     * @param type 验证码类型
     * @return 验证码生成结果
     */
    public static CaptchaResult generateStatic(String type) {
        return CaptchaManager.generateStatic(type);
    }

    /**
     * 静态方法：生成验证码（带运行时配置覆盖）。
     *
     * @param type      验证码类型
     * @param overrides 运行时配置覆盖
     * @return 验证码生成结果
     */
    public static CaptchaResult generateStatic(String type, CaptchaProperties overrides) {
        return CaptchaManager.generateStatic(type, overrides);
    }

    /**
     * 静态方法：生成验证码（带运行时配置覆盖和加密密钥）。
     *
     * @param type          验证码类型
     * @param overrides     运行时配置覆盖
     * @param encryptionKey 运行时加密密钥
     * @return 验证码生成结果
     */
    public static CaptchaResult generateStatic(String type, CaptchaProperties overrides, String encryptionKey) {
        return CaptchaManager.generateStatic(type, overrides, encryptionKey);
    }

    /**
     * 静态方法：用合并凭证校验验证码。
     *
     * @param key       合并凭证（生成时返回的 {@code key} 字段）
     * @param userInput 用户输入
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean verifyStatic(String key, String userInput) {
        return CaptchaManager.verifyStatic(key, userInput);
    }

    /**
     * 静态方法：用合并凭证校验验证码（带运行时加密密钥）。
     *
     * @param key           合并凭证
     * @param userInput     用户输入
     * @param encryptionKey 运行时加密密钥
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean verifyStatic(String key, String userInput, String encryptionKey) {
        return CaptchaManager.verifyStatic(key, userInput, encryptionKey);
    }

    // ==================== 前端资源（JS/CSS 内容） ====================

    /** 前端 JS 内容缓存（类加载后只需读取一次） */
    private static volatile String _jsContent = null;

    /** classpath 中前端 JS 文件的路径 */
    private static final String JS_RESOURCE_PATH = "static/jaravel-captcha.js";

    /**
     * 获取验证码前端 JS 库的完整内容（含内嵌 CSS）。
     * <p>
     * 从 classpath 读取 {@code static/jaravel-captcha.js} 文件内容并返回，
     * 可用于将 JS 内联到 HTML 页面中，无需额外的静态资源服务。
     * <p>
     * 首次调用时从 classpath 加载，后续直接返回缓存。
     *
     * @return JS 文件内容字符串
     * @throws RuntimeException 如果资源文件不存在或读取失败
     */
    public static String getCaptchaJsContent() {
        if (_jsContent != null) {
            return _jsContent;
        }
        synchronized (CaptchaService.class) {
            if (_jsContent != null) {
                return _jsContent;
            }
            _jsContent = loadClasspathResource(JS_RESOURCE_PATH);
            return _jsContent;
        }
    }

    /**
     * 获取验证码前端 CSS 内容。
     * <p>
     * CSS 内嵌在 JS 中，通过 {@code style.textContent = `...`} 动态注入。
     * 此方法提取该模板字符串中的 CSS 内容。
     * 如果 JS 中没有内嵌 CSS，返回空字符串。
     *
     * @return CSS 内容字符串
     */
    public static String getCaptchaCssContent() {
        String js = getCaptchaJsContent();
        // CSS 通过 style.textContent = `...` 注入，提取反引号之间的内容
        String marker = "style.textContent = `";
        int start = js.indexOf(marker);
        if (start < 0) return "";
        start += marker.length();
        int end = js.indexOf("`;", start);
        if (end < 0) return "";
        return js.substring(start, end).trim();
    }

    /**
     * 从 classpath 加载资源文件内容。
     *
     * @param path classpath 路径
     * @return 文件内容字符串
     * @throws RuntimeException 如果资源不存在或读取失败
     */
    private static String loadClasspathResource(String path) {
        try (InputStream is = CaptchaService.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Classpath 资源不存在: " + path);
            }
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取 classpath 资源失败: " + path, e);
        }
    }
}
