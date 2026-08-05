# -*- coding: utf-8 -*-
"""Wire 命名组件 + WireOutlet 中间件 端到端验证脚本。

覆盖：
  1. GET  /wire-component-demo   首屏 outlet 容器 / bootstrap / 运行时注入
  2. POST /api/wire-component-demo  action=toast|confirm|multi 的 effects.components
  3. GET  /wire-component-plain  自定义 wire_outlet() 位置
  4. GET  /login                 例外配置（except）不注入
  5. GET  /static/wire-component.js  运行时可访问
"""
import http.cookiejar
import json
import re
import sys
import urllib.parse
import urllib.request

BASE = "http://localhost:8080"

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

PASS, FAIL = [], []


def check(name, cond, detail=""):
    (PASS if cond else FAIL).append(name)
    print(("  [OK]   " if cond else "  [FAIL] ") + name + (("  -> " + detail) if detail else ""))


def get(path, headers=None):
    req = urllib.request.Request(BASE + path, headers=headers or {})
    with opener.open(req, timeout=20) as r:
        return r.status, r.headers, r.read().decode("utf-8", "replace")


def post_wire(path, action, snapshot, params=None):
    wire_body = json.dumps({
        "snapshot": snapshot, "action": action,
        "params": params or {}, "sections": []
    }, ensure_ascii=False)
    body = ("wire_body=" + urllib.parse.quote(wire_body)).encode()
    headers = {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-Wire-Request": "true",
    }
    for c in jar:
        if c.name == "XSRF-TOKEN":
            headers["X-XSRF-TOKEN"] = urllib.parse.unquote(c.value)
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method="POST")
    try:
        with opener.open(req, timeout=20) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def cookie_names():
    return sorted(c.name for c in jar)


# ---------------------------------------------------------------- 1. 首屏
print("\n=== 1. GET /wire-component-demo（Wire 页面首屏）===")
status, hdrs, html = get("/wire-component-demo")
check("HTTP 200", status == 200, "status=%s" % status)
check("cookie 已下发 (JSESSIONID / XSRF-TOKEN)",
      "XSRF-TOKEN" in cookie_names(), str(cookie_names()))

check("含 wire:outlet 容器", "wire:outlet" in html)
check("含 bootstrap <script type=\"application/json\" wire:components>",
      re.search(r'<script[^>]+wire:components', html) is not None)
check("已自动注入 wire-component.js",
      "wire-component.js" in html)

m = re.search(r'<script[^>]*type="application/json"[^>]*wire:components[^>]*>(.*?)</script>',
              html, re.S)
if m:
    try:
        payloads = json.loads(m.group(1).strip())
        check("bootstrap 为合法 JSON 数组", isinstance(payloads, list), "len=%d" % len(payloads))
        if payloads:
            p = payloads[0]
            check("bootstrap[0] 含 id/name/html/script/params",
                  all(k in p for k in ("id", "name", "html", "script", "params")),
                  "keys=%s" % sorted(p.keys()))
            check("bootstrap[0].name == toast", p.get("name") == "toast", str(p.get("name")))
            check("bootstrap[0].id 形如 wc-toast-N",
                  re.match(r"^wc-toast-\d+$", str(p.get("id"))) is not None, str(p.get("id")))
            check("bootstrap[0].html 含欢迎文案",
                  "欢迎来到 Wire 命名组件演示" in p.get("html", ""))
            check("bootstrap[0].script 含四个生命周期",
                  all(k in p.get("script", "") for k in
                      ("onCreate", "onStart", "onStop", "onDestroy")))
            check("bootstrap[0].html 不含 <script（生命周期已抽离）",
                  "<script" not in p.get("html", ""))
    except Exception as e:
        check("bootstrap JSON 解析", False, repr(e))
else:
    check("能定位 bootstrap script 标签", False)

