package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.http.request.Request;
import com.weacsoft.jaravel.vendor.http.response.Response;
import com.weacsoft.jaravel.vendor.http.response.ResponseBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 页面控制器，使用 jblade 模板引擎渲染页面。
 * <p>
 * 演示 jblade 的用法：布局继承（@extends / @section / @yield）、
 * 变量传递（{{ $var }}）、条件判断（@if）、循环（@foreach）、
 * 静态资源指令（@asset）。
 */
@Component
public class PageController {

    /**
     * 首页 — 演示 jblade 基本变量输出和布局继承。
     */
    public Response index(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "jaravel 插件平台");
        data.put("page", "index");
        data.put("appName", "jaravel");
        data.put("description", "多租户 Jar/Java 热更新在线运行平台");
        return ResponseBuilder.view("index", data);
    }

    /**
     * 管理员页面 — 演示 jblade 条件判断和变量传递。
     */
    public Response admin(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "管理员后台 - jaravel");
        data.put("page", "admin");
        data.put("appName", "jaravel");
        return ResponseBuilder.view("admin", data);
    }

    /**
     * 用户页面 — 演示 jblade 循环和复杂数据传递。
     */
    public Response user(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "用户中心 - jaravel");
        data.put("page", "user");
        data.put("appName", "jaravel");
        return ResponseBuilder.view("user", data);
    }

    /**
     * 验证码演示页面。
     */
    public Response captchaDemo(Request request) {
        Map<String, Object> data = new HashMap<>();
        data.put("appName", "jaravel");
        return ResponseBuilder.view("captcha-demo", data);
    }
}
