package com.weacsoft.jaravel.app.service;

import com.weacsoft.jaravel.vendor.plugin.java.executor.JavaSourceExecutor;
import com.weacsoft.jaravel.vendor.plugin.jar.executor.JarBytesExecutor;

import java.io.File;
import java.util.Map;

/**
 * 插件运行服务（薄封装层）。
 * <p>
 * 核心编译/加载/执行逻辑全部委托给 vendor 模块，支持两种方式：
 * <ul>
 *   <li>Java 源码 → {@link JavaSourceExecutor#compileAndRun}（内存编译或文件编译）</li>
 *   <li>Jar 插件 → {@link JarBytesExecutor#execute}（内存加载或文件加载）</li>
 * </ul>
 */
public final class PluginRunService {

    private PluginRunService() {
    }

    /**
     * 编译并执行 Java 源码（默认纯内存编译）。
     *
     * @param code Java 源代码字符串
     * @return 执行结果 Map
     */
    public static Map<String, Object> runJava(String code) {
        return JavaSourceExecutor.compileAndRun(code);
    }

    /**
     * 编译并执行 Java 源码。
     *
     * @param code     Java 源代码字符串
     * @param inMemory true=纯内存编译（不落盘），false=文件编译（落盘到临时目录）
     * @return 执行结果 Map
     */
    public static Map<String, Object> runJava(String code, boolean inMemory) {
        return JavaSourceExecutor.compileAndRun(code, inMemory);
    }

    /**
     * 加载 Jar 文件并反射调用指定方法（默认纯内存加载）。
     */
    public static Map<String, Object> runJar(String jarName, String mainClass, String method) {
        return runJar(jarName, mainClass, method, true);
    }

    /**
     * 加载 Jar 文件并反射调用指定方法。
     *
     * @param jarName   Jar 文件名（位于 plugins/ 目录下）
     * @param mainClass 要调用的主类全限定名
     * @param method    要调用的方法名（默认 run）
     * @param inMemory  true=纯内存加载（不落盘），false=文件加载（URLClassLoader）
     * @return 执行结果 Map
     */
    public static Map<String, Object> runJar(String jarName, String mainClass, String method, boolean inMemory) {
        File jarFile = resolveJarFile(jarName);
        if (jarFile == null || !jarFile.exists()) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("success", false);
            result.put("error", "Jar 文件不存在: " + jarName);
            return result;
        }
        return JarBytesExecutor.execute(jarFile, mainClass, method, inMemory);
    }

    /** 检查 JDK 编译器是否可用 */
    public static boolean isCompilerAvailable() {
        return JavaSourceExecutor.isCompilerAvailable();
    }

    /** 在 plugins/ 目录下查找 Jar 文件 */
    private static File resolveJarFile(String jarName) {
        if (jarName == null || jarName.isEmpty()) return null;
        File pluginsDir = new File("plugins");
        if (pluginsDir.isDirectory()) {
            File jar = new File(pluginsDir, jarName.endsWith(".jar") ? jarName : jarName + ".jar");
            if (jar.exists()) return jar;
        }
        File jar = new File(jarName.endsWith(".jar") ? jarName : jarName + ".jar");
        return jar.exists() ? jar : null;
    }
}
