package com.maxkb4j.tool.executor;

import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.MD5Util;
import com.maxkb4j.tool.sandbox.GroovySandboxCompilerConfigurer;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Groovy 脚本编译缓存。
 * <p>
 * Groovy 每次编译都会生成新的 Class，重复编译既浪费 CPU 又导致 metaspace 增长。
 * 相同脚本只编译一次（编译配置来自 {@link GroovySandboxCompilerConfigurer}），
 * 后续执行仅新建 Script 实例（实例独立、线程安全）。
 * 每个唯一脚本使用独立 GroovyClassLoader，缓存淘汰后整个 loader 连同其加载的类可被 GC 回收。
 * </p>
 */
public final class GroovyScriptCache {

    /** 编译缓存最大条目数，超出按 LRU 淘汰 */
    private static final int MAX_CACHED_SCRIPTS = 256;

    /**
     * 已编译脚本缓存：key 为脚本内容摘要，value 为脚本类及其专属 ClassLoader。
     */
    private static final Map<String, CompiledScript> SCRIPT_CACHE =
            Collections.synchronizedMap(new LinkedHashMap<>(32, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CompiledScript> eldest) {
                    return size() > MAX_CACHED_SCRIPTS;
                }
            });

    /** 编译产物：脚本类及其专属 ClassLoader。 */
    public record CompiledScript(GroovyClassLoader loader, Class<?> scriptClass) {
    }

    private GroovyScriptCache() {
    }

    /** 获取脚本编译结果，未缓存时编译并放入缓存。 */
    public static CompiledScript get(String code) {
        synchronized (SCRIPT_CACHE) {
            return SCRIPT_CACHE.computeIfAbsent(cacheKey(code), key -> compileScript(code));
        }
    }

    /** 脚本是否已进入编译缓存。 */
    public static boolean contains(String code) {
        return SCRIPT_CACHE.containsKey(cacheKey(code));
    }

    private static String cacheKey(String code) {
        return MD5Util.encrypt(code);
    }

    private static CompiledScript compileScript(String code) {
        GroovyClassLoader loader = new GroovyClassLoader(
                GroovyScriptCache.class.getClassLoader(),
                GroovySandboxCompilerConfigurer.safeConfiguration());
        Class<?> scriptClass;
        try {
            scriptClass = loader.parseClass(code);
        } catch (CompilationFailedException e) {
            // SecureAST 在编译期抛出的 SecurityException 会被包装进编译失败异常，
            // 这里还原原始 SecurityException，保持与运行期沙箱拒绝一致的错误语义
            SecurityException securityException = GroovySandboxCompilerConfigurer.findCompileSecurityException(e);
            if (securityException != null) {
                throw securityException;
            }
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", e.getMessage()), e);
        }
        if (!Script.class.isAssignableFrom(scriptClass)) {
            throw new RuntimeException(I18nUtil.get("tool.groovy.script.execution.failed", "unsupported script structure"));
        }
        return new CompiledScript(loader, scriptClass);
    }
}
