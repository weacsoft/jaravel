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
 *     <li>{@link #verify(String, String, String)} 校验用户输入；</li>
 *     <li>{@link #getAvailableTypes()} 获取所有已注册的验证码类型。</li>
 * </ul>
 * <p>
 * 同时支持静态调用，无需注入即可使用：
 * <pre>
 *   CaptchaResult result = CaptchaService.generateStatic("number");
 *   boolean ok = CaptchaService.verifyStatic("number", captchaKey, userInput);
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
     * 校验验证码。
     *
     * @param type        验证码类型
     * @param captchaKey  生成时返回的验证码 key
     * @param userInput   用户输入（文本输入，或包含 value+trajectory 的 JSON，或包含 clicks 的 JSON）
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean verify(String type, String captchaKey, String userInput) {
        return captchaManager.verify(type, captchaKey, userInput);
    }

    /**
     * 校验验证码（详细结果）。
     * <p>
     * 区分"验证失败"和"验证码已被使用"两种情况，便于前端给出不同的提示。
     *
     * @param type        验证码类型
     * @param captchaKey  验证码 key
     * @param userInput   用户输入
     * @return 验证结果（含是否通过、是否已被使用）
     */
    public VerifyResult verifyDetailed(String type, String captchaKey, String userInput) {
        return captchaManager.verifyDetailed(type, captchaKey, userInput, null, null);
    }

    /**
     * 校验验证码（带运行时加密密钥）。
     *
     * @param type          验证码类型
     * @param captchaKey    验证码 key
     * @param userInput     用户输入
     * @param encryptionKey 运行时加密密钥
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean verify(String type, String captchaKey, String userInput, String encryptionKey) {
        return captchaManager.verify(type, captchaKey, userInput, encryptionKey);
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
     * 静态方法：校验验证码。
     *
     * @param type       验证码类型
     * @param captchaKey 验证码 key
     * @param userInput  用户输入
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean verifyStatic(String type, String captchaKey, String userInput) {
        return CaptchaManager.verifyStatic(type, captchaKey, userInput);
    }

    /**
     * 静态方法：校验验证码（带运行时加密密钥）。
     *
     * @param type          验证码类型
     * @param captchaKey    验证码 key
     * @param userInput     用户输入
     * @param encryptionKey 运行时加密密钥
     * @return 校验通过返回 true，否则返回 false
     */
    public static boolean verifyStatic(String type, String captchaKey, String userInput, String encryptionKey) {
        return CaptchaManager.verifyStatic(type, captchaKey, userInput, encryptionKey);
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
