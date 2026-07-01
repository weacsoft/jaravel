package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.captcha.CaptchaManager;
import com.weacsoft.jaravel.vendor.captcha.CaptchaResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 验证码演示控制器。
 * <p>
 * 使用标准 Spring MVC（{@code @RestController} + {@code @RequestMapping}），
 * 与 jaravel 自定义 RouterFunction 路由并存（两者由不同的 HandlerMapping 处理，互不冲突）。
 * <p>
 * 引入 {@code captcha} 依赖后，{@link CaptchaManager} 由 {@code CaptchaAutoConfiguration}
 * 自动装配（前缀 {@code jaravel.captcha}），注册数字、算术、滑动、旋转四种验证码类型。
 * <p>
 * 提供接口：
 * <ul>
 *   <li>{@code GET  /api/captcha/generate?type=number|arithmetic|slider|rotate} 生成验证码</li>
 *   <li>{@code POST /api/captcha/verify} 验证用户输入</li>
 *   <li>{@code GET  /api/captcha/types} 获取所有支持的验证码类型</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    @Autowired
    private CaptchaManager captchaManager;

    /**
     * 生成验证码。
     * <p>
     * GET /api/captcha/generate?type=number|arithmetic|slider|rotate
     *
     * @param type 验证码类型，默认 number
     * @return 验证码信息（captchaKey、图片 base64、过期时间、额外数据等）
     */
    @GetMapping("/generate")
    public Map<String, Object> generate(@RequestParam(defaultValue = "number") String type) {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        CaptchaResult result = captchaManager.generate(type, captchaKey);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("captchaKey", captchaKey);
        data.put("type", type);
        data.put("imageBase64", result.getImageBase64());
        data.put("token", result.getToken());
        data.put("expireTime", result.getExpireTime());
        data.put("extra", result.getExtra());
        return data;
    }

    /**
     * 验证用户输入。
     * <p>
     * POST /api/captcha/verify
     * <pre>
     * body: {"captchaKey": "xxx", "input": "用户输入", "type": "number"}
     * </pre>
     * <p>
     * 滑动/旋转验证码启用轨迹验证时，{@code input} 需为 JSON 字符串：
     * {@code {"value": 123, "trajectory": [{"t":0,"v":0},...]}}
     *
     * @param body 请求体（captchaKey、input、type）
     * @return 验证结果
     */
    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, Object> body) {
        String captchaKey = (String) body.get("captchaKey");
        String input = (String) body.get("input");
        String type = (String) body.getOrDefault("type", "number");

        boolean ok = captchaManager.verify(type, captchaKey, input);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", ok);
        data.put("message", ok ? "验证通过" : "验证失败");
        return data;
    }

    /**
     * 获取所有支持的验证码类型。
     * <p>
     * GET /api/captcha/types
     *
     * @return 已注册的验证码类型集合
     */
    @GetMapping("/types")
    public Map<String, Object> types() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("types", captchaManager.getTypes());
        return data;
    }
}
