package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.service.CaptchaService;
import com.weacsoft.jaravel.vendor.captcha.CaptchaProperties;
import com.weacsoft.jaravel.vendor.captcha.CaptchaResult;
import com.weacsoft.jaravel.vendor.captcha.VerifyResult;
import com.weacsoft.jaravel.vendor.captcha.springboot.CaptchaSceneRegistry;
import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 验证码控制器 — 基于项目自带 captcha 模块。
 * <p>
 * 支持五种验证码类型：number、arithmetic、slider、rotate、click。
 * 采用无状态设计，captchaKey 自包含加密的答案信息，服务端无需存储任何状态。
 * <p>
 * 校验失败返回 HTTP 403（属于前端用户输入错误，而非服务器内部错误）。
 *
 * <h3>配置权限边界（重要）</h3>
 * 本接口<b>不接受任何前端传入的生成参数</b>。所有影响验证码强度的参数
 * （{@code tolerance / noise / interfereLines / length / width / height /
 * clickTargetCount / clickDecoyCount / expireSeconds / encryption*}）
 * 一律来自后端 {@code jaravel.captcha.*} 配置。
 * <p>
 * 前端唯一能影响生成结果的入口是 {@code scene}（场景名），且只能从后端
 * {@code jaravel.captcha.scenes.*} 预声明的<b>白名单</b>中「选择」，不能「设值」；
 * 未命中白名单时静默回落到全局默认配置，绝不因此降低难度。
 * <p>
 * 展示层配置（弹层模式、主题、占位尺寸、文案等）完全由前端 JS 自行处理，
 * 不参与任何后端请求 —— 对齐 anji-plus/captcha 的 CaptchaConfig / CaptchaVO 分离思路。
 * <p>
 * API：
 * <ul>
 *   <li>GET  /api/captcha/generate?type=rotate[&amp;scene=login]  — 生成验证码</li>
 *   <li>POST /api/captcha/verify                                  — 校验验证码</li>
 * </ul>
 */
@Controller
public class CaptchaController implements Controllers {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    @Autowired
    private CaptchaService captchaService;

    /**
     * 场景白名单注册表（由 captcha 模块自动装配，可能为 null）。
     */
    @Autowired(required = false)
    private CaptchaSceneRegistry sceneRegistry;

    /**
     * 生成验证码。
     * <p>
     * 请求参数：
     * <ul>
     *   <li>{@code type} = number | arithmetic | slider | rotate | click（默认 rotate）</li>
     *   <li>{@code scene}（可选）= 后端预声明的场景名，仅用于「选择」后端预设配置</li>
     * </ul>
     * 其余任何查询参数一律被忽略。
     * <p>
     * 响应格式：{code: 200, data: {captchaKey, type, imageBase64, expireTime, extra}}
     */
    public Response generate(Request request) {
        String type = request.get("type", "rotate");
        String scene = request.get("scene", (String) null);
        try {
            // 前端只能「选择」后端预声明的场景，不能「设值」；未命中则用全局默认配置
            CaptchaProperties sceneProps = resolveScene(scene);
            CaptchaResult result = (sceneProps != null)
                    ? captchaService.generate(type, sceneProps)
                    : captchaService.generate(type);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("captchaKey", result.getCaptchaKey());
            data.put("type", result.getType());
            data.put("imageBase64", result.getImageBase64());
            data.put("expireTime", result.getExpireTime());
            data.put("extra", result.getExtra());

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("code", 200);
            ok.put("data", data);
            return ResponseBuilder.json(ok);
        } catch (Exception e) {
            log.error("[captcha] 生成验证码失败: type={}, scene={}", type, scene, e);
            return ResponseBuilder.raw()
                    .status(500)
                    .contentType("application/json; charset=utf-8")
                    .body(ResponseBuilder.toJson(Map.of("code", 500, "msg", "验证码生成失败: " + e.getMessage())));
        }
    }

    /**
     * 解析场景名对应的后端预设配置。
     * <p>
     * 场景名是不可信输入，注册表内部会做字符集与长度校验；
     * 未命中白名单返回 null，调用方回落到全局默认配置。
     *
     * @param scene 前端传入的场景名（可为 null）
     * @return 后端预设配置；未命中返回 null
     */
    private CaptchaProperties resolveScene(String scene) {
        if (sceneRegistry == null || scene == null || scene.isEmpty()) {
            return null;
        }
        CaptchaProperties props = sceneRegistry.resolve(scene);
        if (props == null) {
            log.debug("[captcha] 场景 '{}' 不在白名单内，使用全局默认配置", scene);
        }
        return props;
    }

    /**
     * 校验验证码。
     * <p>
     * 请求体 JSON：{ type: "rotate", captchaKey: "xxx", input: "45" 或 JSON }
     * 成功响应：{code: 200, msg: "验证通过"}
     * 失败响应：HTTP 403，{code: 403, msg: "验证码校验失败，请重试"}
     * 已使用响应：HTTP 410，{code: 410, msg: "验证码已被使用，请刷新后重试"}
     */
    public Response verify(Request request) {
        try {
            Map<String, Object> all = request.all();

            String type = all.get("type") != null ? all.get("type").toString() : "rotate";
            String captchaKey = all.get("captchaKey") != null ? all.get("captchaKey").toString() : "";
            String input = all.get("input") != null ? all.get("input").toString() : "";

            if (captchaKey.isEmpty()) {
                return jsonError(400, "缺少 captchaKey");
            }
            if (input.isEmpty()) {
                return jsonError(400, "缺少验证输入");
            }

            log.debug("[captcha] verify type={}, key={}, input={}", type, captchaKey,
                    input.length() > 100 ? input.substring(0, 100) + "..." : input);

            VerifyResult result = captchaService.verifyDetailed(type, captchaKey, input);
            if (result.isPassed()) {
                Map<String, Object> ok = new LinkedHashMap<>();
                ok.put("code", 200);
                ok.put("msg", "验证通过");
                return ResponseBuilder.json(ok);
            } else if (result.isAlreadyUsed()) {
                // 验证码已被使用（一次性消费），返回 410 Gone
                return jsonError(410, "验证码已被使用，请刷新后重试");
            } else {
                // 校验失败属于前端错误，返回 403
                return jsonError(403, "验证码校验失败，请重试");
            }
        } catch (Exception e) {
            log.error("[captcha] 校验验证码异常", e);
            return jsonError(500, "验证码校验异常: " + e.getMessage());
        }
    }

    /**
     * 返回指定 HTTP 状态码的 JSON 错误响应。
     */
    private Response jsonError(int status, String msg) {
        return ResponseBuilder.raw()
                .status(status)
                .contentType("application/json; charset=utf-8")
                .body(ResponseBuilder.toJson(Map.of("code", status, "msg", msg)));
    }
}
