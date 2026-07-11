package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.app.service.CaptchaService;
import com.weacsoft.jaravel.vendor.captcha.CaptchaResult;
import com.weacsoft.jaravel.vendor.captcha.VerifyResult;
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
 * <p>
 * API：
 * <ul>
 *   <li>GET  /api/captcha/generate?type=rotate  — 生成验证码</li>
 *   <li>POST /api/captcha/verify               — 校验验证码</li>
 * </ul>
 */
@Controller
public class CaptchaController implements Controllers {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    @Autowired
    private CaptchaService captchaService;

    /**
     * 生成验证码。
     * <p>
     * 请求参数：type = number | arithmetic | slider | rotate | click（默认 rotate）
     * 响应格式：{code: 200, data: {captchaKey, type, imageBase64, expireTime, extra}}
     */
    public Response generate(Request request) {
        String type = request.get("type", "rotate");
        try {
            CaptchaResult result = captchaService.generate(type);

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
            log.error("[captcha] 生成验证码失败: type={}", type, e);
            return ResponseBuilder.raw()
                    .status(500)
                    .contentType("application/json; charset=utf-8")
                    .body(ResponseBuilder.toJson(Map.of("code", 500, "msg", "验证码生成失败: " + e.getMessage())));
        }
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
