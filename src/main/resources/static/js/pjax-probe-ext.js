// PJAX 区域脚本运行时探针 —— 外部脚本重复加载检测。
//
// 本文件被 /home 与 /list 的 content 区域同时通过 <script src> 引用。
// 首屏由浏览器原生加载一次；之后每次 PJAX 区域替换若去重失效，就会再执行一次，
// window.__rt.ext 计数随之增长。整轮往返切换结束后该值必须仍为 1。
(function (w) {
    var rt = w.__rt = w.__rt || {};
    rt.ext = (rt.ext || 0) + 1;
})(window);
