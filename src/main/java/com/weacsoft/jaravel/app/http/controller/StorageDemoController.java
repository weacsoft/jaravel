package com.weacsoft.jaravel.app.http.controller;

import com.weacsoft.jaravel.vendor.http.controller.Controllers;
import com.weacsoft.jaravel.vendor.http.controller.request.Request;
import com.weacsoft.jaravel.vendor.http.controller.response.Response;
import com.weacsoft.jaravel.vendor.http.controller.response.ResponseBuilder;
import com.weacsoft.jaravel.vendor.storage.facade.Storage;
import com.weacsoft.jaravel.vendor.storage.contract.FileInfo;

import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 数据库文件存储（driver: database）的演示控制器。
 * <p>
 * 覆盖用户诉求的「数据库存文件」与「和 HTTP Request/Response 整合」：
 * <ul>
 *   <li><b>上传（Request 整合）</b>：从 {@link MultipartFile} 取值后经 {@code Storage.put(disk, path, byte[])} 写入。</li>
 *   <li><b>下载 / 预览（Response 整合）</b>：{@link Storage#download(String, String)} /
 *       {@link Storage#response(String, String)} 直接返回可作为控制器返回值的 {@code Response}。</li>
 *   <li><b>分片 / base64</b>：由 {@code application.yml} 中 db 磁盘的 binary / chunk-size 决定（见存储配置）。</li>
 * </ul>
 */
@Controller
public class StorageDemoController implements Controllers {

    private static final String DB_DISK = "db";

    /**
     * 文件管理首页：上传表单 + 文件列表。
     */
    public Response index(Request request) {
        List<FileInfo> files = Storage.allFiles(DB_DISK, "");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"zh\"><head><meta charset=\"utf-8\">")
                .append("<title>数据库文件存储演示</title>")
                .append("<style>body{font-family:system-ui,Arial,sans-serif;margin:2rem;max-width:880px}")
                .append("table{border-collapse:collapse;width:100%}td,th{border:1px solid #ddd;padding:6px 8px}")
                .append("a{color:#2563eb}form{margin-bottom:1.5rem;padding:1rem;border:1px solid #eee}</style></head><body>");
        html.append("<h1>数据库文件存储演示（disk: db）</h1>");

        String uploaded = request.get("uploaded");
        if (uploaded != null && !uploaded.isEmpty()) {
            html.append("<p style=\"color:green\">已上传：").append(escape(uploaded)).append("</p>");
        }

        html.append("<form method=\"post\" action=\"/demo/storage/upload\" enctype=\"multipart/form-data\">")
                .append("<h3>上传文件</h3>")
                .append("<p><label>目录：<input type=\"text\" name=\"dir\" value=\"uploads\" size=\"20\"></label></p>")
                .append("<p><input type=\"file\" name=\"file\" required></p>")
                .append("<p><button type=\"submit\">上传到数据库</button></p>")
                .append("</form>");

        html.append("<h3>已存储文件（").append(String.valueOf(files.size())).append("）</h3>");
        if (files.isEmpty()) {
            html.append("<p>暂无文件。</p>");
        } else {
            html.append("<table><tr><th>路径</th><th>大小</th><th>类型</th><th>操作</th></tr>");
            for (FileInfo f : files) {
                String enc = URLEncoder.encode(f.path(), StandardCharsets.UTF_8);
                html.append("<tr>")
                        .append("<td>").append(escape(f.path())).append("</td>")
                        .append("<td>").append(formatSize(f.size())).append("</td>")
                        .append("<td>").append(f.mimeType() == null ? "" : escape(f.mimeType())).append("</td>")
                        .append("<td>")
                        .append("<a href=\"/demo/storage/view?path=").append(enc).append("\">预览</a> | ")
                        .append("<a href=\"/demo/storage/download?path=").append(enc).append("\">下载</a> | ")
                        .append("<a href=\"/demo/storage/delete?path=").append(enc).append("\" onclick=\"return confirm('确认删除？')\">删除</a>")
                        .append("</td>")
                        .append("</tr>");
            }
            html.append("</table>");
        }

        html.append("<p><small>配置项：binary（BLOB/base64）、chunk-size（分片大小）、table-prefix。见 application.yml 的 jaravel.storage.disks.db。</small></p>");
        html.append("</body></html>");
        return ResponseBuilder.html(html.toString());
    }

    /**
     * 接收上传并写入数据库磁盘（Request 整合）。
     * <p>
     * 新版 vendor（P1 存储纯化）存储门面改为字节/流 API：
     * {@code Storage.put(disk, path, byte[])}（旧 {@code putFile(disk, dir, MultipartFile)} 已移除）。
     */
    public Response upload(Request request) {
        MultipartFile file = request.file("file");
        if (file == null || file.isEmpty()) {
            return ResponseBuilder.error(400, "未选择文件");
        }
        String dir = request.input("dir", "uploads");
        try {
            String name = file.getOriginalFilename();
            if (name == null || name.isEmpty()) {
                name = "upload-" + System.currentTimeMillis();
            }
            String path = dir.startsWith("/") ? dir + name : dir + "/" + name;
            Storage.disk(DB_DISK).put(path, file.getBytes());
            return ResponseBuilder.redirect("/demo/storage?uploaded=" + URLEncoder.encode(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseBuilder.error(500, "写入失败：" + e.getMessage());
        }
    }

    /**
     * 以附件形式下载（Response 整合）。
     */
    public Response download(Request request) {
        return Storage.download(DB_DISK, request.get("path"));
    }

    /**
     * 以内联形式预览（Response 整合，按 MIME 直接显示图片/PDF 等）。
     */
    public Response view(Request request) {
        return Storage.response(DB_DISK, request.get("path"));
    }

    /**
     * 删除文件。
     */
    public Response delete(Request request) {
        String path = request.get("path");
        if (path != null && !path.isEmpty()) {
            Storage.delete(DB_DISK, path);
        }
        return ResponseBuilder.redirect("/demo/storage");
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        return String.format("%.2f GB", mb / 1024.0);
    }
}