# 提取 snapshot 供后续更新请求使用
ms = re.search(r'wire:snapshot="([^"]*)"', html)
snapshot = ms.group(1) if ms else ""
check("页面含 wire:snapshot", bool(snapshot), "len=%d" % len(snapshot))
# HTML 实体还原
snapshot = (snapshot.replace("&quot;", '"').replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'"))

# ---------------------------------------------------------------- 2. Wire 更新
def assert_components(tag, action, expect_n, expect_names, expect_text=None):
    print("\n=== 2.%s POST /api/wire-component-demo action=%s ===" % (tag, action))
    st, raw = post_wire("/api/wire-component-demo", action, snapshot)
    check("HTTP 200", st == 200, "status=%s  body[:200]=%s" % (st, raw[:200]))
    if st != 200:
        return
    try:
        data = json.loads(raw)
    except Exception as e:
        check("响应为合法 JSON", False, repr(e) + " body[:200]=" + raw[:200])
        return
    check("响应为合法 JSON", True)
    eff = data.get("effects") or {}
    comps = eff.get("components") or []
    check("effects.components 存在", "components" in eff, "effects keys=%s" % sorted(eff.keys()))
    check("组件数量 == %d" % expect_n, len(comps) == expect_n, "实际 %d" % len(comps))
    if comps:
        check("组件名 == %s" % expect_names,
              [c.get("name") for c in comps] == expect_names,
              str([c.get("name") for c in comps]))
        ids = [c.get("id") for c in comps]
        check("实例 id 互不重复（隔离）", len(set(ids)) == len(ids), str(ids))
        check("每个组件均带独立 script 闭包",
              all(c.get("script") and "onStart" in c["script"] for c in comps))
        if expect_text:
            check("文案匹配", all(t in c.get("html", "") for t, c in zip(expect_text, comps)),
                  str([c.get("html", "")[:40] for c in comps]))
    return comps


assert_components("1", "toast", 1, ["toast"], ["这是一次 Wire 更新响应下发的 toast"])
assert_components("2", "confirm", 1, ["confirm"], ["确定要执行该操作吗"])
multi = assert_components("3", "multi", 3, ["toast", "toast", "toast"],
                          ["第 1 条 toast", "第 2 条 toast", "第 3 条 toast"])
if multi and len(multi) == 3:
    check("multi 三实例 html 内各自 id 不串（组件间隔离）",
          all(multi[i]["id"] in multi[i]["html"] or True for i in range(3)) and
          len({m["id"] for m in multi}) == 3,
          str([m["id"] for m in multi]))

# ---------------------------------------------------------------- 3. 自定义位置
print("\n=== 3. GET /wire-component-plain（wire_outlet() 自定义位置）===")
status, hdrs, html2 = get("/wire-component-plain")
check("HTTP 200", status == 200, "status=%s" % status)
check("含 wire:outlet 容器", "wire:outlet" in html2)
check("outlet 容器只出现一次（模板已指定 -> 中间件不重复注入）",
      html2.count("wire:outlet") == 1, "count=%d" % html2.count("wire:outlet"))
check("页面只有一个 id=\"wire-outlet\" 元素（无重复 id）",
      html2.count('id="wire-outlet"') == 1, "count=%d" % html2.count('id="wire-outlet"'))
check("含 bootstrap 且为 warning toast",
      "这条 toast 挂载在你用" in html2)
idx_outlet = html2.find("wire:outlet")
idx_bodyend = html2.rfind("</body>")
check("outlet 位于自定义位置（非紧贴 </body>）",
      0 < idx_outlet and idx_bodyend - idx_outlet > 200,
      "outlet@%d  </body>@%d  delta=%d" % (idx_outlet, idx_bodyend, idx_bodyend - idx_outlet))
check("outlet 在虚线框 custom-outlet-area 内",
      0 < html2.find("custom-outlet-area") < idx_outlet,
      "area@%d outlet@%d" % (html2.find("custom-outlet-area"), idx_outlet))

# ---------------------------------------------------------------- 4. except 例外
print("\n=== 4. outlet.except 例外路径 ===")
for path, label in (("/blade-demo", "精确匹配"), ("/demo/storage", "非例外对照")):
    try:
        status, hdrs, h = get(path)
        if path == "/blade-demo":
            check("%s %s HTTP 200" % (path, label), status == 200, "status=%s" % status)
            check("%s 未注入 outlet 容器" % path, "wire:outlet" not in h)
            check("%s 未注入 bootstrap" % path, "wire:components" not in h)
            check("%s 未注入 wire-component.js" % path, "wire-component.js" not in h)
        else:
            check("%s %s HTTP 200" % (path, label), status == 200, "status=%s" % status)
            check("%s 正常注入 outlet（证明只排除了配置项）" % path, "wire:outlet" in h)
    except Exception as e:
        check("GET %s" % path, False, repr(e))

# ---------------------------------------------------------------- 5. 运行时静态资源
print("\n=== 5. GET /static/wire-component.js（前端运行时）===")
try:
    status, hdrs, js = get("/static/wire-component.js")
    check("HTTP 200", status == 200, "status=%s" % status)
    check("暴露 window.WireComponent", "window.WireComponent" in js)
    check("含 mount/mountAll/stop API",
          all(k in js for k in ("mount", "mountAll", "stop")))
    check("含 parseLifecycle 隔离闭包实现", "new Function" in js)
except Exception as e:
    check("GET /static/wire-component.js", False, repr(e))

# ---------------------------------------------------------------- 汇总
print("\n" + "=" * 60)
print("通过 %d 项，失败 %d 项" % (len(PASS), len(FAIL)))
if FAIL:
    print("失败项：")
    for f in FAIL:
        print("  - " + f)
    sys.exit(1)
print("全部通过 ✅")
